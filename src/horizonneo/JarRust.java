package horizonneo;

import horizonneo.MethodRust.Need;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The translator over a whole jar: every method of the mod's own classes is checked (`MethodRust.check`), then
 * what each needs is weighed against what is translated, until nothing changes: a call of a method that isn't
 * translated, a virtual call one of whose targets isn't, a class whose initializer isn't. The world is closed
 * (`JavaWorld`): a virtual call and a type test are decided over the classes the translated code instantiates.
 * The rest is written into one Rust module with the runtime of the uniform object model, the dispatchers, type
 * tests, class initializers and lambda classes it uses.
 *
 * The bytecode is read from the jar; nothing is decompiled.
 */
final class JarRust {
    private JarRust() {}

    /** A method of the jar, by class, name and descriptor. */
    record Id(String owner, String name, String descriptor) implements Comparable<Id> {
        @Override
        public int compareTo(Id o) {
            return (owner + "." + name + descriptor).compareTo(o.owner + "." + o.name + o.descriptor);
        }

        String display() {
            return owner.replace('/', '.') + "." + name + descriptor;
        }
    }

    /**
     * What came out. methods: how many methods have a body. translated: the Rust name of each one translated.
     * why: why each other one isn't. rust: the module's text.
     */
    record Result(int methods, Map<Id, String> translated, Map<Id, String> why, String rust) {
        /** How many methods each reason keeps out, the most first. */
        Map<String, Integer> reasons() {
            Map<String, Integer> counts = new HashMap<>();
            for (String w : why.values()) counts.merge(kind(w), 1, Integer::sum);
            Map<String, Integer> out = new LinkedHashMap<>();
            counts.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue() != 0 ? b.getValue() - a.getValue() : a.getKey().compareTo(b.getKey()))
                    .forEach(e -> out.put(e.getKey(), e.getValue()));
            return out;
        }
    }

    /** A reason without what is particular to the method: what comes before its first colon. */
    static String kind(String why) {
        if (why.contains(", which isn't translated")) return "it needs a method of the mod that isn't translated";
        int colon = why.indexOf(": ");
        String k = colon < 0 ? why : why.substring(0, colon);
        int bracket = k.indexOf(" (");
        return bracket < 0 ? k : k.substring(0, bracket);
    }

    static Result translate(ModJar jar) throws IOException {
        return translate(jar, Map.of());
    }

    /** outside: classes the mod runs with, known for their supertypes alone (`JavaWorld.outside`). */
    static Result translate(ModJar jar, Map<String, ClassData> outside) throws IOException {
        Map<String, ClassData> classes = new TreeMap<>();
        try (ZipFile zip = new ZipFile(jar.path().toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (e.isDirectory() || !name.endsWith(".class") || name.startsWith("META-INF/") || name.endsWith("module-info.class")) continue;
                ClassData c = ClassData.parse(ModJar.read(zip, e, Analyzer.MAX_CLASS));
                classes.put(c.name, c);
            }
        }
        return translate(classes, Library.classes(), outside);
    }

    static Result translate(Map<String, ClassData> classes) throws IOException {
        return translate(classes, Library.classes());
    }

    static Result translate(Map<String, ClassData> jar, Map<String, ClassData> model) {
        return translate(jar, model, Map.of());
    }

    /**
     * jar: the mod's classes; model: the library model's (`Library`), translated with them where the mod reaches them;
     * outside: classes known for their supertypes alone.
     */
    static Result translate(Map<String, ClassData> jar, Map<String, ClassData> model, Map<String, ClassData> outside) {
        JavaWorld world = new JavaWorld(jar, model, outside);
        int methods = 0;
        Map<Id, String> why = new TreeMap<>();
        Map<Id, List<Need>> needs = new TreeMap<>();
        Map<Id, ClassData> owners = new HashMap<>();
        Map<Id, ClassData.Method> bodies = new HashMap<>();
        for (ClassData c : world.classes.values()) {
            boolean own = world.jar.contains(c.name);
            for (ClassData.Method m : c.methods) {
                if (m.body() == null) continue;
                if (own) methods++;
                Id id = new Id(c.name, m.name(), m.descriptor());
                MethodRust.Check check;
                try {
                    check = MethodRust.check(c.name, m, c.pool, world);
                } catch (ClassData.Malformed | Bytecode.Invalid e) {
                    check = new MethodRust.Check("its bytecode doesn't decode: " + e.getMessage(), List.of());
                }
                if (check.why() != null) {
                    why.put(id, check.why());
                } else {
                    needs.put(id, check.needs());
                    owners.put(id, c);
                    bodies.put(id, m);
                }
            }
        }

        Map<Id, String> translated = new TreeMap<>();
        String rust;
        // Every method that checks, for revive: settle drops methods for good.
        Map<Id, List<Need>> pool = new TreeMap<>(needs);
        // Written until every method written has what it needs written too: one whose flow can't be followed drops out.
        while (true) {
            // Two ways, each leaving methods that have what they need; the one that leaves more of the mod's is kept.
            // The other first drops what drops whatever objects there are (a call, a class initializer): the objects
            // are then only those of the methods that may stay, and fewer objects ask less of the type tests and
            // virtual calls. Neither way's methods are always more than the other's.
            Map<Id, List<Need>> first = new TreeMap<>(needs);
            Map<Id, String> whyFirst = new TreeMap<>(why);
            settle(world, first, whyFirst, null, true);
            settleAndRevive(world, pool, first, whyFirst);
            settleAndRevive(world, pool, needs, why);
            if (own(world, first) > own(world, needs)) {
                needs.clear();
                needs.putAll(first);
                why.clear();
                why.putAll(whyFirst);
            }
            Set<Id> reached = reached(world, needs);
            Map<Id, List<Need>> written = new TreeMap<>();
            for (Id id : reached) if (needs.containsKey(id)) written.put(id, needs.get(id));
            Map<Id, String> names = names(written.keySet());
            Writing w = new Writing(world, written, names);
            translated.clear();
            StringBuilder functions = new StringBuilder();
            Set<String> helpers = new TreeSet<>();
            boolean again = false;
            int fromModel = 0;
            for (Id id : List.copyOf(written.keySet())) {
                try {
                    MethodRust.Done done = MethodRust.write(id.owner(), bodies.get(id), owners.get(id).pool, names.get(id), world, w);
                    functions.append('\n').append(done.rust());
                    helpers.addAll(done.helpers());
                    if (world.jar.contains(id.owner())) translated.put(id, names.get(id));
                    else fromModel++;
                } catch (RuntimeException e) {
                    why.put(id, "its flow couldn't be followed: " + e.getMessage());
                    needs.remove(id);
                    pool.remove(id);
                    again = true;
                }
            }
            if (again) continue;
            rust = w.module(translated.size(), methods, fromModel, helpers, functions.toString());
            break;
        }
        // The model's own methods aren't the mod's: only the mod's are reported.
        why.keySet().removeIf(id -> !world.jar.contains(id.owner()));
        return new Result(methods, translated, why, rust);
    }

    /**
     * The methods to write: the jar's that are translated, and what they reach of the model's (a call, a class
     * initializer, a method a virtual call may select).
     */
    static Set<Id> reached(JavaWorld world, Map<Id, List<Need>> needs) {
        Instances inst = instances(world, needs);
        Set<Id> out = new TreeSet<>();
        java.util.Deque<Id> todo = new java.util.ArrayDeque<>();
        for (Id id : needs.keySet()) if (world.jar.contains(id.owner())) todo.add(id);
        while (!todo.isEmpty()) {
            Id id = todo.poll();
            if (!needs.containsKey(id) || !out.add(id)) continue;
            for (Need n : needs.get(id)) {
                switch (n.kind()) {
                    case "call", "lambda" -> {
                        todo.add(n.id());
                        todo.addAll(Library.callsOf(n.id()));
                    }
                    case "enums" -> {
                        for (String e : inst.enums(world)) todo.add(new Id(e, "values", "()[L" + e + ";"));
                    }
                    case "init" -> {
                        for (String c : world.initOrder(n.type())) if (world.declared(c, "<clinit>", "()V") != null) todo.add(new Id(c, "<clinit>", "()V"));
                    }
                    case "dispatch" -> {
                        for (String a : inst.arrays()) {
                            if (!Boolean.TRUE.equals(world.subtype(a, n.type()))) continue;
                            JavaWorld.Selected sel = arraySelects(world, n.name(), n.descriptor());
                            if (sel.id() != null) todo.add(sel.id());
                        }
                        for (String c : inst.classes()) {
                            if (!Boolean.TRUE.equals(world.subtype(c, n.type()))) continue;
                            JavaWorld.Selected sel = world.select(c, n.name(), n.descriptor());
                            if (sel.id() != null) todo.add(sel.id());
                        }
                        for (String l : inst.lambdas()) {
                            if (!Boolean.TRUE.equals(world.subtype(l, n.type()))) continue;
                            JavaWorld.Selected sel = world.selectOnLambda(l, n.name(), n.descriptor());
                            if (sel.id() != null) todo.add(sel.id());
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        return out;
    }

    // ---- what each method needs, until nothing changes ----

    /**
     * What the translated methods instantiate: classes and arrays they make, lambda sites they reach; tests: the
     * type tests' answers over them, by type, asked once per set.
     */
    record Instances(Set<String> classes, Set<String> arrays, Set<String> lambdas, Set<String> constants, Map<String, Optional<String>> tests) {
        Instances(Set<String> classes, Set<String> arrays, Set<String> lambdas, Set<String> constants) {
            this(classes, arrays, lambdas, constants, new HashMap<>());
        }

        /** The enums a class object may stand for: of a class constant, or of an object's getClass(). */
        Set<String> enums(JavaWorld world) {
            Set<String> out = new TreeSet<>();
            for (Set<String> types : List.of(classes, constants)) for (String t : types) if (world.enumClass(t)) out.add(t);
            return out;
        }

        /**
         * Whether what a method makes (an object, a lambda, a class constant) is among these. A class constant only
         * counts for the enums a class object may stand for: one of another type, or of an enum already among them,
         * adds nothing.
         */
        boolean has(Need n, JavaWorld world) {
            return switch (n.kind()) {
                case "new" -> (n.type().startsWith("[") ? arrays : classes).contains(n.type());
                case "lambda" -> lambdas.contains(n.type());
                case "class" -> !world.enumClass(n.type()) || enums(world).contains(n.type());
                default -> true;
            };
        }

        List<String> all() {
            List<String> out = new ArrayList<>(classes);
            out.addAll(arrays);
            out.addAll(lambdas);
            return out;
        }
    }

    static Instances instances(JavaWorld world, Map<Id, List<Need>> needs) {
        Set<String> classes = new TreeSet<>(), arrays = new TreeSet<>(), lambdas = new TreeSet<>(), constants = new TreeSet<>();
        // What the runtime makes itself: the exceptions it throws, with their messages; class objects (getClass).
        for (String c : Library.thrownClasses()) if (world.concrete(c)) classes.add(c);
        if (world.concrete(MethodRust.STRING)) {
            classes.add(MethodRust.STRING);
            arrays.add("[C");
        }
        if (world.concrete(MethodRust.CLASS)) classes.add(MethodRust.CLASS);
        for (List<Need> list : needs.values()) {
            for (Need n : list) {
                if (n.kind().equals("new")) (n.type().startsWith("[") ? arrays : classes).add(n.type());
                if (n.kind().equals("lambda")) lambdas.add(n.type());
                if (n.kind().equals("class")) constants.add(n.type());
            }
        }
        return new Instances(classes, arrays, lambdas, constants);
    }

    private static void settleAndRevive(JavaWorld world, Map<Id, List<Need>> pool, Map<Id, List<Need>> needs, Map<Id, String> why) {
        settle(world, needs, why, null, false);
        while (revive(world, pool, needs, why)) {
            // What came back may let more come back.
        }
    }

    /** The needs that ask something of every object there may be. */
    private static final Set<String> OVER_OBJECTS = Set.of("dispatch", "enums", "test");

    /**
     * Drops the methods whose needs aren't met until none is left to drop; objectsAside: only by the needs that ask
     * nothing of the objects there may be. The objects there can be are those the methods left make; with frozen,
     * frozen's, and a method that would make one frozen hasn't drops out (it keeps the reason it had).
     */
    private static void settle(JavaWorld world, Map<Id, List<Need>> needs, Map<Id, String> why, Instances frozen, boolean objectsAside) {
        for (boolean changed = true; changed; ) {
            changed = false;
            Instances inst = frozen != null ? frozen : objectsAside ? null : instances(world, needs);
            // With the objects free to shrink: a class of the mod a virtual call may reach, whose method isn't
            // translated, loses its makers rather than the callers (see POISON).
            Map<String, String> poison = frozen == null && !objectsAside ? new TreeMap<>() : null;
            for (Id id : List.copyOf(needs.keySet())) {
                for (Need n : needs.get(id)) {
                    if (objectsAside && OVER_OBJECTS.contains(n.kind())) continue;
                    if (frozen != null && !frozen.has(n, world)) {
                        why.putIfAbsent(id, "it makes an object no other translated method makes");
                        needs.remove(id);
                        changed = true;
                        break;
                    }
                    String reason = unmet(n, world, needs, why, inst, poison);
                    if (reason == null) continue;
                    why.put(id, reason);
                    needs.remove(id);
                    changed = true;
                    break;
                }
            }
            if (poison == null || poison.isEmpty()) continue;
            for (Id id : List.copyOf(needs.keySet())) {
                for (Need n : needs.get(id)) {
                    if (!(n.kind().equals("new") || n.kind().equals("lambda")) || !poison.containsKey(n.type())) continue;
                    String made = n.kind().equals("new") ? "an object of " + n.type().replace('/', '.') : "a lambda of " + world.lambdaInterface(n.type()).replace('/', '.');
                    why.put(id, "it makes " + made + " that a virtual call may reach, and " + poison.get(n.type()));
                    needs.remove(id);
                    changed = true;
                    break;
                }
            }
        }
    }

    /**
     * When a virtual call may reach a class of the mod (or a lambda of the mod's) whose method isn't translated (or
     * can't be told: a default method outside the jar, a supertype unknown), the methods that make its objects drop
     * out, not the callers: one class made in one place, reached through Consumer
     * or Iterator, otherwise kept every method that iterates out. Both keep each method left with what it needs. On
     * Mekanism: 7594 methods without, 8871 for calls on types outside the jar alone, 8967 for every call.
     */
    private static boolean poisonable(JavaWorld world, String instance) {
        String owner = instance.startsWith(JavaWorld.LAMBDA) ? instance.substring(JavaWorld.LAMBDA.length(), instance.indexOf('#')) : instance;
        return world.jar.contains(owner);
    }

    /**
     * settle drops a method for good, though what dropped it may go later: Enum.valueOf drops while an enum whose
     * values() isn't translated has a class constant, and stays dropped once the method with that constant drops in
     * turn. So every method is checked again over frozen objects, those that the methods the mod's reach make, and a
     * method that would make more stays out: each method reached then still has what it needs. True when more of the
     * mod's came back. (Taking in what the methods back make instead, until the objects cover it, brought fewer back
     * on Mekanism: the objects some make cost many others.)
     */
    private static boolean revive(JavaWorld world, Map<Id, List<Need>> pool, Map<Id, List<Need>> needs, Map<Id, String> why) {
        Instances frozen = instances(world, reachedNeeds(world, needs));
        Map<Id, List<Need>> again = new TreeMap<>(pool);
        Map<Id, String> whyAgain = new TreeMap<>(why);
        settle(world, again, whyAgain, frozen, false);
        if (own(world, again) <= own(world, needs)) return false;
        needs.clear();
        needs.putAll(again);
        why.clear();
        why.putAll(whyAgain);
        why.keySet().removeAll(needs.keySet());
        return true;
    }

    /** The needs of the methods written: only what the mod's methods reach runs, and makes objects. */
    private static Map<Id, List<Need>> reachedNeeds(JavaWorld world, Map<Id, List<Need>> needs) {
        Map<Id, List<Need>> out = new TreeMap<>();
        for (Id id : reached(world, needs)) out.put(id, needs.get(id));
        return out;
    }

    private static long own(JavaWorld world, Map<Id, List<Need>> needs) {
        return needs.keySet().stream().filter(id -> world.jar.contains(id.owner())).count();
    }

    /** Why a need isn't met by what is translated; null when it is. */
    static String unmet(Need n, JavaWorld world, Map<Id, List<Need>> needs, Map<Id, String> why, Instances inst) {
        return unmet(n, world, needs, why, inst, null);
    }

    /** poison: where a class (or lambda) of the mod a virtual call may reach is put, with why, instead of failing. */
    static String unmet(Need n, JavaWorld world, Map<Id, List<Need>> needs, Map<Id, String> why, Instances inst, Map<String, String> poison) {
        switch (n.kind()) {
            case "call" -> {
                return untranslated("it calls ", n.id(), needs, why);
            }
            case "init" -> {
                if (world.initLeaves(n.type())) return "it initializes a class whose superclass is outside the jar: " + n.type().replace('/', '.');
                for (String c : world.initOrder(n.type())) {
                    Id clinit = new Id(c, "<clinit>", "()V");
                    if (world.declared(c, "<clinit>", "()V") != null) {
                        String r = untranslated("it initializes " + c.replace('/', '.') + ", whose initializer ", clinit, needs, why);
                        if (r != null) return r;
                    }
                }
                return null;
            }
            case "dispatch" -> {
                for (String a : inst.arrays()) {
                    if (!Boolean.TRUE.equals(world.subtype(a, n.type()))) continue;
                    JavaWorld.Selected s = arraySelects(world, n.name(), n.descriptor());
                    if (s.why() != null) return "a virtual call may reach an array: " + n.type().replace('/', '.') + "." + n.name() + " " + s.why();
                    String r = untranslated("it calls " + n.type().replace('/', '.') + "." + n.name() + ", which on an array runs ", s.id(), needs, why);
                    if (r != null) return r;
                }
                for (String c : inst.classes()) {
                    Boolean sub = world.subtype(c, n.type());
                    String r;
                    if (sub == null) {
                        r = "a virtual call isn't certain: whether " + c.replace('/', '.') + " is a " + n.type().replace('/', '.') + " can't be told";
                    } else if (!sub) {
                        continue;
                    } else {
                        JavaWorld.Selected s = world.select(c, n.name(), n.descriptor());
                        r = s.why() != null ? "a virtual call isn't certain: " + n.type().replace('/', '.') + "." + n.name() + " " + s.why()
                                : untranslated("it calls " + n.type().replace('/', '.') + "." + n.name() + ", which may run ", s.id(), needs, why);
                    }
                    if (r != null && poison != null && poisonable(world, c)) {
                        poison.putIfAbsent(c, r);
                        continue;
                    }
                    if (r != null) return r;
                }
                for (String key : inst.lambdas()) {
                    Boolean sub = world.subtype(key, n.type());
                    String r;
                    if (sub == null) {
                        r = "a virtual call isn't certain: whether a lambda of " + world.lambdaInterface(key).replace('/', '.') + " is a " + n.type().replace('/', '.') + " can't be told";
                    } else if (!sub) {
                        continue;
                    } else {
                        ClassData.LambdaSite site = world.lambdas.get(key);
                        if (site.samName().equals(n.name()) && site.erased().equals(n.descriptor())) continue;
                        JavaWorld.Selected s = world.selectOnLambda(key, n.name(), n.descriptor());
                        r = s.why() != null ? "a virtual call isn't certain: on a lambda, " + n.type().replace('/', '.') + "." + n.name() + " " + s.why()
                                : untranslated("it calls " + n.name() + " on a lambda, which may run ", s.id(), needs, why);
                    }
                    if (r != null && poison != null && poisonable(world, key)) {
                        poison.putIfAbsent(key, r);
                        continue;
                    }
                    if (r != null) return r;
                }
                return null;
            }
            case "enums" -> {
                for (String e : inst.enums(world)) {
                    String r = untranslated("it asks an enum's class for its constants, and ", new Id(e, "values", "()[L" + e + ";"), needs, why);
                    if (r != null) return r;
                }
                return null;
            }
            case "test" -> {
                Optional<String> known = inst.tests().get(n.type());
                if (known == null) {
                    known = Optional.ofNullable(testWhy(n, world, inst));
                    inst.tests().put(n.type(), known);
                }
                return known.orElse(null);
            }
            default -> {
                return null;
            }
        }
    }

    /** Why a type test (or an aastore's) isn't certain over every object there can be; null when it is. */
    private static String testWhy(Need n, JavaWorld world, Instances inst) {
        switch (n.kind()) {
            case "test" -> {
                if (n.type().equals("aastore")) {
                    for (String a : inst.arrays()) {
                        if (a.length() == 2) continue;
                        String component = JavaWorld.type(a.substring(1));
                        for (String x : inst.all()) {
                            if (world.subtype(x, component) == null) return "a type test isn't certain: whether " + x + " goes into an array of " + component.replace('/', '.') + " can't be told";
                        }
                    }
                    return null;
                }
                for (String x : inst.all()) {
                    if (world.subtype(x, n.type()) == null) return "a type test isn't certain: whether " + x + " is a " + n.type().replace('/', '.') + " can't be told";
                }
                return null;
            }
            default -> {
                return null;
            }
        }
    }

    /** What a virtual call selects on an array: Object's method (an array's clone isn't modelled yet). */
    static JavaWorld.Selected arraySelects(JavaWorld world, String name, String descriptor) {
        if (name.equals("clone") && descriptor.equals("()Ljava/lang/Object;")) return new JavaWorld.Selected(new Id("[", "clone", descriptor), null);
        if (world.declared(JavaWorld.OBJECT, name, descriptor) == null) return new JavaWorld.Selected(null, "(Object has no " + name + ")");
        return new JavaWorld.Selected(new Id(JavaWorld.OBJECT, name, descriptor), null);
    }

    private static String untranslated(String said, Id id, Map<Id, List<Need>> needs, Map<Id, String> why) {
        if (Library.nativeOf(id) != null) {
            for (Id c : Library.callsOf(id)) {
                String r = untranslated(said, c, needs, why);
                if (r != null) return r;
            }
            return null;
        }
        if (needs.containsKey(id)) return null;
        return said + id.display() + ", which isn't translated (" + (why.containsKey(id) ? why.get(id) : "it has no body in the jar") + ")";
    }

    // ---- writing ----

    /** The names the methods ask for, and what they name, written once all functions are. */
    static final class Writing implements MethodRust.Names {
        final JavaWorld world;
        final Map<Id, List<Need>> needs;
        final Map<Id, String> functions;
        final Instances inst;
        final Map<String, String> dispatchers = new LinkedHashMap<>();
        final Map<String, String> tests = new LinkedHashMap<>();
        final Map<String, Integer> classIndex = new TreeMap<>();
        final Set<String> inits = new TreeSet<>(), statics = new TreeSet<>(), lambdaNews = new TreeSet<>();

        Writing(JavaWorld world, Map<Id, List<Need>> needs, Map<Id, String> functions) {
            this.world = world;
            this.needs = needs;
            this.functions = functions;
            this.inst = instances(world, needs);
        }

        final Map<String, String> literals = new LinkedHashMap<>();
        final Set<String> natives = new TreeSet<>();

        final Map<String, String> objectMethodNames = new LinkedHashMap<>();

        @Override
        public String objectMethods(String key) {
            return objectMethodNames.computeIfAbsent(key, k -> "jobject_methods_" + objectMethodNames.size());
        }

        @Override
        public String literal(String text) {
            return literals.computeIfAbsent(text, k -> "jliteral_" + literals.size());
        }

        @Override
        public String function(Id id) {
            Library.Native n = Library.nativeOf(id);
            if (n != null) {
                natives.add(n.name());
                return n.name();
            }
            String name = functions.get(id);
            if (name == null) throw new IllegalStateException(id.display() + " isn't translated");
            return name;
        }

        @Override
        public String dispatcher(String owner, String name, String descriptor) {
            return dispatchers.computeIfAbsent(owner + "." + name + descriptor, k -> "jd_" + dispatchers.size() + "_" + snake(name));
        }

        @Override
        public String typeTest(String type) {
            return tests.computeIfAbsent(type, k -> "jis_" + tests.size());
        }

        int index(String cls) {
            return classIndex.computeIfAbsent(cls, k -> classIndex.size());
        }

        @Override
        public String init(String cls) {
            inits.add(cls);
            return "jinit_" + index(cls);
        }

        @Override
        public String statics(String cls) {
            statics.add(cls);
            return "JSTATICS_" + index(cls);
        }

        @Override
        public int id(String type) {
            return world.id(type);
        }

        @Override
        public String lambdaNew(String key) {
            lambdaNews.add(key);
            return "jlambda_" + world.id(key) + "_new";
        }

        String module(int translated, int methods, int fromModel, Set<String> helpers, String functionText) {
            StringBuilder parts = new StringBuilder();
            // The runtime's exceptions and texts, made from the model's classes: asked for first, as they ask for type tests.
            String worldText = worldRuntime();
            // Lambdas and dispatchers may ask for more of each other, type tests and initializers: written until none is new.
            Set<String> doneLambdas = new TreeSet<>(), doneDispatchers = new TreeSet<>(), doneObjectMethods = new TreeSet<>();
            StringBuilder generated = new StringBuilder();
            for (boolean more = true; more; ) {
                more = false;
                for (String key : new ArrayList<>(inst.lambdas())) {
                    if (doneLambdas.add(key)) {
                        generated.append(lambda(key));
                        more = true;
                    }
                }
                for (String k : new ArrayList<>(dispatchers.keySet())) {
                    if (doneDispatchers.add(k)) {
                        generated.append(dispatch(k));
                        more = true;
                    }
                }
                for (String k : new ArrayList<>(objectMethodNames.keySet())) {
                    if (doneObjectMethods.add(k)) {
                        generated.append(objectMethodsFn(k));
                        more = true;
                    }
                }
            }
            StringBuilder initText = new StringBuilder();
            Set<String> doneInits = new TreeSet<>();
            for (boolean more = true; more; ) {
                more = false;
                for (String c : new ArrayList<>(inits)) if (doneInits.add(c)) {
                    initText.append(initializer(c));
                    more = true;
                }
            }
            StringBuilder staticText = new StringBuilder();
            for (String c : statics) staticText.append(staticStore(c));
            StringBuilder testText = new StringBuilder();
            tests.forEach((type, name) -> testText.append(test(name, type)));
            StringBuilder literalText = new StringBuilder();
            literals.forEach((text, name) -> literalText.append(literal(name, text)));
            StringBuilder nativeText = new StringBuilder();
            // In a fixed order: Map.ofEntries iterates in an order of its own each run.
            for (Map.Entry<String, Library.Native> e : new TreeMap<>(Library.NATIVES).entrySet()) {
                Library.Native n = e.getValue();
                if (!natives.contains(n.name())) continue;
                String text = n.rust();
                List<Id> calls = Library.NATIVE_CALLS.getOrDefault(n.name(), List.of());
                for (int k = 0; k < calls.size(); k++) text = text.replace("{CALL" + k + "}", function(calls.get(k)));
                nativeText.append('\n').append(text);
            }
            if (natives.contains("jnative_float_text") || natives.contains("jnative_double_text")) nativeText.append(Library.NUMBER_TEXT);
            if (natives.stream().anyMatch(Library.UNICODE_NATIVES::contains)) nativeText.append('\n').append(Library.unicodeText());

            parts.append("// The mod's methods translated from their bytecode by horizon-port-neoforge's own translator, with Java's\n");
            parts.append("// semantics: wrapping int arithmetic, shifts modulo the width, saturating float casts, NaN as each comparison\n");
            parts.append("// treats it, Math's exact results. Objects follow a uniform model: a reference is an Option<Arc<Obj>>, fields\n");
            parts.append("// are slots, a virtual call matches on the object's class. A method returns a Result: what Java throws is an Err\n");
            parts.append("// holding the exception, an object of the library model's (horizon-port-neoforge's lib/, translated here too).\n");
            parts.append("// ").append(translated).append(" of ").append(methods).append(" methods of the mod, and ").append(fromModel)
                    .append(" of the library model; translation.md says why the others aren't.\n");
            parts.append(RUNTIME);
            parts.append(worldText);
            parts.append(accepts());
            for (String h : helpers) parts.append('\n').append("#[allow(dead_code)]\n").append(MethodRust.HELPERS.get(h)).append('\n');
            parts.append(staticText).append(initText).append(testText).append(literalText).append(nativeText).append(generated).append(functionText);
            return parts.toString();
        }

        /** The ids of the instantiated types that are subtypes of type. */
        List<Integer> idsOf(String type) {
            List<Integer> out = new ArrayList<>();
            for (String x : inst.all()) {
                Boolean sub = world.subtype(x, type);
                if (sub == null) throw new IllegalStateException("whether " + x + " is a " + type + " can't be told");
                if (sub) out.add(world.id(x));
            }
            out.sort(null);
            return out;
        }

        private static String matches(List<Integer> ids) {
            return ids.isEmpty() ? "false" : "matches!(c, " + String.join(" | ", ids.stream().map(String::valueOf).toList()) + ")";
        }

        String test(String name, String type) {
            return "\n/// Whether an object of class c is a `" + type.replace('/', '.') + "`.\n#[allow(dead_code, unused_variables)]\nfn " + name + "(c: u32) -> bool {\n    "
                    + matches(idsOf(type)) + "\n}\n";
        }

        /** A text constant: its String, interned, made the first time it is asked for. */
        String literal(String name, String text) {
            StringBuilder units = new StringBuilder();
            for (int i = 0; i < text.length(); i++) units.append(i == 0 ? "" : ", ").append((int) text.charAt(i));
            return "\n/// " + MethodRust.rustText(text).replace("\n", " ") + "\n#[allow(dead_code)]\nfn " + name + "() -> Ref {\n    static TEXT: LazyLock<Ref> = LazyLock::new(|| jintern(&["
                    + units + "]));\n    TEXT.clone()\n}\n";
        }

        /** The natives that read the class objects' tables: when one is written, the tables are. */
        static final Set<String> CLASS_NATIVES = Set.of("jnative_object_get_class", "jnative_class_name", "jnative_class_simple_name",
                "jnative_class_canonical_name", "jnative_class_instance", "jnative_class_assignable", "jnative_class_flags", "jnative_class_super",
                "jnative_class_enum_values", "jnative_class_primitive");

        /** The class objects' tables, over every type with an id and their supertypes (each given an id here). */
        String classTables() {
            // Every supertype of a type with an id gets one: the tables walk them.
            java.util.Deque<String> todo = new java.util.ArrayDeque<>(world.ids().keySet());
            todo.addAll(List.of(JavaWorld.OBJECT, "java/lang/Cloneable", "java/io/Serializable"));
            for (char k : JavaWorld.PRIMITIVES.toCharArray()) todo.add(JavaWorld.PRIMITIVE + k);
            Set<String> seen = new java.util.HashSet<>();
            while (!todo.isEmpty()) {
                String t = todo.poll();
                if (!seen.add(t)) continue;
                world.id(t);
                if (t.startsWith("[")) {
                    String component = t.substring(1);
                    if (component.length() > 1) todo.add(JavaWorld.type(component));
                } else if (t.startsWith(JavaWorld.LAMBDA)) {
                    todo.add(world.lambdaInterface(t));
                } else {
                    ClassData c = world.knownClass(t);
                    if (c == null) continue;
                    if (c.superName != null) todo.add(c.superName);
                    todo.addAll(c.interfaces);
                }
            }
            Map<String, Integer> ids = new TreeMap<>(world.ids());
            StringBuilder sup = new StringBuilder(), ifs = new StringBuilder(), comp = new StringBuilder(), flags = new StringBuilder(),
                    simple = new StringBuilder(), canonical = new StringBuilder();
            for (Map.Entry<String, Integer> e : ids.entrySet()) {
                String t = e.getKey();
                int id = e.getValue();
                int f = 0;
                if (t.startsWith(JavaWorld.PRIMITIVE)) {
                    f = 8;
                } else if (t.startsWith("[")) {
                    f = 4;
                    String component = t.substring(1);
                    if (component.length() > 1) comp.append("        ").append(id).append(" => ").append(world.id(JavaWorld.type(component))).append(",\n");
                    sup.append("        ").append(id).append(" => ").append(world.id(JavaWorld.OBJECT)).append(",\n");
                } else if (t.startsWith(JavaWorld.LAMBDA)) {
                    sup.append("        ").append(id).append(" => ").append(world.id(JavaWorld.OBJECT)).append(",\n");
                    ifs.append("        ").append(id).append(" => &[").append(world.id(world.lambdaInterface(t))).append("],\n");
                } else {
                    ClassData c = world.knownClass(t);
                    if (c != null) {
                        if ((c.access & ClassData.INTERFACE) != 0) f |= 1;
                        if (world.enumClass(t)) f |= 2;
                        if (c.superName != null && (c.access & ClassData.INTERFACE) == 0) sup.append("        ").append(id).append(" => ").append(world.id(c.superName)).append(",\n");
                        if (!c.interfaces.isEmpty()) {
                            List<String> list = new ArrayList<>();
                            for (String i : c.interfaces) list.add(String.valueOf(world.id(i)));
                            ifs.append("        ").append(id).append(" => &[").append(String.join(", ", list)).append("],\n");
                        }
                    }
                }
                if (f != 0) flags.append("        ").append(id).append(" => ").append(f).append(",\n");
                simple.append("        ").append(id).append(" => ").append(MethodRust.rustText(simpleName(t))).append(",\n");
                String can = canonicalName(t);
                if (can != null) canonical.append("        ").append(id).append(" => Some(").append(MethodRust.rustText(can)).append("),\n");
            }
            StringBuilder primitives = new StringBuilder();
            for (char k : JavaWorld.PRIMITIVES.toCharArray()) primitives.append("        b'").append(k).append("' => ").append(world.id(JavaWorld.PRIMITIVE + k)).append(",\n");
            StringBuilder enums = new StringBuilder();
            for (String e : inst.enums(world)) {
                String fn = functions.get(new Id(e, "values", "()[L" + e + ";"));
                if (fn != null) enums.append("        ").append(world.id(e)).append(" => ").append(fn).append("(),\n");
            }
            JavaWorld.FieldAt idSlot = world.field(MethodRust.CLASS, "id", "I", false);
            if (idSlot.why() != null) throw new IllegalStateException("the library model's Class has no id field");
            return "\n// ---- class objects: one for each type, and their tables ----\n"
                    + "#[allow(dead_code)]\nconst JCLASS: u32 = " + world.id(MethodRust.CLASS) + ";\n"
                    + "#[allow(dead_code)]\nconst JCLASS_LAYOUT: &[u8] = b\"" + world.layout(MethodRust.CLASS) + "\";\n"
                    + "#[allow(dead_code)]\nconst JOBJECT: u32 = " + world.id(JavaWorld.OBJECT) + ";\n"
                    + "#[allow(dead_code)]\nconst JCLONEABLE: u32 = " + world.id("java/lang/Cloneable") + ";\n"
                    + "#[allow(dead_code)]\nconst JSERIALIZABLE: u32 = " + world.id("java/io/Serializable") + ";\n"
                    + "\n/// A class's superclass (0: none, an interface, Object).\n#[allow(dead_code)]\nfn jclass_super(c: u32) -> u32 {\n    match c {\n" + sup + "        _ => 0,\n    }\n}\n"
                    + "\n/// A type's direct interfaces.\n#[allow(dead_code)]\nfn jclass_interfaces(c: u32) -> &'static [u32] {\n    match c {\n" + ifs + "        _ => &[],\n    }\n}\n"
                    + "\n/// An array of references' component type (0: not one).\n#[allow(dead_code)]\nfn jclass_component(c: u32) -> u32 {\n    match c {\n" + comp + "        _ => 0,\n    }\n}\n"
                    + "\n/// 1: an interface, 2: an enum's own class, 4: an array, 8: a primitive type or void.\n#[allow(dead_code)]\nfn jclass_flags(c: u32) -> i32 {\n    match c {\n" + flags + "        _ => 0,\n    }\n}\n"
                    + "\n/// getSimpleName.\n#[allow(dead_code)]\nfn jclass_simple(c: u32) -> &'static str {\n    match c {\n" + simple + "        _ => \"?\",\n    }\n}\n"
                    + "\n/// getCanonicalName (None for a local or anonymous class, a lambda's).\n#[allow(dead_code)]\nfn jclass_canonical(c: u32) -> Option<&'static str> {\n    match c {\n" + canonical + "        _ => None,\n    }\n}\n"
                    + "\n/// The id of a primitive type or void, from its descriptor.\n#[allow(dead_code)]\nfn jclass_primitive(descriptor: i32) -> u32 {\n    match descriptor as u8 {\n" + primitives + "        _ => 0,\n    }\n}\n"
                    + "\n/// An enum's values(), from its class's id; null for any other type.\n#[allow(dead_code)]\nfn jenum_values(c: u32) -> JR<Ref> {\n    match c {\n" + enums + "        _ => Ok(None),\n    }\n}\n"
                    + """

                    /// Whether an object of type a is one of type t (Class.isInstance, isAssignableFrom).
                    #[allow(dead_code)]
                    fn jinstance_of(t: u32, a: u32) -> bool {
                        if a == t {
                            return true;
                        }
                        if jclass_flags(a) & 8 != 0 || jclass_flags(t) & 8 != 0 {
                            return false;
                        }
                        if t == JOBJECT {
                            return true;
                        }
                        if jclass_flags(a) & 4 != 0 {
                            if jclass_flags(t) & 4 == 0 {
                                return t == JCLONEABLE || t == JSERIALIZABLE;
                            }
                            let (ca, ct) = (jclass_component(a), jclass_component(t));
                            return ca != 0 && ct != 0 && jinstance_of(ct, ca);
                        }
                        let mut todo = vec![a];
                        let mut seen = Vec::new();
                        while let Some(c) = todo.pop() {
                            if c == t {
                                return true;
                            }
                            if seen.contains(&c) {
                                continue;
                            }
                            seen.push(c);
                            let s = jclass_super(c);
                            if s != 0 {
                                todo.push(s);
                            }
                            todo.extend_from_slice(jclass_interfaces(c));
                        }
                        false
                    }

                    /// The one class object of a type.
                    #[allow(dead_code)]
                    fn jclass_object(c: u32) -> Ref {
                        static OBJECTS: LazyLock<Mutex<std::collections::HashMap<u32, Ref>>> = LazyLock::new(|| Mutex::new(std::collections::HashMap::new()));
                        let mut objects = jlock(&*OBJECTS);
                        objects
                            .entry(c)
                            .or_insert_with(|| {
                                let o = jnew(JCLASS, JCLASS_LAYOUT);
                                jset(&o, JCLASS_ID, Value::I(c as i32));
                                o
                            })
                            .clone()
                    }
                    """
                    + "#[allow(dead_code)]\nconst JCLASS_ID: usize = " + idSlot.slot() + ";\n";
        }

        /** getSimpleName, as the JDK gives it from the InnerClasses attribute. */
        String simpleName(String t) {
            if (t.startsWith(JavaWorld.PRIMITIVE)) return primitiveName(t.charAt(JavaWorld.PRIMITIVE.length()));
            if (t.startsWith("[")) {
                String component = t.substring(1);
                return (component.length() == 1 ? primitiveName(component.charAt(0)) : simpleName(JavaWorld.type(component))) + "[]";
            }
            if (t.startsWith(JavaWorld.LAMBDA)) {
                String name = t.substring(JavaWorld.LAMBDA.length(), t.indexOf('#')) + "$$Lambda";
                return name.substring(name.lastIndexOf('/') + 1);
            }
            ClassData c = world.knownClass(t);
            if (c != null && c.nested) return c.innerName == null ? "" : c.innerName;
            return t.substring(t.lastIndexOf('/') + 1);
        }

        /** getCanonicalName: null for a local or anonymous class, a lambda's, or an array of one. */
        String canonicalName(String t) {
            if (t.startsWith(JavaWorld.PRIMITIVE)) return primitiveName(t.charAt(JavaWorld.PRIMITIVE.length()));
            if (t.startsWith("[")) {
                String component = t.substring(1);
                if (component.length() == 1) return primitiveName(component.charAt(0)) + "[]";
                String c = canonicalName(JavaWorld.type(component));
                return c == null ? null : c + "[]";
            }
            if (t.startsWith(JavaWorld.LAMBDA)) return null;
            ClassData c = world.knownClass(t);
            if (c != null && c.nested) {
                if (c.outerName == null || c.innerName == null) return null;
                String outer = canonicalName(c.outerName);
                return outer == null ? null : outer + "." + c.innerName;
            }
            return t.replace('/', '.');
        }

        private static String primitiveName(char k) {
            return switch (k) {
                case 'Z' -> "boolean";
                case 'B' -> "byte";
                case 'C' -> "char";
                case 'S' -> "short";
                case 'I' -> "int";
                case 'J' -> "long";
                case 'F' -> "float";
                case 'V' -> "void";
                default -> "double";
            };
        }

        /**
         * The part of the runtime that the model's classes shape: the ids of the exceptions the runtime throws, how
         * their message and cause are kept, how a String keeps its units, each class's name.
         */
        String worldRuntime() {
            // The class objects' tables first: they give ids to supertypes, which jclass_name then names.
            String classes = natives.stream().anyMatch(CLASS_NATIVES::contains) ? classTables() : "";
            StringBuilder b = new StringBuilder("\n// ---- the runtime's exceptions and texts, from the library model ----\n");
            b.append(classes);
            StringBuilder layouts = new StringBuilder();
            for (Map.Entry<String, String> e : Library.THROWN.entrySet()) {
                b.append("#[allow(dead_code)]\nconst ").append(e.getKey()).append(": u32 = ").append(world.concrete(e.getValue()) ? world.id(e.getValue()) : 0).append(";\n");
                if (world.concrete(e.getValue())) layouts.append("        ").append(e.getKey()).append(" => b\"").append(world.layout(e.getValue())).append("\",\n");
            }
            // Each exception the runtime makes has its class's fields (Throwable's suppressed ones among them).
            b.append("\n/// The fields of an exception the runtime throws.\n#[allow(dead_code)]\nfn jthrown_layout(class: u32) -> &'static [u8] {\n    match class {\n")
                    .append(layouts).append("        _ => b\"").append(world.layout("java/lang/Throwable")).append("\",\n    }\n}\n");
            String throwable = "java/lang/Throwable";
            JavaWorld.FieldAt message = world.field(throwable, "detailMessage", "Ljava/lang/String;", false);
            JavaWorld.FieldAt cause = world.field(throwable, "cause", "Ljava/lang/Throwable;", false);
            JavaWorld.FieldAt value = world.field(MethodRust.STRING, "value", "[C", false);
            if (message.why() != null || cause.why() != null || value.why() != null) throw new IllegalStateException("the library model lacks Throwable's or String's fields");
            String errorTest = typeTest("java/lang/Error");
            b.append("const JMESSAGE: usize = ").append(message.slot()).append(";\nconst JCAUSE: usize = ").append(cause.slot()).append(";\nconst JVALUE: usize = ")
                    .append(value.slot()).append(";\n");
            b.append("#[allow(dead_code)]\nconst JSTRING: u32 = ").append(world.id(MethodRust.STRING)).append(";\n");
            b.append("#[allow(dead_code)]\nconst JSTRING_LAYOUT: &[u8] = b\"").append(world.layout(MethodRust.STRING)).append("\";\n");
            b.append("\n/// Each class's binary name, as getClass().getName() gives it.\n#[allow(dead_code)]\nfn jclass_name(c: u32) -> &'static str {\n    match c {\n");
            for (Map.Entry<String, Integer> e : new TreeMap<>(world.ids()).entrySet()) {
                String t = e.getKey();
                String shown = t.startsWith(JavaWorld.LAMBDA) ? t.substring(JavaWorld.LAMBDA.length(), t.indexOf('#')).replace('/', '.') + "$$Lambda"
                        : t.startsWith(JavaWorld.PRIMITIVE) ? primitiveName(t.charAt(JavaWorld.PRIMITIVE.length())) : t.replace('/', '.');
                b.append("        ").append(e.getValue()).append(" => ").append(MethodRust.rustText(shown)).append(",\n");
            }
            b.append("        _ => \"?\",\n    }\n}\n");
            b.append("""

                    /// A String of these UTF-16 units: a char array (class 3) in the model's String.
                    #[allow(dead_code)]
                    fn jstring_of(units: &[u16]) -> Ref {
                        let chars = Some(Arc::new(Obj { class: 3, slots: Mutex::new(units.iter().map(|u| Value::I(*u as i32)).collect()) }));
                        let s = jnew(JSTRING, JSTRING_LAYOUT);
                        jset(&s, JVALUE, Value::A(chars));
                        s
                    }

                    #[allow(dead_code)]
                    fn jstring(text: &str) -> Ref {
                        jstring_of(&text.encode_utf16().collect::<Vec<u16>>())
                    }

                    /// A String's units.
                    #[allow(dead_code)]
                    fn jstring_units(s: &Ref) -> JR<Vec<u16>> {
                        let chars = jga(jget(s, JVALUE)?);
                        let units = jlock(&jobj(&chars)?.slots).iter().map(|v| jgi(v.clone()) as u16).collect();
                        Ok(units)
                    }

                    /// A String as Rust text (unpaired surrogates as U+FFFD).
                    #[allow(dead_code)]
                    fn jstring_text(s: &Ref) -> String {
                        match jstring_units(s) {
                            Ok(units) => String::from_utf16_lossy(&units),
                            Err(_) => "null".to_string(),
                        }
                    }

                    /// One String of each text, the literals' among them (String.intern).
                    #[allow(dead_code)]
                    fn jintern(units: &[u16]) -> Ref {
                        static TABLE: LazyLock<Mutex<std::collections::HashMap<Vec<u16>, Ref>>> = LazyLock::new(|| Mutex::new(std::collections::HashMap::new()));
                        let mut table = jlock(&*TABLE);
                        if let Some(s) = table.get(units) {
                            return s.clone();
                        }
                        let s = jstring_of(units);
                        table.insert(units.to_vec(), s.clone());
                        s
                    }

                    /// An exception the runtime throws, as its constructor with a message would make it: its cause not yet given.
                    #[allow(dead_code)]
                    fn jexception(class: u32, message: Option<&str>) -> Ref {
                        let e = jnew(class, jthrown_layout(class));
                        jset(&e, JMESSAGE, Value::A(message.map(jstring).unwrap_or(None)));
                        jset(&e, JCAUSE, Value::A(e.clone()));
                        e
                    }

                    /// What a class initializer threw, as the JVM hands it on: an Error as it is, anything else in an ExceptionInInitializerError.
                    #[allow(dead_code)]
                    fn jinit_failed(e: Ref) -> Ref {
                        if jinstanceof(&e, %s) == 1 {
                            return e;
                        }
                        let x = jexception(JID_INITIALIZER, None);
                        jset(&x, JMESSAGE, Value::A(None));
                        jset(&x, JCAUSE, Value::A(e));
                        x
                    }

                    /// An exception as a line: its class, and its message when it has one.
                    #[allow(dead_code)]
                    pub fn jdescribe(e: &Ref) -> String {
                        let Some(o) = e else { return "null".to_string() };
                        let message = jlock(&o.slots).get(JMESSAGE).cloned();
                        match message {
                            Some(Value::A(Some(m))) => format!("{}: {}", jclass_name(o.class), jstring_text(&Some(m))),
                            _ => jclass_name(o.class).to_string(),
                        }
                    }
                    """.formatted(errorTest));
            return b.toString();
        }

        /** What aastore lets into each array of references: the instances of its component type. */
        String accepts() {
            StringBuilder b = new StringBuilder("\n/// Whether an array of class `array` takes an object of class `value` (aastore's ArrayStoreException).\n");
            b.append("#[allow(dead_code, unused_variables)]\nfn jaccepts(array: u32, value: u32) -> bool {\n    let c = value;\n    match array {\n");
            // Only where a translated method stores into an array of references: then each answer is certain (settle).
            boolean stores = needs.values().stream().flatMap(List::stream).anyMatch(n -> n.kind().equals("test") && n.type().equals("aastore"));
            for (String a : stores ? inst.arrays() : Set.<String>of()) {
                if (a.length() == 2) continue;
                b.append("        ").append(world.id(a)).append(" => ").append(matches(idsOf(JavaWorld.type(a.substring(1))))).append(",\n");
            }
            b.append("        _ => true,\n    }\n}\n");
            return b.toString();
        }

        /** A negative literal without the parentheses it has as an operand: a whole argument needs none. */
        static String bare(String literal) {
            return literal.startsWith("(-") && literal.endsWith(")") && literal.indexOf('(', 1) < 0 ? literal.substring(1, literal.length() - 1) : literal;
        }

        String staticStore(String c) {
            StringBuilder b = new StringBuilder("\n/// The static fields of `" + c.replace('/', '.') + "`, as prepared before it initializes.\n");
            b.append("static JSTATICS_").append(index(c)).append(": LazyLock<Mutex<Vec<Value>>> = LazyLock::new(|| Mutex::new(vec![");
            List<String> values = new ArrayList<>();
            for (ClassData.Field f : world.statics(c)) {
                char k = JavaWorld.kind(f.descriptor());
                Object constant = f.constant();
                if (constant instanceof String text) {
                    values.add("Value::A(" + literal(text) + "())");
                    continue;
                }
                values.add(constant instanceof Number n && k != 'A' ? MethodRust.wrap(k, bare(switch (k) {
                    case 'J' -> MethodRust.literal(n.longValue());
                    case 'F' -> MethodRust.literal(n.floatValue());
                    case 'D' -> MethodRust.literal(n.doubleValue());
                    default -> MethodRust.literal(n.intValue());
                })) : "jzero(b'" + k + "')");
            }
            b.append(String.join(", ", values)).append("]));\n");
            return b.toString();
        }

        String initializer(String c) {
            int n = index(c);
            StringBuilder b = new StringBuilder("\n/// `" + c.replace('/', '.') + "` initialized once, its superclass first (JVMS 5.5).\n");
            b.append("static JINIT_").append(n).append(": JInit = JInit::new();\n#[allow(dead_code)]\nfn jinit_").append(n).append("() -> JR<()> {\n    jinit(&JINIT_").append(n)
                    .append(", ").append(MethodRust.rustText(c.replace('/', '.'))).append(", || {\n");
            ClassData data = world.cls(c);
            if (data.superName != null && world.cls(data.superName) != null && (data.access & ClassData.INTERFACE) == 0) {
                b.append("        ").append(init(data.superName)).append("()?;\n");
            }
            if (world.declared(c, "<clinit>", "()V") != null) b.append("        ").append(function(new Id(c, "<clinit>", "()V"))).append("()?;\n");
            b.append("        Ok(())\n    })\n}\n");
            return b.toString();
        }

        /** A dispatcher: the method each instantiated class selects, by its class id. */
        String dispatch(String key) {
            String name = dispatchers.get(key);
            int paren = key.indexOf('(');
            String head = key.substring(0, paren), descriptor = key.substring(paren);
            String owner = head.substring(0, head.lastIndexOf('.')), method = head.substring(head.lastIndexOf('.') + 1);
            List<String> params = ClassData.parameters(descriptor);
            String r = ClassData.returns(descriptor);
            StringBuilder sig = new StringBuilder("this: Ref"), args = new StringBuilder("this");
            for (int k = 0; k < params.size(); k++) {
                sig.append(", a").append(k).append(": ").append(MethodRust.rust(params.get(k)));
                args.append(", a").append(k);
            }
            Map<String, List<Integer>> arms = new TreeMap<>();
            for (String a : inst.arrays()) {
                if (!Boolean.TRUE.equals(world.subtype(a, owner))) continue;
                arms.computeIfAbsent(function(arraySelects(world, method, descriptor).id()), x -> new ArrayList<>()).add(world.id(a));
            }
            for (String c : inst.classes()) {
                if (!Boolean.TRUE.equals(world.subtype(c, owner))) continue;
                arms.computeIfAbsent(function(world.select(c, method, descriptor).id()), x -> new ArrayList<>()).add(world.id(c));
            }
            for (String l : inst.lambdas()) {
                if (!Boolean.TRUE.equals(world.subtype(l, owner))) continue;
                ClassData.LambdaSite site = world.lambdas.get(l);
                String target = site.samName().equals(method) && site.erased().equals(descriptor) ? "jlambda_" + world.id(l) + "_call"
                        : function(world.selectOnLambda(l, method, descriptor).id());
                arms.computeIfAbsent(target, x -> new ArrayList<>()).add(world.id(l));
            }
            StringBuilder b = new StringBuilder("\n/// `" + owner.replace('/', '.') + "." + method + descriptor + "`, called virtually: the method each class selects.\n");
            b.append("#[allow(dead_code, non_snake_case, unused_variables)]\nfn ").append(name).append('(').append(sig).append(") -> JR<").append(r.equals("V") ? "()" : MethodRust.rust(r)).append("> {\n");
            b.append("    match jclass(&this)? {\n");
            arms.forEach((target, ids) -> b.append("        ").append(String.join(" | ", ids.stream().sorted().map(String::valueOf).toList())).append(" => ")
                    .append(target).append('(').append(args).append("),\n"));
            b.append("        _ => unreachable!(),\n    }\n}\n");
            return b.toString();
        }

        /** A record's equals, hashCode or toString, as ObjectMethods.bootstrap makes it. */
        String objectMethodsFn(String key) {
            MethodRust.ObjectMethodsSite site = world.objectMethods.get(key);
            String name = objectMethodNames.get(key);
            StringBuilder b = new StringBuilder("\n/// The record `" + site.record().replace('/', '.') + "`'s " + site.method() + ", as ObjectMethods makes it.\n");
            b.append("#[allow(dead_code, non_snake_case, unused_parens, unused_variables, unused_mut)]\n");
            List<JavaWorld.FieldAt> fields = site.fields();
            switch (site.method()) {
                case "toString" -> {
                    String simple = site.record().substring(site.record().lastIndexOf('/') + 1);
                    simple = simple.substring(simple.lastIndexOf('$') + 1).replaceFirst("^[0-9]+", "");
                    List<String> pieces = new ArrayList<>();
                    StringBuilder text = new StringBuilder(simple).append('[');
                    for (int k = 0; k < fields.size(); k++) {
                        text.append(k == 0 ? "" : ", ").append(site.names().get(k)).append('=');
                        pieces.add(literal(text.toString()) + "()");
                        text.setLength(0);
                        JavaWorld.FieldAt f = fields.get(k);
                        String value = MethodRust.getter(f.kind()) + "(jget(&this, " + f.slot() + ")?)";
                        String d = f.descriptor().length() == 1 && (f.descriptor().equals("B") || f.descriptor().equals("S")) ? "I" : f.descriptor();
                        pieces.add(function(MethodRust.textOf(f.descriptor())) + "(" + Writing.toEdge(d, value) + ")?");
                    }
                    text.append(']');
                    pieces.add(literal(text.toString()) + "()");
                    b.append("fn ").append(name).append("(this: Ref) -> JR<Ref> {\n");
                    b.append("    let parts = jnewarray(").append(world.id("[Ljava/lang/String;")).append(", b'A', ").append(pieces.size()).append(")?;\n");
                    for (int k = 0; k < pieces.size(); k++) b.append("    japut(&parts, ").append(k).append(", Value::A(").append(pieces.get(k)).append("))?;\n");
                    b.append("    ").append(function(MethodRust.CONCAT)).append("(parts)\n}\n");
                }
                case "hashCode" -> {
                    b.append("fn ").append(name).append("(this: Ref) -> JR<i32> {\n    let mut h: i32 = 0;\n");
                    for (JavaWorld.FieldAt f : fields) {
                        String value = MethodRust.getter(f.kind()) + "(jget(&this, " + f.slot() + ")?)";
                        b.append("    h = h.wrapping_mul(31).wrapping_add(").append(function(MethodRust.hashOf(f.descriptor()))).append('(')
                                .append(Writing.toEdge(f.descriptor(), value)).append(")?);\n");
                    }
                    b.append("    Ok(h)\n}\n");
                }
                default -> {
                    b.append("fn ").append(name).append("(this: Ref, o: Ref) -> JR<bool> {\n");
                    b.append("    if jinstanceof(&o, ").append(typeTest(site.record())).append(") == 0 {\n        return Ok(false);\n    }\n");
                    for (JavaWorld.FieldAt f : fields) {
                        String g = MethodRust.getter(f.kind());
                        String mine = g + "(jget(&this, " + f.slot() + ")?)", theirs = g + "(jget(&o, " + f.slot() + ")?)";
                        JarRust.Id eq = MethodRust.equalsOf(f.descriptor());
                        String differs = eq == null ? mine + " != " + theirs
                                : f.kind() == 'A' ? "!" + function(eq) + "(" + mine + ", " + theirs + ")?"
                                : function(eq) + "(" + mine + ", " + theirs + ")? != 0";
                        b.append("    if ").append(differs).append(" {\n        return Ok(false);\n    }\n");
                    }
                    b.append("    Ok(true)\n}\n");
                }
            }
            return b.toString();
        }

        /** A lambda's class: made with its captured values, run through its interface's method. */
        String lambda(String key) {
            ClassData.LambdaSite site = world.lambdas.get(key);
            int id = world.id(key);
            List<String> captured = ClassData.parameters(site.site());
            List<String> sam = ClassData.parameters(site.erased());
            StringBuilder layout = new StringBuilder();
            for (String c : captured) layout.append(JavaWorld.kind(c));
            StringBuilder b = new StringBuilder("\n/// A lambda of `" + JavaWorld.type(ClassData.returns(site.site())).replace('/', '.') + "` that runs `"
                    + site.owner().replace('/', '.') + "." + site.name() + site.descriptor() + "`.\n");
            b.append("#[allow(dead_code, non_snake_case, unused_parens)]\nfn jlambda_").append(id).append("_new(");
            for (int k = 0; k < captured.size(); k++) b.append(k == 0 ? "" : ", ").append("c").append(k).append(": ").append(MethodRust.rust(captured.get(k)));
            b.append(") -> Ref {\n    let o = jnew(").append(id).append(", b\"").append(layout).append("\");\n");
            for (int k = 0; k < captured.size(); k++) {
                char kind = JavaWorld.kind(captured.get(k));
                b.append("    jset(&o, ").append(k).append(", ").append(MethodRust.wrap(kind, fromEdge(captured.get(k), "c" + k))).append(");\n");
            }
            b.append("    o\n}\n");

            String r = ClassData.returns(site.erased());
            b.append("#[allow(dead_code, non_snake_case, unused_variables, unused_parens)]\nfn jlambda_").append(id).append("_call(this: Ref");
            for (int k = 0; k < sam.size(); k++) b.append(", a").append(k).append(": ").append(MethodRust.rust(sam.get(k)));
            b.append(") -> JR<").append(r.equals("V") ? "()" : MethodRust.rust(r)).append("> {\n");
            List<String> given = new ArrayList<>(), types = new ArrayList<>();
            for (int k = 0; k < captured.size(); k++) {
                char kind = JavaWorld.kind(captured.get(k));
                b.append("    let c").append(k).append(": ").append(MethodRust.rust(captured.get(k))).append(" = ")
                        .append(toEdge(captured.get(k), MethodRust.getter(kind) + "(jget(&this, " + k + ")?)")).append(";\n");
                given.add("c" + k);
                types.add(captured.get(k));
            }
            for (int k = 0; k < sam.size(); k++) {
                given.add("a" + k);
                types.add(sam.get(k));
            }
            List<String> taken = new ArrayList<>();
            if (site.kind() == 5 || site.kind() == 7 || site.kind() == 9) taken.add("L" + site.owner() + ";");
            taken.addAll(ClassData.parameters(site.descriptor()));
            List<String> args = new ArrayList<>();
            for (int k = 0; k < given.size(); k++) args.add(adapt(given.get(k), types.get(k), taken.get(k)));
            String call;
            Id impl = new Id(site.owner(), site.name(), site.descriptor());
            // A private method (an interface's own lambda body among them) isn't selected: it is called as it is.
            ClassData.Method target = world.declared(site.owner(), site.name(), site.descriptor());
            boolean direct = target != null && (target.access() & 0x0002) != 0;
            switch (direct && (site.kind() == 5 || site.kind() == 9) ? 7 : site.kind()) {
                case 5, 9 -> call = dispatcher(site.owner(), site.name(), site.descriptor()) + "(" + String.join(", ", args) + ")?";
                case 8 -> {
                    b.append("    ").append(init(site.owner())).append("()?;\n");
                    b.append("    let o = jnew(").append(world.id(site.owner())).append(", b\"").append(world.layout(site.owner())).append("\");\n");
                    List<String> withThis = new ArrayList<>(List.of("o.clone()"));
                    withThis.addAll(args);
                    b.append("    ").append(function(impl)).append('(').append(String.join(", ", withThis)).append(")?;\n");
                    call = "o";
                }
                default -> call = function(impl) + "(" + String.join(", ", args) + ")?";
            }
            String ir = site.kind() == 8 ? "L" + site.owner() + ";" : ClassData.returns(site.descriptor());
            if (r.equals("V")) b.append("    ").append(call).append(";\n    Ok(())\n");
            else b.append("    Ok(").append(adapt(call, ir, r)).append(")\n");
            b.append("}\n");
            return b.toString();
        }

        /**
         * A value of descriptor from, as Rust at a function's edge, made one of descriptor to as LambdaMetafactory's
         * adapter does: widened, boxed, unboxed (cast into the box first when it isn't one), or cast. The check
         * (`MethodRust.adaptNeeds`) made sure each is possible and translated.
         */
        String adapt(String e, String from, String to) {
            boolean pf = MethodRust.primitive(from), pt = MethodRust.primitive(to);
            if (pf && pt) return from.equals(to) ? e : "(" + e + " as " + MethodRust.rust(to) + ")";
            if (!pf && !pt) {
                return from.equals(to) || to.equals("Ljava/lang/Object;") ? e : "jcheckcast(" + e + ", " + typeTest(JavaWorld.type(to)) + ")?";
            }
            if (pf) {
                String b = MethodRust.box(from);
                return function(new Id(b, "valueOf", "(" + from + ")L" + b + ";")) + "(" + e + ")?";
            }
            String held = MethodRust.unboxed(from);
            String through = held != null ? held : to;
            String b = MethodRust.box(through);
            String object = held != null ? e : "jcheckcast(" + e + ", " + typeTest(b) + ")?";
            String value = dispatcher(b, MethodRust.UNBOX.get(through), "()" + through) + "(" + object + ")?";
            return through.equals(to) ? value : "(" + value + " as " + MethodRust.rust(to) + ")";
        }

        /** A value of a descriptor's type at a function's edge, as the int a slot keeps. */
        static String fromEdge(String d, String e) {
            return "ZBCS".indexOf(d.charAt(0)) >= 0 ? "(" + e + " as i32)" : e;
        }

        static String toEdge(String d, String e) {
            return switch (d) {
                case "Z" -> "(" + e + " != 0)";
                case "B" -> "(" + e + " as i8)";
                case "C" -> "(" + e + " as u16)";
                case "S" -> "(" + e + " as i16)";
                default -> e;
            };
        }
    }

    /** The uniform object model's runtime, written once at the top of the module. */
    static final String RUNTIME = """

            #[allow(unused_imports)]
            use std::sync::{Arc, Condvar, LazyLock, Mutex, MutexGuard};

            /// A field, a static field or an array element: one of Java's values.
            #[derive(Clone, Debug)]
            #[allow(dead_code)]
            pub enum Value {
                I(i32),
                J(i64),
                F(f32),
                D(f64),
                A(Ref),
            }

            /// A reference: None is null.
            pub type Ref = Option<Arc<Obj>>;

            /// What a method gives: its value, or the exception it throws.
            pub type JR<T> = Result<T, Ref>;

            /// How a block in a try range ends: on to the next block, or out of the method with its value.
            #[allow(dead_code)]
            pub enum JFlow<T> {
                Next,
                Return(T),
            }

            /// An object: its class id, and its fields (an array's elements) in slots.
            #[derive(Debug)]
            pub struct Obj {
                pub class: u32,
                pub slots: Mutex<Vec<Value>>,
            }

            #[allow(dead_code)]
            fn jlock<T>(m: &Mutex<T>) -> MutexGuard<'_, T> {
                m.lock().unwrap_or_else(|e| e.into_inner())
            }

            #[allow(dead_code)]
            fn jzero(t: u8) -> Value {
                match t {
                    b'J' => Value::J(0),
                    b'F' => Value::F(0.0),
                    b'D' => Value::D(0.0),
                    b'A' => Value::A(None),
                    _ => Value::I(0),
                }
            }

            #[allow(dead_code)]
            fn jnew(class: u32, layout: &[u8]) -> Ref {
                Some(Arc::new(Obj { class, slots: Mutex::new(layout.iter().map(|t| jzero(*t)).collect()) }))
            }

            /// A slot of an object just made (never null).
            #[allow(dead_code)]
            fn jset(r: &Ref, slot: usize, v: Value) {
                if let Some(o) = r {
                    jlock(&o.slots)[slot] = v;
                }
            }

            #[allow(dead_code)]
            fn jobj(r: &Ref) -> JR<&Arc<Obj>> {
                match r {
                    Some(o) => Ok(o),
                    None => Err(jexception(JID_NULL_POINTER, None)),
                }
            }

            #[allow(dead_code)]
            fn jclass(r: &Ref) -> JR<u32> {
                Ok(jobj(r)?.class)
            }

            #[allow(dead_code)]
            fn jget(r: &Ref, slot: usize) -> JR<Value> {
                Ok(jlock(&jobj(r)?.slots)[slot].clone())
            }

            #[allow(dead_code)]
            fn jput(r: &Ref, slot: usize, v: Value) -> JR<()> {
                jlock(&jobj(r)?.slots)[slot] = v;
                Ok(())
            }

            #[allow(dead_code)]
            fn jgi(v: Value) -> i32 {
                match v {
                    Value::I(x) => x,
                    _ => unreachable!(),
                }
            }

            #[allow(dead_code)]
            fn jgj(v: Value) -> i64 {
                match v {
                    Value::J(x) => x,
                    _ => unreachable!(),
                }
            }

            #[allow(dead_code)]
            fn jgf(v: Value) -> f32 {
                match v {
                    Value::F(x) => x,
                    _ => unreachable!(),
                }
            }

            #[allow(dead_code)]
            fn jgd(v: Value) -> f64 {
                match v {
                    Value::D(x) => x,
                    _ => unreachable!(),
                }
            }

            #[allow(dead_code)]
            fn jga(v: Value) -> Ref {
                match v {
                    Value::A(x) => x,
                    _ => unreachable!(),
                }
            }

            /// Java's == on references.
            #[allow(dead_code)]
            fn jsame(a: &Ref, b: &Ref) -> bool {
                match (a, b) {
                    (None, None) => true,
                    (Some(x), Some(y)) => Arc::ptr_eq(x, y),
                    _ => false,
                }
            }

            /// Java's monitors: by object, the thread that holds it and how many times (they are reentrant), and how
            /// many notifies it had (for wait).
            #[allow(dead_code)]
            struct JMonitors {
                held: std::collections::HashMap<usize, (std::thread::ThreadId, u32)>,
                notified: std::collections::HashMap<usize, u64>,
            }

            #[allow(dead_code)]
            static JMONITORS: LazyLock<(Mutex<JMonitors>, Condvar)> =
                LazyLock::new(|| (Mutex::new(JMonitors { held: std::collections::HashMap::new(), notified: std::collections::HashMap::new() }), Condvar::new()));

            thread_local! {
                /// The current thread's Thread object, once asked for or started.
                #[allow(dead_code)]
                static JCURRENT: std::cell::RefCell<Ref> = const { std::cell::RefCell::new(None) };
            }

            /// monitorenter: waits while another thread holds the object's monitor.
            #[allow(dead_code)]
            fn jmonitor_enter(r: &Ref) -> JR<()> {
                let key = Arc::as_ptr(jobj(r)?) as usize;
                let me = std::thread::current().id();
                let (lock, released) = &*JMONITORS;
                let mut m = jlock(lock);
                loop {
                    match m.held.get_mut(&key) {
                        None => {
                            m.held.insert(key, (me, 1));
                            return Ok(());
                        }
                        Some((owner, count)) if *owner == me => {
                            *count += 1;
                            return Ok(());
                        }
                        _ => m = released.wait(m).unwrap_or_else(|e| e.into_inner()),
                    }
                }
            }

            /// monitorexit: an IllegalMonitorStateException unless this thread holds the monitor.
            #[allow(dead_code)]
            fn jmonitor_exit(r: &Ref) -> JR<()> {
                let key = Arc::as_ptr(jobj(r)?) as usize;
                let me = std::thread::current().id();
                let (lock, released) = &*JMONITORS;
                let mut m = jlock(lock);
                match m.held.get_mut(&key) {
                    Some((owner, count)) if *owner == me => {
                        *count -= 1;
                        if *count == 0 {
                            m.held.remove(&key);
                            released.notify_all();
                        }
                        Ok(())
                    }
                    _ => Err(jexception(JID_ILLEGAL_MONITOR, None)),
                }
            }

            /// athrow: the exception, or a NullPointerException for null.
            #[allow(dead_code)]
            fn jthrow(e: Ref) -> Ref {
                if e.is_none() { jexception(JID_NULL_POINTER, None) } else { e }
            }

            #[allow(dead_code)]
            fn jdiv_i32(a: i32, b: i32) -> JR<i32> {
                if b == 0 { Err(jexception(JID_ARITHMETIC, Some("/ by zero"))) } else { Ok(a.wrapping_div(b)) }
            }

            #[allow(dead_code)]
            fn jrem_i32(a: i32, b: i32) -> JR<i32> {
                if b == 0 { Err(jexception(JID_ARITHMETIC, Some("/ by zero"))) } else { Ok(a.wrapping_rem(b)) }
            }

            #[allow(dead_code)]
            fn jdiv_i64(a: i64, b: i64) -> JR<i64> {
                if b == 0 { Err(jexception(JID_ARITHMETIC, Some("/ by zero"))) } else { Ok(a.wrapping_div(b)) }
            }

            #[allow(dead_code)]
            fn jrem_i64(a: i64, b: i64) -> JR<i64> {
                if b == 0 { Err(jexception(JID_ARITHMETIC, Some("/ by zero"))) } else { Ok(a.wrapping_rem(b)) }
            }

            #[allow(dead_code)]
            fn jnewarray(class: u32, kind: u8, n: i32) -> JR<Ref> {
                if n < 0 {
                    return Err(jexception(JID_NEGATIVE_SIZE, Some(&n.to_string())));
                }
                Ok(Some(Arc::new(Obj { class, slots: Mutex::new(vec![jzero(kind); n as usize]) })))
            }

            #[allow(dead_code)]
            fn jindex(len: usize, i: i32) -> JR<usize> {
                if i < 0 || i as usize >= len {
                    return Err(jexception(JID_ARRAY_INDEX, Some(&format!("Index {} out of bounds for length {}", i, len))));
                }
                Ok(i as usize)
            }

            #[allow(dead_code)]
            fn jaget(r: &Ref, i: i32) -> JR<Value> {
                let s = jlock(&jobj(r)?.slots);
                let k = jindex(s.len(), i)?;
                Ok(s[k].clone())
            }

            #[allow(dead_code)]
            fn japut(r: &Ref, i: i32, v: Value) -> JR<()> {
                let mut s = jlock(&jobj(r)?.slots);
                let k = jindex(s.len(), i)?;
                s[k] = v;
                Ok(())
            }

            #[allow(dead_code)]
            fn jalen(r: &Ref) -> JR<i32> {
                Ok(jlock(&jobj(r)?.slots).len() as i32)
            }

            /// bastore: a boolean array (class 1) keeps the lowest bit, a byte array the lowest byte.
            #[allow(dead_code)]
            fn jbastore(r: &Ref, i: i32, v: i32) -> JR<()> {
                let boolean = jclass(r)? == 1;
                japut(r, i, Value::I(if boolean { v & 1 } else { (v as i8) as i32 }))
            }

            /// aastore: null first, then the index, then the store's type (ArrayStoreException, named by the value's class).
            #[allow(dead_code)]
            fn jaastore(r: &Ref, i: i32, v: Ref) -> JR<()> {
                let o = jobj(r)?;
                let mut s = jlock(&o.slots);
                let k = jindex(s.len(), i)?;
                if let Some(x) = &v {
                    if !jaccepts(o.class, x.class) {
                        return Err(jexception(JID_ARRAY_STORE, Some(jclass_name(x.class))));
                    }
                }
                s[k] = Value::A(v);
                Ok(())
            }

            #[allow(dead_code)]
            fn jsget(s: &LazyLock<Mutex<Vec<Value>>>, slot: usize) -> Value {
                jlock(&**s)[slot].clone()
            }

            #[allow(dead_code)]
            fn jsput(s: &LazyLock<Mutex<Vec<Value>>>, slot: usize, v: Value) {
                jlock(&**s)[slot] = v;
            }

            /// checkcast; a ClassCastException has no message here (the JDK's names class loaders).
            #[allow(dead_code)]
            fn jcheckcast(r: Ref, ok: fn(u32) -> bool) -> JR<Ref> {
                if let Some(o) = &r {
                    if !ok(o.class) {
                        return Err(jexception(JID_CLASS_CAST, None));
                    }
                }
                Ok(r)
            }

            #[allow(dead_code)]
            fn jinstanceof(r: &Ref, ok: fn(u32) -> bool) -> i32 {
                match r {
                    Some(o) if ok(o.class) => 1,
                    _ => 0,
                }
            }

            /// A class's initialization (JVMS 5.5): once; the thread that runs it may come back to it, the others wait.
            /// A class whose initializer threw is erroneous: using it again throws NoClassDefFoundError.
            pub struct JInit {
                state: Mutex<(u8, Option<std::thread::ThreadId>)>,
                done: Condvar,
            }

            #[allow(dead_code)]
            impl JInit {
                pub const fn new() -> JInit {
                    JInit { state: Mutex::new((0, None)), done: Condvar::new() }
                }
            }

            #[allow(dead_code)]
            fn jinit(i: &JInit, name: &str, run: fn() -> JR<()>) -> JR<()> {
                let me = std::thread::current().id();
                let mut s = jlock(&i.state);
                loop {
                    match *s {
                        (2, _) => return Ok(()),
                        (3, _) => return Err(jexception(JID_NO_CLASS, Some(&format!("Could not initialize class {}", name)))),
                        (1, Some(t)) if t == me => return Ok(()),
                        (1, _) => s = i.done.wait(s).unwrap_or_else(|e| e.into_inner()),
                        _ => break,
                    }
                }
                *s = (1, Some(me));
                drop(s);
                let result = run();
                *jlock(&i.state) = (if result.is_ok() { 2 } else { 3 }, None);
                i.done.notify_all();
                result.map_err(jinit_failed)
            }
            """;

    /** Each method's Rust name: `class_name__method_name`, made unique by its descriptor, then by a number. */
    static Map<Id, String> names(Set<Id> ids) {
        Map<String, List<Id>> byBase = new TreeMap<>();
        for (Id id : new TreeSet<>(ids)) byBase.computeIfAbsent(snake(simple(id.owner())) + "__" + snake(id.name()), k -> new ArrayList<>()).add(id);
        Map<Id, String> out = new HashMap<>();
        Set<String> taken = new TreeSet<>();
        byBase.forEach((base, list) -> {
            for (Id id : list) {
                String name = list.size() == 1 ? base : base + "__" + descriptorWord(id.descriptor());
                String unique = name;
                for (int n = 2; !taken.add(unique); n++) unique = name + "_" + n;
                out.put(id, unique);
            }
        });
        return out;
    }

    private static String simple(String internal) {
        return internal.substring(internal.lastIndexOf('/') + 1);
    }

    private static String descriptorWord(String descriptor) {
        String params = descriptor.substring(1, descriptor.indexOf(')'));
        if (params.isEmpty()) return "v";
        StringBuilder b = new StringBuilder();
        for (String p : ClassData.parameters(descriptor)) {
            if (b.length() > 0) b.append('_');
            b.append(p.startsWith("L") ? snake(simple(JavaWorld.type(p))) : p.startsWith("[") ? "arr_" + snake(simple(p.replaceAll("[\\[L;]", ""))) : p.toLowerCase(java.util.Locale.ROOT));
        }
        return b.toString();
    }

    /** `MathUtil$Inner` as `math_util_inner`, `lambda$x$0` as `lambda_x_0`. */
    static String snake(String name) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                boolean afterSmall = i > 0 && (Character.isLowerCase(name.charAt(i - 1)) || Character.isDigit(name.charAt(i - 1)));
                boolean beforeSmall = i > 0 && Character.isUpperCase(name.charAt(i - 1)) && i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1));
                if ((afterSmall || beforeSmall) && b.length() > 0 && b.charAt(b.length() - 1) != '_') b.append('_');
                b.append(Character.toLowerCase(c));
            } else if (c < 128 && Character.isLetterOrDigit(c)) {
                b.append(c);
            } else if (b.length() > 0 && b.charAt(b.length() - 1) != '_') {
                b.append('_');
            }
        }
        String s = b.toString().replaceAll("_+$", "");
        if (s.isEmpty() || Character.isDigit(s.charAt(0))) s = "m_" + s;
        return s;
    }

    /** translation.md: the count, the reasons, and each method left out with why. */
    static String report(Result r, int horizonPort) {
        StringBuilder b = new StringBuilder("# What horizon-port-neoforge's own translator translated\n\n");
        b.append(r.translated().size()).append(" of ").append(r.methods()).append(" methods with a body, from their bytecode");
        if (horizonPort >= 0) b.append(" (horizon-port, from decompiled Java: ").append(horizonPort).append(")");
        b.append(". Objects, arrays, fields, inheritance, lambdas, exceptions and the library model (texts, boxes, collections, Class objects, streams...) are translated; the game comes next.\n\n");
        b.append("| Why the others aren't | Methods |\n|---|---|\n");
        r.reasons().forEach((w, n) -> b.append("| ").append(w.replace("|", "\\|")).append(" | ").append(n).append(" |\n"));
        b.append("\n## Translated\n\n");
        r.translated().forEach((id, name) -> b.append("- `").append(id.display()).append("` as `").append(name).append("`\n"));
        b.append("\n## Not translated\n\n");
        r.why().forEach((id, w) -> b.append("- `").append(id.display()).append("`: ").append(w).append('\n'));
        return b.toString();
    }
}
