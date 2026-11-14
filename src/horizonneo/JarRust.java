package horizonneo;

import horizonneo.MethodRust.Need;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        return translate(classes);
    }

    static Result translate(Map<String, ClassData> classes) throws IOException {
        return translate(classes, Library.classes());
    }

    /** jar: the mod's classes; model: the library model's (`Library`), translated with them where the mod reaches them. */
    static Result translate(Map<String, ClassData> jar, Map<String, ClassData> model) {
        JavaWorld world = new JavaWorld(jar, model);
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
        // Written until every method written has what it needs written too: one whose flow can't be followed drops out.
        while (true) {
            settle(world, needs, why);
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
                    case "call", "lambda" -> todo.add(n.id());
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
                            JavaWorld.Selected sel = world.select(world.lambdaInterface(l), n.name(), n.descriptor());
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

    /** What the translated methods instantiate: classes and arrays they make, lambda sites they reach. */
    record Instances(Set<String> classes, Set<String> arrays, Set<String> lambdas) {
        List<String> all() {
            List<String> out = new ArrayList<>(classes);
            out.addAll(arrays);
            out.addAll(lambdas);
            return out;
        }
    }

    static Instances instances(JavaWorld world, Map<Id, List<Need>> needs) {
        Set<String> classes = new TreeSet<>(), arrays = new TreeSet<>(), lambdas = new TreeSet<>();
        // What the runtime makes itself: the exceptions it throws, with their messages.
        for (String c : Library.thrownClasses()) if (world.concrete(c)) classes.add(c);
        if (world.concrete(MethodRust.STRING)) {
            classes.add(MethodRust.STRING);
            arrays.add("[C");
        }
        for (List<Need> list : needs.values()) {
            for (Need n : list) {
                if (n.kind().equals("new")) (n.type().startsWith("[") ? arrays : classes).add(n.type());
                if (n.kind().equals("lambda")) lambdas.add(n.type());
            }
        }
        return new Instances(classes, arrays, lambdas);
    }

    private static void settle(JavaWorld world, Map<Id, List<Need>> needs, Map<Id, String> why) {
        for (boolean changed = true; changed; ) {
            changed = false;
            Instances inst = instances(world, needs);
            for (Id id : List.copyOf(needs.keySet())) {
                for (Need n : needs.get(id)) {
                    String reason = unmet(n, world, needs, why, inst);
                    if (reason == null) continue;
                    why.put(id, reason);
                    needs.remove(id);
                    changed = true;
                    break;
                }
            }
        }
    }

    /** Why a need isn't met by what is translated; null when it is. */
    static String unmet(Need n, JavaWorld world, Map<Id, List<Need>> needs, Map<Id, String> why, Instances inst) {
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
                    if (sub == null) return "a virtual call isn't certain: whether " + c.replace('/', '.') + " is a " + n.type().replace('/', '.') + " can't be told";
                    if (!sub) continue;
                    JavaWorld.Selected s = world.select(c, n.name(), n.descriptor());
                    if (s.why() != null) return "a virtual call isn't certain: " + n.type().replace('/', '.') + "." + n.name() + " " + s.why();
                    String r = untranslated("it calls " + n.type().replace('/', '.') + "." + n.name() + ", which may run ", s.id(), needs, why);
                    if (r != null) return r;
                }
                for (String key : inst.lambdas()) {
                    Boolean sub = world.subtype(key, n.type());
                    if (sub == null) return "a virtual call isn't certain: whether a lambda of " + world.lambdaInterface(key).replace('/', '.') + " is a " + n.type().replace('/', '.') + " can't be told";
                    if (!sub) continue;
                    ClassData.LambdaSite site = world.lambdas.get(key);
                    if (site.samName().equals(n.name()) && site.erased().equals(n.descriptor())) continue;
                    JavaWorld.Selected s = world.select(world.lambdaInterface(key), n.name(), n.descriptor());
                    if (s.why() != null) return "a virtual call isn't certain: on a lambda, " + n.type().replace('/', '.') + "." + n.name() + " " + s.why();
                    String r = untranslated("it calls " + n.name() + " on a lambda, which may run ", s.id(), needs, why);
                    if (r != null) return r;
                }
                return null;
            }
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
        if (name.equals("clone")) return new JavaWorld.Selected(null, "(an array's clone isn't translated yet)");
        if (world.declared(JavaWorld.OBJECT, name, descriptor) == null) return new JavaWorld.Selected(null, "(Object has no " + name + ")");
        return new JavaWorld.Selected(new Id(JavaWorld.OBJECT, name, descriptor), null);
    }

    private static String untranslated(String said, Id id, Map<Id, List<Need>> needs, Map<Id, String> why) {
        if (needs.containsKey(id) || Library.nativeOf(id) != null) return null;
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
            Set<String> doneLambdas = new TreeSet<>(), doneDispatchers = new TreeSet<>();
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
            for (Library.Native n : Library.NATIVES.values()) if (natives.contains(n.name())) nativeText.append('\n').append(n.rust());
            if (natives.contains("jnative_float_text") || natives.contains("jnative_double_text")) nativeText.append(Library.NUMBER_TEXT);

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

        /**
         * The part of the runtime that the model's classes shape: the ids of the exceptions the runtime throws, how
         * their message and cause are kept, how a String keeps its units, each class's name.
         */
        String worldRuntime() {
            StringBuilder b = new StringBuilder("\n// ---- the runtime's exceptions and texts, from the library model ----\n");
            for (Map.Entry<String, String> e : Library.THROWN.entrySet()) {
                b.append("#[allow(dead_code)]\nconst ").append(e.getKey()).append(": u32 = ").append(world.concrete(e.getValue()) ? world.id(e.getValue()) : 0).append(";\n");
            }
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
                String shown = t.startsWith(JavaWorld.LAMBDA) ? t.substring(JavaWorld.LAMBDA.length(), t.indexOf('#')).replace('/', '.') + "$$Lambda" : t.replace('/', '.');
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
                        let layout = vec![b'A'; JMESSAGE.max(JCAUSE) + 1];
                        let e = jnew(class, &layout);
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
                values.add(constant instanceof Number n && k != 'A' ? MethodRust.wrap(k, switch (k) {
                    case 'J' -> MethodRust.literal(n.longValue());
                    case 'F' -> MethodRust.literal(n.floatValue());
                    case 'D' -> MethodRust.literal(n.doubleValue());
                    default -> MethodRust.literal(n.intValue());
                }) : "jzero(b'" + k + "')");
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
                        : function(world.select(world.lambdaInterface(l), method, descriptor).id());
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
            for (int k = 0; k < given.size(); k++) {
                String g = given.get(k);
                // A reference handed on as a narrower type is cast, as LambdaMetafactory's adapter does.
                boolean cast = !MethodRust.primitive(types.get(k)) && !types.get(k).equals(taken.get(k)) && !taken.get(k).equals("Ljava/lang/Object;");
                args.add(cast ? "jcheckcast(" + g + ", " + typeTest(JavaWorld.type(taken.get(k))) + ")?" : g);
            }
            String call;
            Id impl = new Id(site.owner(), site.name(), site.descriptor());
            switch (site.kind()) {
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
            if (r.equals("V")) b.append("    ").append(call).append(";\n    Ok(())\n");
            else b.append("    Ok(").append(call).append(")\n");
            b.append("}\n");
            return b.toString();
        }

        /** A value of a descriptor's type at a function's edge, as the int a slot keeps. */
        private static String fromEdge(String d, String e) {
            return "ZBCS".indexOf(d.charAt(0)) >= 0 ? "(" + e + " as i32)" : e;
        }

        private static String toEdge(String d, String e) {
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
        b.append(". Objects, arrays, fields, inheritance and lambdas are translated; exceptions, texts, Java's library beyond Math and the game come next.\n\n");
        b.append("| Why the others aren't | Methods |\n|---|---|\n");
        r.reasons().forEach((w, n) -> b.append("| ").append(w.replace("|", "\\|")).append(" | ").append(n).append(" |\n"));
        b.append("\n## Translated\n\n");
        r.translated().forEach((id, name) -> b.append("- `").append(id.display()).append("` as `").append(name).append("`\n"));
        b.append("\n## Not translated\n\n");
        r.why().forEach((id, w) -> b.append("- `").append(id.display()).append("`: ").append(w).append('\n'));
        return b.toString();
    }
}
