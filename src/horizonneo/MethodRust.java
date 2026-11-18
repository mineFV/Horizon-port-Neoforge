package horizonneo;

import horizonneo.Bytecode.Insn;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static horizonneo.Bytecode.*;

/**
 * One method's bytecode as a Rust function, with Java's own semantics: int and long arithmetic wraps, shifts take
 * their count modulo the width, a float cast to an int saturates and NaN becomes 0, comparisons of floats treat NaN
 * as the instruction says, Math's methods keep their exact results (a NaN or a negative zero included).
 *
 * Objects follow the uniform model (`JavaWorld`): a reference is a `Ref` (`Option<Arc<Obj>>`), a field is a slot,
 * a virtual or interface call goes through a dispatcher that matches on the object's class, a lambda is an object
 * of a class of its own, an array is an object whose slots are its elements. Where Java throws (null, an index out
 * of bounds, a failed cast, an int divided by zero), the function returns an `Err` holding the exception, as it
 * does for one the method throws; a guarded block hands it to its handler. Java's library is the model's (lib/),
 * translated with the mod; a method that uses the game isn't translated yet.
 *
 * The control flow is kept as the bytecode has it: each basic block is an arm of a `match` in a `loop`, which
 * is exact for any flow (a method of one block is written straight). Values left on the operand stack between
 * blocks go through variables of their own (`s0_i`...), each local slot is a variable of its type (`l3`, or
 * `l3_j` where a slot holds several types).
 */
final class MethodRust {
    private MethodRust() {}

    /** A method a call names. */
    record Call(String owner, String name, String descriptor) {}

    /**
     * What a method needs of the rest of the translation. kind: "call" (id is called directly), "dispatch" (a
     * virtual or interface call: type, name, descriptor), "new" (an object of type is made: a class or an array
     * descriptor), "init" (class type is initialized first), "lambda" (the site type, running id), "test" (a cast
     * or instanceof against type).
     */
    record Need(String kind, JarRust.Id id, String type, String name, String descriptor) {}

    /** What the check found: why the method can't be translated (null when it can), and what it needs. */
    record Check(String why, List<Need> needs) {}

    /** The translation: the function's text, and the helpers of this tool's own it uses (`jcmpl_f32`...). */
    record Done(String rust, Set<String> helpers) {}

    /** The names of the rest of the translation, given while writing (each asked for is written by `JarRust`). */
    interface Names {
        String function(JarRust.Id id);

        String dispatcher(String owner, String name, String descriptor);

        String typeTest(String type);

        String init(String cls);

        String statics(String cls);

        int id(String type);

        String lambdaNew(String key);

        /** A text constant: the function that gives its interned String. */
        String literal(String text);

        /** A record's equals, hashCode or toString made by ObjectMethods, by its site's key. */
        String objectMethods(String key);
    }

    // ---- what can be translated ----

    static Check check(String owner, ClassData.Method m, ClassData.Pool pool, JavaWorld world) {
        return check(owner, m, pool, world, null);
    }

    /**
     * As check, and with all: every reason the method can't be translated, each instruction's, added to it (the check
     * then goes on past the first, for Blockers); the needs then hold what the instructions that can be translated need.
     */
    static Check check(String owner, ClassData.Method m, ClassData.Pool pool, JavaWorld world, List<String> all) {
        List<Need> needs = new ArrayList<>();
        if (m.body() == null) return new Check("it has no body", needs);
        for (ClassData.Handler h : m.body().handlers()) {
            if (h.type() == null) continue;
            if (where(h.type(), world) != null) return new Check("it catches an exception of " + where(h.type(), world) + ": " + h.type().replace('/', '.'), needs);
            needs.add(new Need("test", null, h.type(), null, null));
        }
        if (initsOnEntry(owner, m, world)) needs.add(new Need("init", null, owner, null, null));
        if ((m.access() & SYNCHRONIZED) != 0 && m.isStatic()) {
            // A static synchronized method holds its class object's monitor.
            if (!world.concrete(CLASS)) return new Check("it is synchronized on its class: the library model has no Class", needs);
            needs.add(new Need("new", null, CLASS, null, null));
            needs.add(new Need("class", null, owner, null, null));
        }
        List<Insn> code;
        try {
            code = Bytecode.decode(m.body().code());
        } catch (Bytecode.Invalid e) {
            return new Check("its bytecode doesn't decode: " + e.getMessage(), needs);
        }
        // The model's split(String) hands its own (checked) separator on to split(String, int).
        String split = owner.equals(STRING) ? null : splitWhy(code, m.body().handlers(), pool);
        if (split != null) return new Check(split, needs);
        String first = null;
        for (Insn i : code) {
            String why = need(owner, i, pool, world, needs);
            if (why == null) continue;
            if (all == null) return new Check(why, needs);
            all.add(why);
            if (first == null) first = why;
        }
        return new Check(first, needs);
    }

    /**
     * String.split is the model's only for the JDK's fast path: a separator that is one character no regular
     * expression gives a meaning to (or a backslash and one that isn't a letter nor a digit). So each call must take
     * a constant like that, loaded just before it (with the limit, if any, pushed between), with no jump onto it.
     */
    private static String splitWhy(List<Insn> code, List<ClassData.Handler> handlers, ClassData.Pool pool) {
        Set<Integer> targets = new java.util.HashSet<>();
        for (Insn i : code) for (int t : i.targets()) targets.add(t);
        for (ClassData.Handler h : handlers) targets.add(h.handler());
        for (int k = 0; k < code.size(); k++) {
            Insn i = code.get(k);
            if (i.op() != INVOKEVIRTUAL) continue;
            String[] t = pool.member(i.index());
            if (!t[0].equals(STRING) || !t[1].equals("split")) continue;
            boolean limited = t[2].equals("(Ljava/lang/String;I)[Ljava/lang/String;");
            int at = k - (limited ? 2 : 1);
            boolean ok = at >= 0 && !targets.contains(i.pc());
            if (ok && limited) {
                Insn limit = code.get(k - 1);
                int op = limit.op();
                ok = !targets.contains(limit.pc()) && ((op >= ICONST_M1 && op <= ICONST_5) || op == BIPUSH || op == SIPUSH || (op >= ILOAD && op <= ILOAD + 0) || op == 0x1a
                        || op == 0x1b || op == 0x1c || op == 0x1d);
            }
            if (ok) {
                Insn load = code.get(at);
                Object c = (load.op() == LDC || load.op() == LDC_W) ? pool.constant(load.index()) : null;
                ok = c instanceof String sep && fastSplit(sep);
            }
            if (!ok) return "it splits a text by a regular expression (only a constant separator of one plain character is translated)";
        }
        return null;
    }

    /** Whether String.split takes the JDK's fast path for this separator (no regular expression). */
    static boolean fastSplit(String regex) {
        char ch = 0;
        boolean single = regex.length() == 1 && ".$|()[{^?*+\\".indexOf(ch = regex.charAt(0)) == -1;
        boolean escaped = regex.length() == 2 && regex.charAt(0) == '\\' && (((ch = regex.charAt(1)) - '0') | ('9' - ch)) < 0 && ((ch - 'a') | ('z' - ch)) < 0
                && ((ch - 'A') | ('Z' - ch)) < 0;
        return (single || escaped) && (ch < Character.MIN_HIGH_SURROGATE || ch > Character.MAX_LOW_SURROGATE);
    }

    /** What one instruction needs, added to needs; why it can't be translated, else null. */
    private static String need(String owner, Insn i, ClassData.Pool pool, JavaWorld world, List<Need> needs) {
        int op = i.op();
        switch (op) {
            case LDC, LDC_W, LDC2_W -> {
                Object c = pool.constant(i.index());
                if (c instanceof Number) return null;
                if (c instanceof String) return text(world, needs);
                if (c instanceof ClassData.TypeName t && world.concrete(CLASS)) {
                    // A class constant: the runtime's one object for that type.
                    needs.add(new Need("new", null, CLASS, null, null));
                    needs.add(new Need("class", null, t.descriptor(), null, null));
                    return null;
                }
                return "it uses a constant not translated yet (a method handle or a method type)";
            }
            case GETSTATIC, PUTSTATIC -> {
                String[] t = pool.member(i.index());
                if (where(t[0], world) != null) return "it reads or writes a static field of " + where(t[0], world) + ": " + t[0].replace('/', '.') + "." + t[1];
                JavaWorld.FieldAt f = world.field(t[0], t[1], t[2], true);
                if (f.why() != null) return "it uses " + f.why();
                if (needsInit(owner, f.declaring(), world)) needs.add(new Need("init", null, f.declaring(), null, null));
                return null;
            }
            case GETFIELD, PUTFIELD -> {
                String[] t = pool.member(i.index());
                if (where(t[0], world) != null) return "it reads or writes a field of " + where(t[0], world) + ": " + t[0].replace('/', '.') + "." + t[1];
                JavaWorld.FieldAt f = world.field(t[0], t[1], t[2], false);
                return f.why() == null ? null : "it uses " + f.why();
            }
            case INVOKESTATIC -> {
                String[] t = pool.member(i.index());
                Call call = new Call(t[0], t[1], t[2]);
                // Class.getEnumConstants: every enum whose class object there may be has its values() translated.
                if (t[0].equals(CLASS) && t[1].equals("jEnumValues")) needs.add(new Need("enums", null, null, null, null));
                if (library(call) != null) return null;
                if (where(t[0], world) != null) return "it calls " + where(t[0], world) + ": " + display(call);
                String declaring = null;
                for (String c = t[0]; c != null && world.cls(c) != null; c = world.cls(c).superName) {
                    if (world.declared(c, t[1], t[2]) != null) {
                        declaring = c;
                        break;
                    }
                }
                if (declaring == null) {
                    ClassData.Method m = world.declared(t[0], t[1], t[2]);
                    if (m == null) {
                        return world.jar.contains(t[0]) ? "it calls " + display(call) + ", which isn't found in the jar"
                                : "it calls a method the library model doesn't have yet: " + display(call);
                    }
                    declaring = t[0];
                }
                needs.add(new Need("call", new JarRust.Id(declaring, t[1], t[2]), null, null, null));
                if (needsInit(owner, declaring, world)) needs.add(new Need("init", null, declaring, null, null));
                return null;
            }
            case INVOKESPECIAL -> {
                String[] t = pool.member(i.index());
                if (t[0].equals(JavaWorld.OBJECT) && t[1].equals("<init>")) return null;
                if (where(t[0], world) != null) return "it calls " + where(t[0], world) + ": " + display(new Call(t[0], t[1], t[2]));
                JavaWorld.Selected s = world.special(owner, t[0], t[1], t[2]);
                if (s.why() != null) return "it calls " + display(new Call(t[0], t[1], t[2])) + ": " + s.why();
                needs.add(new Need("call", s.id(), null, null, null));
                return null;
            }
            case INVOKEVIRTUAL, INVOKEINTERFACE -> {
                String[] t = pool.member(i.index());
                if (where(t[0], world) != null) return "it calls " + where(t[0], world) + ": " + display(new Call(t[0], t[1], t[2]));
                ClassData.Method declared = world.declared(t[0], t[1], t[2]);
                if (declared != null && (declared.access() & 0x0002) != 0) {
                    needs.add(new Need("call", new JarRust.Id(t[0], t[1], t[2]), null, null, null));
                } else {
                    needs.add(new Need("dispatch", null, t[0], t[1], t[2]));
                }
                return null;
            }
            case INVOKEDYNAMIC -> {
                ClassData.LambdaSite site = pool.lambdaSite(i.index());
                if (site == null) {
                    ClassData.Bootstrap b = pool.bootstrap(i.index());
                    if (b.owner().equals("java/lang/invoke/StringConcatFactory")) return concatNeeds(i, pool, world, needs);
                    if (b.owner().equals("java/lang/runtime/ObjectMethods")) return objectMethodsNeeds(owner, i, pool, world, needs);
                    if (b.owner().equals(SWITCH_BOOTSTRAPS) && b.name().equals("typeSwitch")) return typeSwitchNeeds(b, needs);
                    String[] d = pool.dynamic(i.index());
                    return "it uses an invokedynamic that isn't a plain lambda nor a text joined: " + b.owner().replace('/', '.') + "." + b.name() + " (" + d[0] + ")";
                }
                String why = lambdaWhy(site, world, needs);
                if (why != null) return why;
                String key = JavaWorld.lambdaKey(owner, i.index());
                world.lambdas.put(key, site);
                JarRust.Id impl = new JarRust.Id(site.owner(), site.name(), site.descriptor());
                needs.add(new Need("lambda", impl, key, null, null));
                // A reference handed on as a narrower type is cast in the lambda's class.
                List<String> given = new ArrayList<>(ClassData.parameters(site.site()));
                given.addAll(ClassData.parameters(site.erased()));
                List<String> taken = new ArrayList<>();
                if (site.kind() == 5 || site.kind() == 7 || site.kind() == 9) taken.add("L" + site.owner() + ";");
                taken.addAll(ClassData.parameters(site.descriptor()));
                for (int k = 0; k < given.size(); k++) {
                    if (!primitive(given.get(k)) && !given.get(k).equals(taken.get(k)) && !taken.get(k).equals("Ljava/lang/Object;")) {
                        needs.add(new Need("test", null, JavaWorld.type(taken.get(k)), null, null));
                    }
                }
                ClassData.Method target = world.declared(site.owner(), site.name(), site.descriptor());
                boolean direct = target != null && (target.access() & 0x0002) != 0;
                switch (direct && (site.kind() == 5 || site.kind() == 9) ? 7 : site.kind()) {
                    case 5, 9 -> needs.add(new Need("dispatch", null, site.owner(), site.name(), site.descriptor()));
                    case 8 -> {
                        // A constructor reference makes objects as `new` does, and is refused where `new` is.
                        if (!world.concrete(site.owner())) return "it makes an object whose superclass is outside the jar: " + site.owner().replace('/', '.');
                        String hidden = world.unknownSupertype(site.owner());
                        if (hidden != null) return unknownWhy(site.owner(), hidden);
                        needs.add(new Need("new", null, site.owner(), null, null));
                        needs.add(new Need("init", null, site.owner(), null, null));
                        needs.add(new Need("call", impl, null, null, null));
                    }
                    default -> {
                        needs.add(new Need("call", impl, null, null, null));
                        if (site.kind() == 6 && needsInit(owner, site.owner(), world)) needs.add(new Need("init", null, site.owner(), null, null));
                    }
                }
                return null;
            }
            case NEW -> {
                String c = pool.className(i.index());
                if (where(c, world) != null && outside(c) != null) return "it makes an object of " + outside(c) + ": " + c.replace('/', '.');
                if (world.cls(c) == null) return "it makes an object of another library or mod: " + c.replace('/', '.');
                if (!world.concrete(c)) return "it makes an object whose superclass is outside the jar: " + c.replace('/', '.');
                String hidden = world.unknownSupertype(c);
                if (hidden != null) return unknownWhy(c, hidden);
                needs.add(new Need("new", null, c, null, null));
                if (!c.equals(owner)) needs.add(new Need("init", null, c, null, null));
                return null;
            }
            case NEWARRAY -> {
                needs.add(new Need("new", null, JavaWorld.PRIMITIVE_ARRAYS.get(arrayKind(i.value())), null, null));
                return null;
            }
            case ANEWARRAY -> {
                String c = pool.className(i.index());
                needs.add(new Need("new", null, "[" + (c.startsWith("[") ? c : "L" + c + ";"), null, null));
                return null;
            }
            case AASTORE -> {
                needs.add(new Need("test", null, "aastore", null, null));
                return null;
            }
            case CHECKCAST, INSTANCEOF -> {
                needs.add(new Need("test", null, pool.className(i.index()), null, null));
                return null;
            }
            case MULTIANEWARRAY -> {
                return "it makes an array of several dimensions at once: not translated yet";
            }
            case ATHROW -> {
                return world.cls("java/lang/Throwable") == null ? "it throws an exception: the library model has no Throwable" : null;
            }
            case MONITORENTER, MONITOREXIT -> {
                return null;
            }
            case JSR, JSR_W, RET -> {
                return "it uses a subroutine (class files before Java 7): not translated";
            }
            default -> {
                return null;
            }
        }
    }

    /** What a text needs: the model's String, made by the runtime, and the char arrays it holds. */
    private static String text(JavaWorld world, List<Need> needs) {
        if (world.cls("java/lang/String") == null) return "it uses a text: the library model has no String";
        needs.add(new Need("new", null, "java/lang/String", null, null));
        needs.add(new Need("new", null, "[C", null, null));
        return null;
    }

    static final String STRING = "java/lang/String";

    static final String CLASS = "java/lang/Class";

    /** The model's String.valueOf (or jtext, for a String) that turns a value of this descriptor into text. */
    static JarRust.Id textOf(String d) {
        String param = switch (d) {
            case "Z" -> "Z";
            case "C" -> "C";
            case "J" -> "J";
            case "F" -> "F";
            case "D" -> "D";
            case "I", "B", "S" -> "I";
            case "Ljava/lang/String;" -> null;
            default -> "Ljava/lang/Object;";
        };
        return param == null ? new JarRust.Id(STRING, "jtext", "(Ljava/lang/String;)Ljava/lang/String;")
                : new JarRust.Id(STRING, "valueOf", "(" + param + ")Ljava/lang/String;");
    }

    static final JarRust.Id CONCAT = new JarRust.Id(STRING, "jconcat", "([Ljava/lang/String;)Ljava/lang/String;");

    static final String SWITCH_BOOTSTRAPS = "java/lang/runtime/SwitchBootstraps";

    static final int SYNCHRONIZED = 0x0020;

    /**
     * A pattern switch's typeSwitch: of a null, -1; else the first label from the restart index on that the object is
     * an instance of; else the number of labels. Its labels are type tests; a constant label (a text, a number, an enum
     * constant) isn't translated yet.
     */
    private static String typeSwitchNeeds(ClassData.Bootstrap b, List<Need> needs) {
        for (Object a : b.arguments()) {
            if (!(a instanceof ClassData.TypeName t)) return "it switches on a pattern with a constant label (a text, a number, an enum constant): not translated yet";
            needs.add(new Need("test", null, t.descriptor(), null, null));
        }
        return null;
    }

    /** A text joined by StringConcatFactory: each value turned into text by the model, then joined. */
    private static String concatNeeds(Insn i, ClassData.Pool pool, JavaWorld world, List<Need> needs) {
        String why = text(world, needs);
        if (why != null) return why;
        ClassData.Bootstrap b = pool.bootstrap(i.index());
        if (!b.name().equals("makeConcatWithConstants") && !b.name().equals("makeConcat")) return "it joins texts in a way not translated yet: " + b.name();
        for (Object a : b.arguments()) if (a == null || a instanceof ClassData.TypeName) return "it joins texts with a constant not translated yet";
        needs.add(new Need("new", null, "[Ljava/lang/String;", null, null));
        needs.add(new Need("call", CONCAT, null, null, null));
        for (String d : ClassData.parameters(pool.dynamic(i.index())[1])) needs.add(new Need("call", textOf(d), null, null, null));
        return null;
    }

    /**
     * A record's equals, hashCode or toString, made by ObjectMethods.bootstrap from its components' getters: each
     * component compared, hashed or written as the JDK does, through the model's Objects, boxes and String.
     */
    record ObjectMethodsSite(String record, String method, List<String> names, List<JavaWorld.FieldAt> fields, String descriptor) {}

    static final String OBJECT_METHODS = "objectmethods:";

    private static String objectMethodsNeeds(String owner, Insn i, ClassData.Pool pool, JavaWorld world, List<Need> needs) {
        ClassData.Bootstrap b = pool.bootstrap(i.index());
        String[] d = pool.dynamic(i.index());
        List<Object> a = b.arguments();
        if (!b.name().equals("bootstrap") || a.size() < 2 || !(a.get(0) instanceof ClassData.TypeName record) || !(a.get(1) instanceof String names)) {
            return "it uses an invokedynamic that isn't a plain lambda nor a text joined: java.lang.runtime.ObjectMethods." + b.name();
        }
        List<JavaWorld.FieldAt> fields = new ArrayList<>();
        for (int k = 2; k < a.size(); k++) {
            if (!(a.get(k) instanceof ClassData.Handle h) || h.kind() != 1) return "a record's " + d[0] + " reads a component in a way not translated yet";
            JavaWorld.FieldAt f = world.field(h.owner(), h.name(), h.descriptor(), false);
            if (f.why() != null) return "it uses " + f.why();
            fields.add(f);
        }
        List<String> componentNames = names.isEmpty() ? List.of() : List.of(names.split(";"));
        if (componentNames.size() != fields.size()) return "a record's " + d[0] + " names its components in a way not translated yet";
        String key = OBJECT_METHODS + owner + "#" + i.index();
        world.objectMethods.put(key, new ObjectMethodsSite(record.descriptor(), d[0], componentNames, fields, d[1]));
        switch (d[0]) {
            case "toString" -> {
                String why = text(world, needs);
                if (why != null) return why;
                needs.add(new Need("new", null, "[Ljava/lang/String;", null, null));
                needs.add(new Need("call", CONCAT, null, null, null));
                for (JavaWorld.FieldAt f : fields) needs.add(new Need("call", textOf(f.descriptor()), null, null, null));
            }
            case "hashCode" -> {
                for (JavaWorld.FieldAt f : fields) needs.add(new Need("call", hashOf(f.descriptor()), null, null, null));
            }
            case "equals" -> {
                needs.add(new Need("test", null, record.descriptor(), null, null));
                for (JavaWorld.FieldAt f : fields) {
                    JarRust.Id eq = equalsOf(f.descriptor());
                    if (eq != null) needs.add(new Need("call", eq, null, null, null));
                }
            }
            default -> {
                return "it uses ObjectMethods for " + d[0] + ", which isn't translated";
            }
        }
        return null;
    }

    /** The box of a primitive descriptor. */
    static String box(String d) {
        return switch (d) {
            case "Z" -> "java/lang/Boolean";
            case "B" -> "java/lang/Byte";
            case "C" -> "java/lang/Character";
            case "S" -> "java/lang/Short";
            case "I" -> "java/lang/Integer";
            case "J" -> "java/lang/Long";
            case "F" -> "java/lang/Float";
            case "D" -> "java/lang/Double";
            default -> null;
        };
    }

    /** How ObjectMethods hashes a component: its box's static hashCode, or Objects.hashCode. */
    static JarRust.Id hashOf(String d) {
        return primitive(d) ? new JarRust.Id(box(d), "hashCode", "(" + d + ")I") : new JarRust.Id("java/util/Objects", "hashCode", "(Ljava/lang/Object;)I");
    }

    /** How ObjectMethods compares a component: == for integers, the box's compare for floats, Objects.equals for references. */
    static JarRust.Id equalsOf(String d) {
        if (d.equals("F") || d.equals("D")) return new JarRust.Id(box(d), "compare", "(" + d + d + ")I");
        return primitive(d) ? null : new JarRust.Id("java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
    }

    /** Why an object of class c isn't made: its supertype hidden isn't known. */
    private static String unknownWhy(String c, String hidden) {
        return "it makes an object whose supertypes aren't all known: " + c.replace('/', '.') + " is a " + hidden.replace('/', '.')
                + ", which isn't read (another mod's, or the game's without --server)";
    }

    /** Why a lambda site can't be translated: its values would need boxing, or another adaptation. */
    private static String lambdaWhy(ClassData.LambdaSite site, JavaWorld world, List<Need> needs) {
        if (site.owner().equals(STRING) && site.name().equals("split")) return "it makes a lambda of String.split, whose separator can't be checked";
        String iface = JavaWorld.type(ClassData.returns(site.site()));
        String hidden = world.unknownSupertype(iface);
        if (hidden != null) return unknownWhy("a lambda of " + iface, hidden);
        if (where(site.owner(), world) != null) return "it makes a lambda that runs " + where(site.owner(), world) + ": " + site.owner().replace('/', '.') + "." + site.name();
        List<String> captured = ClassData.parameters(site.site());
        List<String> sam = ClassData.parameters(site.erased());
        List<String> given = new ArrayList<>(captured);
        given.addAll(sam);
        List<String> taken = new ArrayList<>();
        if (site.kind() == 5 || site.kind() == 7 || site.kind() == 9) taken.add("L" + site.owner() + ";");
        else if (site.kind() != 6 && site.kind() != 8) return "it makes a method reference of a kind not translated yet";
        taken.addAll(ClassData.parameters(site.descriptor()));
        if (given.size() != taken.size()) return "it makes a lambda whose values don't line up with the method it runs";
        for (int k = 0; k < given.size(); k++) {
            String why = adaptNeeds(given.get(k), taken.get(k), world, needs);
            if (why != null) return "it makes a lambda whose values " + why;
        }
        String r = ClassData.returns(site.erased()), ir = site.kind() == 8 ? "L" + site.owner() + ";" : ClassData.returns(site.descriptor());
        if (r.equals("V")) return null;
        if (ir.equals("V")) return "it makes a lambda that gives nothing where its interface wants a value";
        String why = adaptNeeds(ir, r, world, needs);
        return why == null ? null : "it makes a lambda whose result " + why;
    }

    /** Primitive widening (JLS 5.1.2), as LambdaMetafactory allows it. */
    static boolean widens(String from, String to) {
        if (from.equals(to)) return true;
        return switch (from) {
            case "B" -> "SIJFD".contains(to);
            case "S", "C" -> "IJFD".contains(to);
            case "I" -> "JFD".contains(to);
            case "J" -> "FD".contains(to);
            case "F" -> to.equals("D");
            default -> false;
        };
    }

    /** The primitive a box holds, null for another class. */
    static String unboxed(String descriptor) {
        for (String p : List.of("Z", "B", "C", "S", "I", "J", "F", "D")) if (descriptor.equals("L" + box(p) + ";")) return p;
        return null;
    }

    static final Map<String, String> UNBOX = Map.of("Z", "booleanValue", "B", "byteValue", "C", "charValue", "S", "shortValue", "I", "intValue",
            "J", "longValue", "F", "floatValue", "D", "doubleValue");

    /**
     * What turning a value of descriptor from into one of descriptor to needs, as LambdaMetafactory adapts them:
     * widening, boxing into a supertype of the box, unboxing (from a box, or cast into the box first), casting.
     * Why it can't, else null.
     */
    static String adaptNeeds(String from, String to, JavaWorld world, List<Need> needs) {
        boolean pf = primitive(from), pt = primitive(to);
        if (pf && pt) return widens(from, to) ? null : "would need " + from + " made " + to + ", which LambdaMetafactory doesn't do";
        if (!pf && !pt) {
            if (!from.equals(to) && !to.equals("Ljava/lang/Object;")) needs.add(new Need("test", null, JavaWorld.type(to), null, null));
            return null;
        }
        if (pf) {
            String b = box(from);
            if (world.cls(b) == null) return "need boxing into " + b + ", which the library model lacks";
            Boolean fits = world.subtype(b, JavaWorld.type(to));
            if (!Boolean.TRUE.equals(fits)) return "would need " + from + " boxed into " + to + ", which LambdaMetafactory doesn't do";
            needs.add(new Need("call", new JarRust.Id(b, "valueOf", "(" + from + ")L" + b + ";"), null, null, null));
            needs.add(new Need("new", null, b, null, null));
            return null;
        }
        String held = unboxed(from);
        String through = held != null ? held : to;
        if (held != null && !widens(held, to)) return "would need " + from + " made " + to + ", which LambdaMetafactory doesn't do";
        String b = box(through);
        if (world.cls(b) == null) return "need unboxing from " + b + ", which the library model lacks";
        if (held == null) needs.add(new Need("test", null, b, null, null));
        needs.add(new Need("dispatch", null, b, UNBOX.get(through), "()" + through));
        return null;
    }

    /**
     * Whether a static method starts by initializing its class, as invokestatic does (JVMS 5.5) from wherever it is
     * called: from the game, or from code that doesn't know the class is initialized yet. Initializing an initialized
     * class, or one this thread is initializing, does nothing.
     */
    static boolean initsOnEntry(String owner, ClassData.Method m, JavaWorld world) {
        if ((m.access() & 0x0008) == 0 || m.name().equals("<clinit>")) return false;
        for (String c : world.initOrder(owner)) if (world.declared(c, "<clinit>", "()V") != null) return true;
        return false;
    }

    /** Whether a use of class c's statics, from code of class current, has to initialize c first (JVMS 5.5). */
    static boolean needsInit(String current, String c, JavaWorld world) {
        for (String s = current; s != null && world.cls(s) != null; s = world.cls(s).superName) if (s.equals(c)) return false;
        return true;
    }

    /** "the game" or "Java's library" for a class outside the jar of those, else null. */
    static String outside(String c) {
        if (c.startsWith("[")) return null;
        if (c.startsWith("net/minecraft/") || c.startsWith("net/neoforged/") || c.startsWith("com/mojang/") || c.startsWith("cpw/")) return "the game";
        if (c.startsWith("java/") || c.startsWith("javax/") || c.startsWith("jdk/") || c.startsWith("sun/")) return "Java's library";
        return null;
    }

    /** Where a class outside the jar is from: the game, Java's library, or another library or mod; null for the jar's own. */
    static String where(String c, JavaWorld world) {
        if (c.startsWith("[") || world.cls(c) != null) return null;
        return outside(c) != null ? outside(c) : "another library or mod";
    }

    static boolean primitive(String d) {
        return d.length() == 1 && "IJFDZBCS".contains(d);
    }

    static String display(Call c) {
        return c.owner().replace('/', '.') + "." + c.name() + c.descriptor();
    }

    /** newarray's type code as the index of its array in JavaWorld.PRIMITIVE_ARRAYS. */
    static int arrayKind(int atype) {
        return switch (atype) {
            case 4 -> 0;
            case 8 -> 1;
            case 5 -> 2;
            case 9 -> 3;
            case 10 -> 4;
            case 11 -> 5;
            case 6 -> 6;
            case 7 -> 7;
            default -> throw new Bytecode.Invalid("newarray of the unknown type " + atype);
        };
    }

    // ---- the flow as Rust's own loops and blocks ----

    /**
     * A method's blocks as labelled loops and blocks, as Ramsey's "Beyond Relooper" builds them from the dominator
     * tree: a block that a back edge reaches is a loop (`'lN: loop`) its back edges continue; one that two forward
     * edges reach follows a labelled block (`'bN: { ... }`) they break out of; any other is written where its one
     * predecessor branches to it. A block in a try range ends by its closure's result, which says where it went.
     * write gives null when the flow isn't reducible (a loop entered at two places), for the blocks in a match.
     */
    static final class Structure {
        private final Map<Integer, List<String>> bodies;
        private final Map<Integer, List<Integer>> succs;
        private final Set<Integer> guarded;
        private final Map<Integer, Integer> rpo = new HashMap<>();
        private final Map<Integer, Integer> idom = new HashMap<>();
        private final Map<Integer, List<Integer>> children = new HashMap<>();
        private final Set<Integer> loops = new java.util.HashSet<>();
        private final Set<Integer> merges = new java.util.HashSet<>();

        private static final java.util.regex.Pattern GOTO = java.util.regex.Pattern.compile("block = (\\d+);");
        private static final java.util.regex.Pattern IF = java.util.regex.Pattern.compile("block = if (\\S+) \\{ (\\d+) \\} else \\{ (\\d+) \\};");

        Structure(Map<Integer, List<String>> bodies, Map<Integer, List<Integer>> succs, Set<Integer> guarded) {
            this.bodies = bodies;
            this.succs = succs;
            this.guarded = guarded;
        }

        String write() {
            // Reverse postorder from the first block.
            List<Integer> post = new ArrayList<>();
            Set<Integer> seen = new java.util.HashSet<>();
            Deque<int[]> stack = new ArrayDeque<>();
            stack.push(new int[] {0, 0});
            seen.add(0);
            while (!stack.isEmpty()) {
                int[] top = stack.peek();
                List<Integer> out = succs.getOrDefault(top[0], List.of());
                if (top[1] < out.size()) {
                    int next = out.get(top[1]++);
                    if (seen.add(next)) stack.push(new int[] {next, 0});
                } else {
                    post.add(top[0]);
                    stack.pop();
                }
            }
            List<Integer> order = new ArrayList<>(post);
            java.util.Collections.reverse(order);
            for (int k = 0; k < order.size(); k++) rpo.put(order.get(k), k);
            Map<Integer, List<Integer>> preds = new HashMap<>();
            for (int b : order) for (int t : succs.getOrDefault(b, List.of())) preds.computeIfAbsent(t, x -> new ArrayList<>()).add(b);
            // Dominators (Cooper, Harvey and Kennedy).
            idom.put(0, 0);
            for (boolean changed = true; changed; ) {
                changed = false;
                for (int b : order) {
                    if (b == 0) continue;
                    Integer best = null;
                    for (int p : preds.getOrDefault(b, List.of())) {
                        if (!idom.containsKey(p)) continue;
                        best = best == null ? p : intersect(p, best);
                    }
                    if (best != null && !best.equals(idom.get(b))) {
                        idom.put(b, best);
                        changed = true;
                    }
                }
            }
            for (int b : order) if (b != 0) children.computeIfAbsent(idom.get(b), x -> new ArrayList<>()).add(b);
            for (int b : order) {
                int forward = 0;
                for (int p : preds.getOrDefault(b, List.of())) {
                    if (rpo.get(p) >= rpo.get(b)) {
                        // A back edge: its target must dominate it, or the loop is entered at two places.
                        if (!dominates(b, p)) return null;
                        loops.add(b);
                    } else {
                        forward++;
                    }
                }
                if (forward >= 2) merges.add(b);
            }
            StringBuilder out = new StringBuilder();
            tree(0, 1, out);
            out.append("    unreachable!()\n");
            return out.toString();
        }

        private int intersect(int a, int b) {
            while (a != b) {
                while (rpo.get(a) > rpo.get(b)) a = idom.get(a);
                while (rpo.get(b) > rpo.get(a)) b = idom.get(b);
            }
            return a;
        }

        private boolean dominates(int a, int b) {
            for (int x = b; ; x = idom.get(x)) {
                if (x == a) return true;
                if (x == 0) return false;
            }
        }

        private static String ind(int depth) {
            return "    ".repeat(depth);
        }

        private void tree(int x, int depth, StringBuilder out) {
            List<Integer> ys = new ArrayList<>();
            for (int c : children.getOrDefault(x, List.of())) if (merges.contains(c)) ys.add(c);
            ys.sort((a, b) -> rpo.get(b) - rpo.get(a));
            if (loops.contains(x)) {
                out.append(ind(depth)).append("'l").append(x).append(": loop {\n");
                within(x, ys, depth + 1, out);
                out.append(ind(depth)).append("}\n");
            } else {
                within(x, ys, depth, out);
            }
        }

        private void within(int x, List<Integer> ys, int depth, StringBuilder out) {
            if (!ys.isEmpty()) {
                int y = ys.get(0);
                out.append(ind(depth)).append("'b").append(y).append(": {\n");
                within(x, ys.subList(1, ys.size()), depth + 1, out);
                out.append(ind(depth)).append("}\n");
                tree(y, depth, out);
                return;
            }
            List<String> lines = bodies.get(x);
            List<Integer> to = succs.getOrDefault(x, List.of());
            String last = lines.isEmpty() ? "" : lines.get(lines.size() - 1);
            java.util.regex.Matcher go = GOTO.matcher(last), when = IF.matcher(last);
            boolean plain = !guarded.contains(x);
            int keep = plain && (go.matches() || when.matches()) ? lines.size() - 1 : lines.size();
            for (int k = 0; k < keep; k++) out.append(ind(depth)).append(lines.get(k)).append('\n');
            if (to.isEmpty()) return;
            if (plain && go.matches()) {
                branch(x, Integer.parseInt(go.group(1)), depth, out);
            } else if (plain && when.matches()) {
                out.append(ind(depth)).append("if ").append(when.group(1)).append(" {\n");
                branch(x, Integer.parseInt(when.group(2)), depth + 1, out);
                out.append(ind(depth)).append("} else {\n");
                branch(x, Integer.parseInt(when.group(3)), depth + 1, out);
                out.append(ind(depth)).append("}\n");
            } else if (to.size() == 1 && plain) {
                branch(x, to.get(0), depth, out);
            } else {
                out.append(ind(depth)).append("match block {\n");
                for (int t : to) {
                    out.append(ind(depth + 1)).append(t).append(" => {\n");
                    branch(x, t, depth + 2, out);
                    out.append(ind(depth + 1)).append("}\n");
                }
                out.append(ind(depth + 1)).append("_ => unreachable!(),\n");
                out.append(ind(depth)).append("}\n");
            }
        }

        private void branch(int from, int to, int depth, StringBuilder out) {
            if (rpo.get(to) <= rpo.get(from)) {
                out.append(ind(depth)).append("continue 'l").append(to).append(";\n");
            } else if (merges.contains(to)) {
                out.append(ind(depth)).append("break 'b").append(to).append(";\n");
            } else {
                tree(to, depth, out);
            }
        }
    }

    // ---- the library: Math's methods with exact counterparts ----

    /** The Rust for a call of Java's library, `$0`, `$1` for its arguments; null when there is none known to be exact. */
    static String library(Call c) {
        if (!c.owner().equals("java/lang/Math") && !c.owner().equals("java/lang/StrictMath")) return null;
        return switch (c.name() + c.descriptor()) {
            case "abs(I)I", "abs(J)J" -> "$0.wrapping_abs()";
            case "abs(F)F", "abs(D)D" -> "$0.abs()";
            case "max(II)I", "max(JJ)J" -> "$0.max($1)";
            case "min(II)I", "min(JJ)J" -> "$0.min($1)";
            case "max(FF)F" -> "jmax_f32($0, $1)";
            case "max(DD)D" -> "jmax_f64($0, $1)";
            case "min(FF)F" -> "jmin_f32($0, $1)";
            case "min(DD)D" -> "jmin_f64($0, $1)";
            case "floor(D)D" -> "$0.floor()";
            case "ceil(D)D" -> "$0.ceil()";
            case "sqrt(D)D" -> "$0.sqrt()";
            case "signum(F)F" -> "jsignum_f32($0)";
            case "signum(D)D" -> "jsignum_f64($0)";
            case "round(F)I" -> "jround_f32($0)";
            case "round(D)J" -> "jround_f64($0)";
            case "floorDiv(II)I" -> "jfloor_div_i32($0, $1)?";
            case "floorDiv(JJ)J" -> "jfloor_div_i64($0, $1)?";
            case "floorMod(II)I" -> "jfloor_mod_i32($0, $1)?";
            case "floorMod(JJ)J" -> "jfloor_mod_i64($0, $1)?";
            // The JDK's own: a product by its constant (the specification only says "approximately").
            case "toRadians(D)D" -> "jto_radians($0)";
            case "toDegrees(D)D" -> "jto_degrees($0)";
            case "addExact(II)I" -> "jexact_i32($0.checked_add($1))?";
            case "subtractExact(II)I" -> "jexact_i32($0.checked_sub($1))?";
            case "multiplyExact(II)I" -> "jexact_i32($0.checked_mul($1))?";
            case "incrementExact(I)I" -> "jexact_i32($0.checked_add(1))?";
            case "decrementExact(I)I" -> "jexact_i32($0.checked_sub(1))?";
            case "negateExact(I)I" -> "jexact_i32($0.checked_neg())?";
            case "toIntExact(J)I" -> "jexact_i32(i32::try_from($0).ok())?";
            case "addExact(JJ)J" -> "jexact_i64($0.checked_add($1))?";
            case "subtractExact(JJ)J" -> "jexact_i64($0.checked_sub($1))?";
            case "multiplyExact(JJ)J" -> "jexact_i64($0.checked_mul($1))?";
            case "multiplyExact(JI)J" -> "jexact_i64($0.checked_mul($1 as i64))?";
            case "incrementExact(J)J" -> "jexact_i64($0.checked_add(1))?";
            case "decrementExact(J)J" -> "jexact_i64($0.checked_sub(1))?";
            case "negateExact(J)J" -> "jexact_i64($0.checked_neg())?";
            default -> null;
        };
    }

    /** This tool's numeric helpers, each written once in the translated module when used. */
    static final Map<String, String> HELPERS = new TreeMap<>(Map.ofEntries(
            Map.entry("jcmp_i64", "fn jcmp_i64(a: i64, b: i64) -> i32 {\n    if a < b { -1 } else if a == b { 0 } else { 1 }\n}"),
            Map.entry("jcmpl_f32", "fn jcmpl_f32(a: f32, b: f32) -> i32 {\n    if a > b { 1 } else if a == b { 0 } else { -1 }\n}"),
            Map.entry("jcmpg_f32", "fn jcmpg_f32(a: f32, b: f32) -> i32 {\n    if a < b { -1 } else if a == b { 0 } else { 1 }\n}"),
            Map.entry("jcmpl_f64", "fn jcmpl_f64(a: f64, b: f64) -> i32 {\n    if a > b { 1 } else if a == b { 0 } else { -1 }\n}"),
            Map.entry("jcmpg_f64", "fn jcmpg_f64(a: f64, b: f64) -> i32 {\n    if a < b { -1 } else if a == b { 0 } else { 1 }\n}"),
            Map.entry("jmax_f32", "fn jmax_f32(a: f32, b: f32) -> f32 {\n    if a.is_nan() || b.is_nan() { f32::NAN } else if a == 0.0 && b == 0.0 { if a.is_sign_negative() && b.is_sign_negative() { a } else { 0.0 } } else if a >= b { a } else { b }\n}"),
            Map.entry("jmax_f64", "fn jmax_f64(a: f64, b: f64) -> f64 {\n    if a.is_nan() || b.is_nan() { f64::NAN } else if a == 0.0 && b == 0.0 { if a.is_sign_negative() && b.is_sign_negative() { a } else { 0.0 } } else if a >= b { a } else { b }\n}"),
            Map.entry("jmin_f32", "fn jmin_f32(a: f32, b: f32) -> f32 {\n    if a.is_nan() || b.is_nan() { f32::NAN } else if a == 0.0 && b == 0.0 { if a.is_sign_negative() || b.is_sign_negative() { -0.0 } else { 0.0 } } else if a <= b { a } else { b }\n}"),
            Map.entry("jmin_f64", "fn jmin_f64(a: f64, b: f64) -> f64 {\n    if a.is_nan() || b.is_nan() { f64::NAN } else if a == 0.0 && b == 0.0 { if a.is_sign_negative() || b.is_sign_negative() { -0.0 } else { 0.0 } } else if a <= b { a } else { b }\n}"),
            Map.entry("jsignum_f32", "fn jsignum_f32(a: f32) -> f32 {\n    if a == 0.0 || a.is_nan() { a } else { 1.0f32.copysign(a) }\n}"),
            Map.entry("jsignum_f64", "fn jsignum_f64(a: f64) -> f64 {\n    if a == 0.0 || a.is_nan() { a } else { 1.0f64.copysign(a) }\n}"),
            // Java's round: the closest integer, ties towards positive infinity; x - floor(x) is exact.
            Map.entry("jround_f32", "fn jround_f32(a: f32) -> i32 {\n    let f = a.floor();\n    (if a - f >= 0.5 { f + 1.0 } else { f }) as i32\n}"),
            Map.entry("jround_f64", "fn jround_f64(a: f64) -> i64 {\n    let f = a.floor();\n    (if a - f >= 0.5 { f + 1.0 } else { f }) as i64\n}"),
            Map.entry("jfloor_div_i32", "fn jfloor_div_i32(a: i32, b: i32) -> JR<i32> {\n    let q = jdiv_i32(a, b)?;\n    Ok(if (a.wrapping_rem(b) != 0) && ((a ^ b) < 0) { q - 1 } else { q })\n}"),
            Map.entry("jfloor_div_i64", "fn jfloor_div_i64(a: i64, b: i64) -> JR<i64> {\n    let q = jdiv_i64(a, b)?;\n    Ok(if (a.wrapping_rem(b) != 0) && ((a ^ b) < 0) { q - 1 } else { q })\n}"),
            Map.entry("jfloor_mod_i32", "fn jfloor_mod_i32(a: i32, b: i32) -> JR<i32> {\n    let m = jrem_i32(a, b)?;\n    Ok(if m != 0 && ((m ^ b) < 0) { m + b } else { m })\n}"),
            Map.entry("jto_radians", "fn jto_radians(a: f64) -> f64 {\n    a * 0.017453292519943295\n}"),
            Map.entry("jto_degrees", "fn jto_degrees(a: f64) -> f64 {\n    a * 57.29577951308232\n}"),
            Map.entry("jexact_i32", "fn jexact_i32(r: Option<i32>) -> JR<i32> {\n    r.ok_or_else(|| jexception(JID_ARITHMETIC, Some(\"integer overflow\")))\n}"),
            Map.entry("jexact_i64", "fn jexact_i64(r: Option<i64>) -> JR<i64> {\n    r.ok_or_else(|| jexception(JID_ARITHMETIC, Some(\"long overflow\")))\n}"),
            Map.entry("jfloor_mod_i64", "fn jfloor_mod_i64(a: i64, b: i64) -> JR<i64> {\n    let m = jrem_i64(a, b)?;\n    Ok(if m != 0 && ((m ^ b) < 0) { m + b } else { m })\n}")));

    // ---- writing it ----

    /** A value on the operand stack: its kind (I, J, F, D, A) and its Rust expression; simple when a name or a literal. */
    private record V(char t, String e, boolean simple) {
        boolean wide() {
            return t == 'J' || t == 'D';
        }
    }

    /** Writes the function. The method must have passed `check`, and what it needs must be translated. */
    static Done write(String owner, ClassData.Method m, ClassData.Pool pool, String function, JavaWorld world, Names names) {
        return new Writer(owner, m, pool, function, world, names).write();
    }

    private static final class Writer {
        final String owner;
        final ClassData.Method m;
        final ClassData.Pool pool;
        final String function;
        final JavaWorld world;
        final Names names;
        final List<Insn> code;
        final Map<Integer, Integer> at = new HashMap<>();
        final Set<String> helpers = new TreeSet<>();
        /** Each slot's kinds, and the name of its variable for each. */
        final Map<Integer, Map<Character, String>> locals = new TreeMap<>();
        /** The stack variables used between blocks: name to kind. */
        final Map<String, Character> stackVars = new TreeMap<>();
        int temps;

        Writer(String owner, ClassData.Method m, ClassData.Pool pool, String function, JavaWorld world, Names names) {
            this.owner = owner;
            this.m = m;
            this.pool = pool;
            this.function = function;
            this.world = world;
            this.names = names;
            this.code = Bytecode.decode(m.body().code());
            for (int i = 0; i < code.size(); i++) at.put(code.get(i).pc(), i);
        }

        Done write() {
            // Parameters: their slots and kinds; `this` first in an instance method.
            List<String> params = new ArrayList<>();
            if (!m.isStatic()) params.add("L" + owner + ";");
            params.addAll(ClassData.parameters(m.descriptor()));
            int slot = 0;
            List<Integer> paramSlots = new ArrayList<>();
            for (String p : params) {
                char t = JavaWorld.kind(p);
                type(slot, t);
                paramSlots.add(slot);
                slot += t == 'J' || t == 'D' ? 2 : 1;
            }
            for (Insn i : code) {
                int op = i.op();
                if (op >= ILOAD && op <= ALOAD) type(i.index(), "IJFDA".charAt(op - ILOAD));
                else if (op >= ISTORE && op <= ASTORE) type(i.index(), "IJFDA".charAt(op - ISTORE));
                else if (op == IINC) type(i.index(), 'I');
            }
            for (Map<Character, String> types : locals.values()) {
                if (types.size() == 1) continue;
                for (Map.Entry<Character, String> e : types.entrySet()) e.setValue(e.getValue() + "_" + Character.toLowerCase(e.getKey()));
            }

            // Basic blocks: where each starts.
            TreeSet<Integer> leaders = new TreeSet<>(List.of(0));
            for (int k = 0; k < code.size(); k++) {
                Insn i = code.get(k);
                for (int t : i.targets()) leaders.add(at.get(t));
                if (ends(i.op()) && k + 1 < code.size()) leaders.add(k + 1);
            }
            // A block is wholly inside or outside each try range: the ranges' edges and the handlers start blocks.
            List<ClassData.Handler> handlers = m.body().handlers();
            for (ClassData.Handler h : handlers) {
                for (int pc : new int[] {h.start(), h.end(), h.handler()}) {
                    if (at.containsKey(pc)) leaders.add(at.get(pc));
                }
            }
            List<Integer> starts = new ArrayList<>(leaders);
            Map<Integer, Integer> blockOf = new HashMap<>();
            for (int b = 0; b < starts.size(); b++) blockOf.put(starts.get(b), b);
            // The handlers covering each block, in the table's order (the first that matches catches).
            Map<Integer, List<ClassData.Handler>> covering = new HashMap<>();
            for (int b = 0; b < starts.size(); b++) {
                int pc = code.get(starts.get(b)).pc();
                List<ClassData.Handler> list = new ArrayList<>();
                for (ClassData.Handler h : handlers) if (h.start() <= pc && pc < h.end()) list.add(h);
                if (!list.isEmpty()) covering.put(b, list);
            }
            String result = ClassData.returns(m.descriptor()).equals("V") ? "()" : rust(ClassData.returns(m.descriptor()));

            // Each block written in turn, from the stack it starts with: the first that reaches a block decides it.
            Map<Integer, List<Character>> entry = new HashMap<>();
            entry.put(0, List.of());
            Map<Integer, List<String>> bodies = new TreeMap<>();
            // Each block's successors: where it branches, then its handlers' blocks.
            Map<Integer, List<Integer>> succs = new TreeMap<>();
            Set<Integer> guardedBlocks = new TreeSet<>();
            boolean flows = false;
            Deque<Integer> work = new ArrayDeque<>(List.of(0));
            while (!work.isEmpty()) {
                int b = work.poll();
                if (bodies.containsKey(b)) continue;
                int from = starts.get(b), to = b + 1 < starts.size() ? starts.get(b + 1) : code.size();
                Block block = new Block(entry.get(b), blockOf, covering.containsKey(b));
                List<String> lines = block.run(from, to);
                flows |= !block.next.isEmpty() || covering.containsKey(b);
                List<Integer> out = new ArrayList<>();
                for (int next : block.next) if (!out.contains(next)) out.add(next);
                succs.put(b, out);
                for (int next : block.next) {
                    List<Character> had = entry.putIfAbsent(next, block.exit);
                    if (had != null && !had.equals(block.exit)) throw new Bytecode.Invalid("the stack differs where two paths meet, at block " + next);
                    work.add(next);
                }
                if (covering.containsKey(b)) {
                    // The block runs in a closure: what it throws goes to the first handler whose type matches, with
                    // the exception alone on the stack, or out of the method.
                    List<String> guarded = new ArrayList<>();
                    guarded.add("let r: JR<JFlow<" + result + ">> = (|| -> JR<JFlow<" + result + ">> {");
                    for (String line : lines) guarded.add("    " + line);
                    guarded.add("    Ok(JFlow::Next)");
                    guarded.add("})();");
                    guarded.add("match r {");
                    guarded.add("    Ok(JFlow::Next) => {}");
                    guarded.add("    Ok(JFlow::Return(v)) => return Ok(v),");
                    guarded.add("    Err(e) => {");
                    boolean all = false;
                    String sep = "        ";
                    guardedBlocks.add(b);
                    for (ClassData.Handler h : covering.get(b)) {
                        int target = blockOf.get(at.get(h.handler()));
                        if (!succs.get(b).contains(target)) succs.get(b).add(target);
                        List<Character> had = entry.putIfAbsent(target, List.of('A'));
                        if (had != null && !had.equals(List.of('A'))) throw new Bytecode.Invalid("a handler is also reached with another stack, at block " + target);
                        work.add(target);
                        String take = "{ " + stackVar(0, 'A') + " = e; block = " + target + "; }";
                        if (h.type() == null) {
                            guarded.add(sep + (sep.endsWith("else ") ? "" : "") + take);
                            all = true;
                            break;
                        }
                        guarded.add(sep + "if jinstanceof(&e, " + names.typeTest(h.type()) + ") == 1 " + take);
                        sep = "        else ";
                    }
                    if (!all) guarded.add("        else { return Err(e); }");
                    guarded.add("    }");
                    guarded.add("}");
                    lines = guarded;
                }
                bodies.put(b, lines);
            }

            StringBuilder out = new StringBuilder();
            out.append("/// `").append(owner.replace('/', '.')).append('.').append(m.name()).append(m.descriptor()).append("`, translated from its bytecode.\n");
            out.append("#[allow(non_snake_case, unused_mut, unused_assignments, unused_variables, unused_parens, unreachable_code, unused_labels, clippy::all)]\n");
            out.append("pub fn ").append(function).append('(');
            List<String> converted = new ArrayList<>();
            Set<String> declared = new TreeSet<>();
            for (int k = 0; k < params.size(); k++) {
                int s = paramSlots.get(k);
                String p = params.get(k);
                String name = locals.get(s).get(JavaWorld.kind(p));
                declared.add(name);
                if (k > 0) out.append(", ");
                if ("ZBCS".indexOf(p.charAt(0)) >= 0) {
                    out.append("p").append(s).append(": ").append(rust(p));
                    converted.add("    let mut " + name + ": i32 = p" + s + " as i32;");
                } else {
                    out.append("mut ").append(name).append(": ").append(rust(p));
                }
            }
            String r = ClassData.returns(m.descriptor());
            out.append(") -> JR<").append(r.equals("V") ? "()" : rust(r)).append("> {\n");
            converted.forEach(c -> out.append(c).append('\n'));
            if (initsOnEntry(owner, m, world)) out.append("    ").append(names.init(owner)).append("()?;\n");
            for (Map<Character, String> types : locals.values()) {
                for (Map.Entry<Character, String> e : types.entrySet()) {
                    if (declared.add(e.getValue())) out.append("    let mut ").append(e.getValue()).append(": ").append(kindType(e.getKey())).append(" = ").append(zero(e.getKey())).append(";\n");
                }
            }
            stackVars.forEach((name, t) -> out.append("    let mut ").append(name).append(": ").append(kindType(t)).append(" = ").append(zero(t)).append(";\n"));
            // A synchronized method holds its object's (its class object's) monitor while it runs: its body in a
            // closure, the monitor let go however it ends.
            boolean synced = (m.access() & SYNCHRONIZED) != 0;
            if (synced) {
                String monitor = m.isStatic() ? "jclass_object(" + names.id(owner) + ")" : locals.get(0).get('A') + ".clone()";
                out.append("    let jmonitor: Ref = ").append(monitor).append(";\n    jmonitor_enter(&jmonitor)?;\n");
                String type = "JR<" + (r.equals("V") ? "()" : rust(r)) + ">";
                out.append("    let jresult: ").append(type).append(" = (|| -> ").append(type).append(" {\n");
            }
            String structured = flows ? new Structure(bodies, succs, guardedBlocks).write() : null;
            if (!flows) {
                for (String line : bodies.get(0)) out.append("    ").append(line).append('\n');
            } else if (structured != null) {
                out.append("    let mut block: u32 = 0;\n").append(structured);
            } else {
                out.append("    let mut block: u32 = 0;\n    loop {\n        match block {\n");
                bodies.forEach((b, lines) -> {
                    out.append("            ").append(b).append(" => {\n");
                    for (String line : lines) out.append("                ").append(line).append('\n');
                    out.append("            }\n");
                });
                out.append("            _ => unreachable!(),\n        }\n    }\n");
            }
            if (synced) out.append("    })();\n    jmonitor_exit(&jmonitor)?;\n    jresult\n");
            out.append("}\n");
            return new Done(out.toString(), helpers);
        }

        private void type(int slot, char t) {
            locals.computeIfAbsent(slot, k -> new TreeMap<>()).putIfAbsent(t, "l" + slot);
        }

        String local(int slot, char t) {
            return locals.get(slot).get(t);
        }

        /** One basic block, from the stack it starts with. */
        final class Block {
            final Deque<V> stack = new ArrayDeque<>();
            final List<String> lines = new ArrayList<>();
            final Map<Integer, Integer> blockOf;
            final List<Integer> next = new ArrayList<>();
            /** In a try range: its lines run in a closure, so a return says so (JFlow). */
            final boolean guarded;
            List<Character> exit = List.of();

            Block(List<Character> entry, Map<Integer, Integer> blockOf, boolean guarded) {
                this.blockOf = blockOf;
                this.guarded = guarded;
                for (int d = 0; d < entry.size(); d++) stack.addLast(new V(entry.get(d), stackVar(d, entry.get(d)), true));
            }

            List<String> run(int from, int to) {
                for (int k = from; k < to; k++) {
                    if (step(code.get(k))) return lines;
                }
                // Falls through into the next block.
                int target = blockOf.get(to);
                spill();
                lines.add("block = " + target + ";");
                next.add(target);
                return lines;
            }

            /** One instruction; true when it ends the block. */
            boolean step(Insn i) {
                int op = i.op();
                if (op == NOP) return false;
                if (op == ACONST_NULL) push('A', "None");
                else if (op >= ICONST_M1 && op <= ICONST_5) push('I', literal(op - 3));
                else if (op == LCONST_0 || op == LCONST_1) push('J', literal((long) (op - LCONST_0)));
                else if (op >= FCONST_0 && op <= FCONST_2) push('F', literal((float) (op - FCONST_0)));
                else if (op == DCONST_0 || op == DCONST_1) push('D', literal((double) (op - DCONST_0)));
                else if (op == BIPUSH || op == SIPUSH) push('I', literal(i.index()));
                else if (op == LDC || op == LDC_W || op == LDC2_W) {
                    Object c = pool.constant(i.index());
                    switch (c) {
                        case Integer n -> push('I', literal(n));
                        case Long n -> push('J', literal(n));
                        case Float n -> push('F', literal(n));
                        case Double n -> push('D', literal(n));
                        case String text -> pushTemp('A', names.literal(text) + "()");
                        case ClassData.TypeName t -> pushTemp('A', "jclass_object(" + names.id(t.descriptor()) + ")");
                        default -> throw new IllegalStateException("ldc of " + c);
                    }
                } else if (op >= ILOAD && op <= ALOAD) {
                    char t = "IJFDA".charAt(op - ILOAD);
                    push(t, local(i.index(), t));
                } else if (op >= ISTORE && op <= ASTORE) {
                    char t = "IJFDA".charAt(op - ISTORE);
                    V v = stack.removeLast();
                    String value = val(v);
                    settle();
                    lines.add(local(i.index(), t) + " = " + value + ";");
                } else if (op == IINC) {
                    settle();
                    String l = local(i.index(), 'I');
                    lines.add(l + " = " + l + ".wrapping_add(" + literal(i.value()) + ");");
                } else if (op >= IALOAD && op <= SALOAD) {
                    V index = stack.removeLast(), array = stack.removeLast();
                    char t = "IJFDAIII".charAt(op - IALOAD);
                    pushTemp(t, getter(t) + "(jaget(" + ref(array) + ", " + index.e() + ")?)");
                } else if (op >= IASTORE && op <= SASTORE) {
                    V value = stack.removeLast(), index = stack.removeLast(), array = stack.removeLast();
                    String a = val(array), x = index.e(), v = val(value);
                    settle();
                    lines.add(switch (op) {
                        case 84 -> "jbastore(&" + a + ", " + x + ", " + v + ")?;";
                        case 85 -> "japut(&" + a + ", " + x + ", Value::I((" + paren(value) + " as u16) as i32))?;";
                        case 86 -> "japut(&" + a + ", " + x + ", Value::I((" + paren(value) + " as i16) as i32))?;";
                        case 83 -> "jaastore(&" + a + ", " + x + ", " + v + ")?;";
                        default -> "japut(&" + a + ", " + x + ", " + wrap("IJFD".charAt(op - IASTORE), v) + ")?;";
                    });
                } else if (op >= POP && op <= SWAP) {
                    shuffle(op);
                } else if (op >= IADD && op <= LXOR) {
                    arithmetic(op);
                } else if (op >= I2L && op <= I2S) {
                    convert(op);
                } else if (op >= LCMP && op <= DCMPG) {
                    V b = stack.removeLast(), a = stack.removeLast();
                    String h = switch (op) {
                        case LCMP -> "jcmp_i64";
                        case FCMPL -> "jcmpl_f32";
                        case FCMPG -> "jcmpg_f32";
                        case DCMPL -> "jcmpl_f64";
                        default -> "jcmpg_f64";
                    };
                    helpers.add(h);
                    push('I', h + "(" + a.e() + ", " + b.e() + ")", false);
                } else if ((op >= IFEQ && op <= IF_ACMPNE) || op == IFNULL || op == IFNONNULL) {
                    String cond;
                    if (op <= IFLE) {
                        V v = stack.removeLast();
                        cond = paren(v) + " " + COMPARE[op - IFEQ] + " 0";
                    } else if (op <= IF_ICMPLE) {
                        V b = stack.removeLast(), a = stack.removeLast();
                        cond = paren(a) + " " + COMPARE[op - IF_ICMPEQ] + " " + paren(b);
                    } else if (op == IF_ACMPEQ || op == IF_ACMPNE) {
                        V b = stack.removeLast(), a = stack.removeLast();
                        cond = (op == IF_ACMPNE ? "!" : "") + "jsame(" + ref(a) + ", " + ref(b) + ")";
                    } else {
                        V v = stack.removeLast();
                        cond = "(" + ref(v) + ")." + (op == IFNULL ? "is_none()" : "is_some()");
                    }
                    String c = temp("bool", cond);
                    spill();
                    int yes = blockOf.get(at.get(i.targets()[0])), no = blockOf.get(at.get(i.pc()) + 1);
                    lines.add("block = if " + c + " { " + yes + " } else { " + no + " };");
                    next.add(yes);
                    next.add(no);
                    return true;
                } else if (op == GOTO || op == GOTO_W) {
                    spill();
                    int target = blockOf.get(at.get(i.targets()[0]));
                    lines.add("block = " + target + ";");
                    next.add(target);
                    return true;
                } else if (op == TABLESWITCH || op == LOOKUPSWITCH) {
                    V key = stack.removeLast();
                    String k = temp("i32", key.e());
                    spill();
                    StringBuilder b = new StringBuilder("block = match " + k + " {");
                    Map<Integer, List<Integer>> byTarget = new LinkedHashMap<>();
                    for (int c = 0; c < i.keys().length; c++) byTarget.computeIfAbsent(blockOf.get(at.get(i.targets()[c + 1])), x -> new ArrayList<>()).add(i.keys()[c]);
                    byTarget.forEach((target, keys) -> {
                        b.append(' ').append(String.join(" | ", keys.stream().map(n -> literal((int) n)).toList())).append(" => ").append(target).append(',');
                        next.add(target);
                    });
                    int fallback = blockOf.get(at.get(i.targets()[0]));
                    b.append(" _ => ").append(fallback).append(" };");
                    next.add(fallback);
                    lines.add(b.toString());
                    return true;
                } else if (op >= IRETURN && op <= ARETURN) {
                    V v = stack.removeLast();
                    lines.add(ret(to(ClassData.returns(m.descriptor()), val(v))));
                    return true;
                } else if (op == RETURN) {
                    lines.add(ret("()"));
                    return true;
                } else if (op == GETSTATIC || op == PUTSTATIC) {
                    String[] t = pool.member(i.index());
                    JavaWorld.FieldAt f = world.field(t[0], t[1], t[2], true);
                    String init = needsInit(owner, f.declaring(), world) ? names.init(f.declaring()) + "()?; " : "";
                    if (op == GETSTATIC) {
                        if (!init.isEmpty()) {
                            settle();
                            lines.add(init.strip());
                        }
                        pushTemp(f.kind(), getter(f.kind()) + "(jsget(&" + names.statics(f.declaring()) + ", " + f.slot() + "))");
                    } else {
                        V v = stack.removeLast();
                        String value = val(v);
                        settle();
                        lines.add(init + "jsput(&" + names.statics(f.declaring()) + ", " + f.slot() + ", " + wrap(f.kind(), narrow(f.descriptor(), value, v)) + ");");
                    }
                } else if (op == GETFIELD) {
                    String[] t = pool.member(i.index());
                    JavaWorld.FieldAt f = world.field(t[0], t[1], t[2], false);
                    V o = stack.removeLast();
                    pushTemp(f.kind(), getter(f.kind()) + "(jget(" + ref(o) + ", " + f.slot() + ")?)");
                } else if (op == PUTFIELD) {
                    String[] t = pool.member(i.index());
                    JavaWorld.FieldAt f = world.field(t[0], t[1], t[2], false);
                    V v = stack.removeLast(), o = stack.removeLast();
                    String value = val(v), object = ref(o);
                    settle();
                    lines.add("jput(" + object + ", " + f.slot() + ", " + wrap(f.kind(), narrow(f.descriptor(), value, v)) + ")?;");
                } else if (op >= INVOKEVIRTUAL && op <= INVOKEINTERFACE) {
                    call(i);
                } else if (op == INVOKEDYNAMIC && pool.lambdaSite(i.index()) == null
                        && pool.bootstrap(i.index()).owner().equals("java/lang/runtime/ObjectMethods")) {
                    String key = OBJECT_METHODS + owner + "#" + i.index();
                    List<String> params = ClassData.parameters(pool.dynamic(i.index())[1]);
                    String[] args = new String[params.size()];
                    for (int k = params.size() - 1; k >= 0; k--) args[k] = val(stack.removeLast());
                    settle();
                    String r = ClassData.returns(pool.dynamic(i.index())[1]);
                    pushTemp(JavaWorld.kind(r), from(r, names.objectMethods(key) + "(" + String.join(", ", args) + ")?"));
                } else if (op == INVOKEDYNAMIC && pool.lambdaSite(i.index()) == null && pool.bootstrap(i.index()).owner().equals(SWITCH_BOOTSTRAPS)) {
                    V restart = stack.removeLast(), target = settled(stack.removeLast());
                    settle();
                    String r = temp("i32", restart.e());
                    StringBuilder e = new StringBuilder("if " + target.e() + ".is_none() { -1 }");
                    List<Object> labels = pool.bootstrap(i.index()).arguments();
                    for (int k = 0; k < labels.size(); k++) {
                        String type = ((ClassData.TypeName) labels.get(k)).descriptor();
                        e.append(" else if ").append(r).append(" <= ").append(k).append(" && jinstanceof(&").append(target.e()).append(", ").append(names.typeTest(type))
                                .append(") == 1 { ").append(k).append(" }");
                    }
                    e.append(" else { ").append(labels.size()).append(" }");
                    pushTemp('I', e.toString());
                } else if (op == INVOKEDYNAMIC && pool.lambdaSite(i.index()) == null) {
                    concat(i);
                } else if (op == MONITORENTER || op == MONITOREXIT) {
                    V v = stack.removeLast();
                    settle();
                    lines.add((op == MONITORENTER ? "jmonitor_enter(" : "jmonitor_exit(") + ref(v) + ")?;");
                } else if (op == ATHROW) {
                    V v = stack.removeLast();
                    lines.add("return Err(jthrow(" + val(v) + "));");
                    return true;
                } else if (op == INVOKEDYNAMIC) {
                    ClassData.LambdaSite site = pool.lambdaSite(i.index());
                    String key = JavaWorld.lambdaKey(owner, i.index());
                    List<String> captured = ClassData.parameters(site.site());
                    String[] args = new String[captured.size()];
                    for (int k = captured.size() - 1; k >= 0; k--) args[k] = val(stack.removeLast());
                    List<String> converted = new ArrayList<>();
                    for (int k = 0; k < args.length; k++) converted.add(to(captured.get(k), args[k]));
                    settle();
                    pushTemp('A', names.lambdaNew(key) + "(" + String.join(", ", converted) + ")");
                } else if (op == NEW) {
                    String c = pool.className(i.index());
                    if (!c.equals(owner)) {
                        settle();
                        lines.add(names.init(c) + "()?;");
                    }
                    pushTemp('A', "jnew(" + names.id(c) + ", b\"" + world.layout(c) + "\")");
                } else if (op == NEWARRAY || op == ANEWARRAY) {
                    V n = stack.removeLast();
                    String type = op == NEWARRAY ? JavaWorld.PRIMITIVE_ARRAYS.get(arrayKind(i.value())) : "[" + arrayComponent(pool.className(i.index()));
                    char k = JavaWorld.kind(type.substring(1));
                    settle();
                    pushTemp('A', "jnewarray(" + names.id(type) + ", b'" + k + "', " + n.e() + ")?");
                } else if (op == ARRAYLENGTH) {
                    V a = stack.removeLast();
                    pushTemp('I', "jalen(" + ref(a) + ")?");
                } else if (op == CHECKCAST) {
                    V v = stack.removeLast();
                    String value = val(v);
                    settle();
                    pushTemp('A', "jcheckcast(" + value + ", " + names.typeTest(pool.className(i.index())) + ")?");
                } else if (op == INSTANCEOF) {
                    V v = stack.removeLast();
                    pushTemp('I', "jinstanceof(" + ref(v) + ", " + names.typeTest(pool.className(i.index())) + ")");
                } else {
                    throw new IllegalStateException("not translated: " + Bytecode.name(op));
                }
                return false;
            }

            /**
             * A text joined (StringConcatFactory): the recipe's pieces and each value as the model's String.valueOf
             * makes it, in order, into an array that the model's jconcat joins.
             */
            void concat(Insn i) {
                ClassData.Bootstrap b = pool.bootstrap(i.index());
                List<String> params = ClassData.parameters(pool.dynamic(i.index())[1]);
                String[] args = new String[params.size()];
                for (int k = params.size() - 1; k >= 0; k--) args[k] = val(stack.removeLast());
                settle();
                String recipe = b.name().equals("makeConcat") ? "\u0001".repeat(params.size()) : (String) b.arguments().get(0);
                List<String> pieces = new ArrayList<>();
                StringBuilder text = new StringBuilder();
                int arg = 0, constant = 1;
                for (int k = 0; k < recipe.length(); k++) {
                    char c = recipe.charAt(k);
                    if (c == '\u0001' || c == '\u0002') {
                        if (text.length() > 0) pieces.add(names.literal(text.toString()) + "()");
                        text.setLength(0);
                        if (c == '\u0002') {
                            pieces.add(names.literal(String.valueOf(b.arguments().get(constant++))) + "()");
                        } else {
                            String d = params.get(arg);
                            pieces.add(names.function(textOf(d)) + "(" + to(d.equals("B") || d.equals("S") ? "I" : d, args[arg]) + ")?");
                            arg++;
                        }
                    } else {
                        text.append(c);
                    }
                }
                if (text.length() > 0) pieces.add(names.literal(text.toString()) + "()");
                String array = temp("Ref", "jnewarray(" + names.id("[Ljava/lang/String;") + ", b'A', " + pieces.size() + ")?");
                for (int k = 0; k < pieces.size(); k++) lines.add("japut(&" + array + ", " + k + ", Value::A(" + pieces.get(k) + "))?;");
                pushTemp('A', names.function(CONCAT) + "(" + array + ")?");
            }

            String ret(String value) {
                return guarded ? "return Ok(JFlow::Return(" + value + "));" : "return Ok(" + value + ");";
            }

            void call(Insn i) {
                int op = i.op();
                String[] t = pool.member(i.index());
                Call call = new Call(t[0], t[1], t[2]);
                List<String> params = new ArrayList<>(ClassData.parameters(call.descriptor()));
                String[] args = new String[params.size()];
                for (int k = params.size() - 1; k >= 0; k--) args[k] = val(stack.removeLast());
                String receiver = op == INVOKESTATIC ? null : val(stack.removeLast());
                String r = ClassData.returns(call.descriptor());
                String lib = op == INVOKESTATIC ? library(call) : null;
                String expr;
                if (lib != null) {
                    expr = lib;
                    for (int k = 0; k < args.length; k++) expr = expr.replace("$" + k, !simpleText(args[k]) ? "(" + args[k] + ")" : args[k]);
                    for (String h : HELPERS.keySet()) if (expr.startsWith(h + "(")) helpers.add(h);
                } else if (op == INVOKESPECIAL && t[0].equals(JavaWorld.OBJECT) && t[1].equals("<init>")) {
                    // Object's constructor does nothing.
                    return;
                } else {
                    String name;
                    if (op == INVOKESTATIC) {
                        String declaring = t[0];
                        for (String c = t[0]; c != null && world.cls(c) != null; c = world.cls(c).superName) {
                            if (world.declared(c, t[1], t[2]) != null) {
                                declaring = c;
                                break;
                            }
                        }
                        if (needsInit(owner, declaring, world)) {
                            settle();
                            lines.add(names.init(declaring) + "()?;");
                        }
                        name = names.function(new JarRust.Id(declaring, t[1], t[2]));
                    } else if (op == INVOKESPECIAL) {
                        name = names.function(world.special(owner, t[0], t[1], t[2]).id());
                    } else {
                        ClassData.Method declared = world.declared(t[0], t[1], t[2]);
                        name = declared != null && (declared.access() & 0x0002) != 0 ? names.function(new JarRust.Id(t[0], t[1], t[2])) : names.dispatcher(t[0], t[1], t[2]);
                    }
                    List<String> converted = new ArrayList<>();
                    if (receiver != null) converted.add(receiver);
                    for (int k = 0; k < args.length; k++) converted.add(to(params.get(k), args[k]));
                    expr = name + "(" + String.join(", ", converted) + ")?";
                }
                // A call happens where it is: its result goes into a variable at once.
                settle();
                if (r.equals("V")) {
                    lines.add(expr + ";");
                } else {
                    String back = lib != null ? expr : from(r, expr);
                    pushTemp(JavaWorld.kind(r), back);
                }
            }

            void arithmetic(int op) {
                int kind = (op - IADD) / 4, width = (op - IADD) % 4;
                if (op >= 116 && op <= 119) {
                    V a = stack.removeLast();
                    char t = a.t();
                    push(t, t == 'I' || t == 'J' ? paren(a) + ".wrapping_neg()" : "-" + paren(a), false);
                    return;
                }
                if (op >= 120) {
                    V b = stack.removeLast(), a = stack.removeLast();
                    int which = op - 120;
                    char t = which % 2 == 0 ? 'I' : 'J';
                    String e = switch (which / 2) {
                        case 0 -> paren(a) + ".wrapping_shl(" + paren(b) + " as u32)";
                        case 1 -> paren(a) + ".wrapping_shr(" + paren(b) + " as u32)";
                        case 2 -> "(" + paren(a) + " as " + (t == 'I' ? "u32" : "u64") + ").wrapping_shr(" + paren(b) + " as u32) as " + (t == 'I' ? "i32" : "i64");
                        case 3 -> paren(a) + " & " + paren(b);
                        case 4 -> paren(a) + " | " + paren(b);
                        default -> paren(a) + " ^ " + paren(b);
                    };
                    push(t, e, false);
                    return;
                }
                V b = stack.removeLast(), a = stack.removeLast();
                char t = "IJFD".charAt(width);
                boolean integer = t == 'I' || t == 'J';
                String e = !integer ? paren(a) + " " + "+-*/%".charAt(kind) + " " + paren(b)
                        : kind >= 3 ? (kind == 3 ? "jdiv_" : "jrem_") + (t == 'I' ? "i32" : "i64") + "(" + a.e() + ", " + b.e() + ")?"
                        : paren(a) + "." + new String[] {"wrapping_add", "wrapping_sub", "wrapping_mul"}[kind] + "(" + b.e() + ")";
                if (integer && kind >= 3) {
                    // Division by zero throws where it is in Java: it is done here, not later.
                    settle();
                    pushTemp(t, e);
                } else {
                    push(t, e, false);
                }
            }

            void convert(int op) {
                V a = stack.removeLast();
                String e = switch (op) {
                    case 133, 140, 143 -> cast(a, "i64");
                    case 134, 137, 144 -> cast(a, "f32");
                    case 135, 138, 141 -> cast(a, "f64");
                    case 136, 139, 142 -> cast(a, "i32");
                    case 145 -> "(" + cast(a, "i8") + " as i32)";
                    case 146 -> "(" + cast(a, "u16") + " as i32)";
                    default -> "(" + cast(a, "i16") + " as i32)";
                };
                char t = "JFDIFDIJDIJFIII".charAt(op - I2L);
                push(t, e, false);
            }

            void shuffle(int op) {
                switch (op) {
                    case POP -> stack.removeLast();
                    case POP2 -> {
                        V a = stack.removeLast();
                        if (!a.wide()) stack.removeLast();
                    }
                    case DUP -> {
                        V a = settled(stack.removeLast());
                        stack.addLast(a);
                        stack.addLast(a);
                    }
                    case DUP_X1 -> {
                        V a = settled(stack.removeLast()), b = stack.removeLast();
                        stack.addLast(a);
                        stack.addLast(b);
                        stack.addLast(a);
                    }
                    case DUP_X2 -> {
                        V a = settled(stack.removeLast()), b = stack.removeLast();
                        if (b.wide()) {
                            stack.addLast(a);
                            stack.addLast(b);
                            stack.addLast(a);
                        } else {
                            V c = stack.removeLast();
                            stack.addLast(a);
                            stack.addLast(c);
                            stack.addLast(b);
                            stack.addLast(a);
                        }
                    }
                    case DUP2 -> {
                        V a = settled(stack.removeLast());
                        if (a.wide()) {
                            stack.addLast(a);
                            stack.addLast(a);
                        } else {
                            V b = settled(stack.removeLast());
                            stack.addLast(b);
                            stack.addLast(a);
                            stack.addLast(b);
                            stack.addLast(a);
                        }
                    }
                    case DUP2_X1 -> {
                        V a = settled(stack.removeLast());
                        if (a.wide()) {
                            V b = stack.removeLast();
                            stack.addLast(a);
                            stack.addLast(b);
                            stack.addLast(a);
                        } else {
                            V b = settled(stack.removeLast()), c = stack.removeLast();
                            stack.addLast(b);
                            stack.addLast(a);
                            stack.addLast(c);
                            stack.addLast(b);
                            stack.addLast(a);
                        }
                    }
                    case DUP2_X2 -> {
                        V a = settled(stack.removeLast());
                        List<V> top = new ArrayList<>();
                        top.add(a);
                        if (!a.wide()) top.add(0, settled(stack.removeLast()));
                        V c = stack.removeLast();
                        List<V> under = new ArrayList<>(List.of(c));
                        if (!c.wide()) under.add(0, stack.removeLast());
                        top.forEach(stack::addLast);
                        under.forEach(stack::addLast);
                        top.forEach(stack::addLast);
                    }
                    default -> {
                        V a = stack.removeLast(), b = stack.removeLast();
                        stack.addLast(a);
                        stack.addLast(b);
                    }
                }
            }

            // ---- the stack ----

            void push(char t, String e) {
                stack.addLast(new V(t, e, true));
            }

            void push(char t, String e, boolean simple) {
                stack.addLast(new V(t, e, simple));
            }

            /** An expression that reads or makes something where it is: into a variable at once. */
            void pushTemp(char t, String e) {
                stack.addLast(new V(t, temp(kindType(t), e), true));
            }

            /** A value made a variable when it isn't one, so that it can be used twice. */
            V settled(V v) {
                if (v.simple()) return v;
                return new V(v.t(), temp(kindType(v.t()), v.e()), true);
            }

            /**
             * Every value on the stack that reads a variable is made a variable of its own, so that what is
             * written next (a local, a field, a call) can't change it.
             */
            void settle() {
                List<V> all = new ArrayList<>(stack);
                stack.clear();
                for (V v : all) {
                    boolean keep = v.simple() && (literalText(v.e()) || v.e().startsWith("t"));
                    stack.addLast(keep ? v : new V(v.t(), temp(kindType(v.t()), val(v)), true));
                }
            }

            /** The stack handed to the next block, in the stack variables: read all first, then written. */
            void spill() {
                List<V> all = new ArrayList<>(stack);
                exit = all.stream().map(V::t).toList();
                List<String> values = new ArrayList<>();
                for (int d = 0; d < all.size(); d++) {
                    String target = stackVar(d, all.get(d).t());
                    values.add(all.get(d).e().equals(target) ? null : temp(kindType(all.get(d).t()), val(all.get(d))));
                }
                for (int d = 0; d < all.size(); d++) {
                    if (values.get(d) != null) lines.add(stackVar(d, all.get(d).t()) + " = " + values.get(d) + ";");
                }
            }

            String temp(String type, String e) {
                String name = "t" + temps++;
                lines.add("let " + name + ": " + type + " = " + e + ";");
                return name;
            }
        }

        String stackVar(int depth, char t) {
            String name = "s" + depth + "_" + Character.toLowerCase(t);
            stackVars.put(name, t);
            return name;
        }

        /** An int of the stack stored into a field of a narrower type keeps only what that type holds, as putfield does. */
        static String narrow(String descriptor, String value, V v) {
            return switch (descriptor) {
                case "Z" -> "(" + value + " & 1)";
                case "B" -> "((" + value + " as i8) as i32)";
                case "C" -> "((" + value + " as u16) as i32)";
                case "S" -> "((" + value + " as i16) as i32)";
                default -> value;
            };
        }

        /** A value of the descriptor's type as the int the stack holds (boolean, byte, char, short are ints there). */
        static String from(String d, String e) {
            return switch (d) {
                case "Z", "B", "C", "S" -> "(" + e + " as i32)";
                default -> e;
            };
        }

        /** An int of the stack as the descriptor's type. */
        static String to(String d, String e) {
            return switch (d) {
                case "Z" -> "(" + e + " != 0)";
                case "B" -> "(" + e + " as i8)";
                case "C" -> "(" + e + " as u16)";
                case "S" -> "(" + e + " as i16)";
                default -> e;
            };
        }
    }

    // ---- small things ----

    private static final String[] COMPARE = {"==", "!=", "<", ">=", ">", "<="};

    private static boolean ends(int op) {
        return (op >= IFEQ && op <= RETURN) || op == GOTO_W || op == IFNULL || op == IFNONNULL || op == ATHROW;
    }

    /** The component descriptor of an array whose components are of anewarray's class. */
    static String arrayComponent(String className) {
        return className.startsWith("[") ? className : "L" + className + ";";
    }

    /** The Rust type of a descriptor, at a function's edge. */
    static String rust(String d) {
        return switch (d.charAt(0)) {
            case 'I' -> "i32";
            case 'J' -> "i64";
            case 'F' -> "f32";
            case 'D' -> "f64";
            case 'Z' -> "bool";
            case 'B' -> "i8";
            case 'C' -> "u16";
            case 'S' -> "i16";
            case 'L', '[' -> "Ref";
            default -> throw new IllegalArgumentException(d);
        };
    }

    /** The Rust type of a stack kind. */
    static String kindType(char t) {
        return switch (t) {
            case 'J' -> "i64";
            case 'F' -> "f32";
            case 'D' -> "f64";
            case 'A' -> "Ref";
            default -> "i32";
        };
    }

    private static String zero(char t) {
        return switch (t) {
            case 'J' -> "0i64";
            case 'F' -> "0.0f32";
            case 'D' -> "0.0f64";
            case 'A' -> "None";
            default -> "0i32";
        };
    }

    /** jget's Value as the kind: gi, gj, gf, gd, ga. */
    static String getter(char t) {
        return "jg" + Character.toLowerCase(t);
    }

    static String wrap(char t, String e) {
        return "Value::" + t + "(" + e + ")";
    }

    /** A value used where it is consumed: a variable holding a reference is cloned, as the reference is copied in Java. */
    private static String val(V v) {
        return v.t() == 'A' && v.simple() && !v.e().equals("None") ? v.e() + ".clone()" : v.e();
    }

    /** A reference borrowed. */
    private static String ref(V v) {
        return v.simple() ? "&" + v.e() : "&(" + v.e() + ")";
    }

    private static String paren(V v) {
        return v.simple() && !v.e().startsWith("-") ? v.e() : "(" + v.e() + ")";
    }

    private static boolean literalText(String e) {
        return e.equals("None") || e.startsWith("(") || Character.isDigit(e.charAt(0)) || e.startsWith("i32::") || e.startsWith("i64::")
                || e.startsWith("f32::") || e.startsWith("f64::");
    }

    private static boolean simpleText(String e) {
        return e.matches("[A-Za-z_][A-Za-z0-9_]*|[0-9][0-9a-z_.]*|[A-Za-z0-9_]+::[A-Z_]+");
    }

    private static String cast(V v, String to) {
        return paren(v) + " as " + to;
    }

    /** A Rust string literal of this text. */
    static String rustText(String text) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c < 0x20 || c > 0x7e) b.append(String.format("\\u{%x}", (int) c));
            else b.append(c);
        }
        return b.append('"').toString();
    }

    static String literal(int n) {
        if (n == Integer.MIN_VALUE) return "i32::MIN";
        return n < 0 ? "(" + n + "i32)" : n + "i32";
    }

    static String literal(long n) {
        if (n == Long.MIN_VALUE) return "i64::MIN";
        return n < 0 ? "(" + n + "i64)" : n + "i64";
    }

    static String literal(float f) {
        if (Float.isNaN(f) || Float.isInfinite(f)) return "f32::from_bits(0x" + Integer.toHexString(Float.floatToRawIntBits(f)) + ")";
        String s = Float.toString(f).replace('E', 'e') + "f32";
        return s.startsWith("-") ? "(" + s + ")" : s;
    }

    static String literal(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) return "f64::from_bits(0x" + Long.toHexString(Double.doubleToRawLongBits(d)) + ")";
        String s = Double.toString(d).replace('E', 'e') + "f64";
        return s.startsWith("-") ? "(" + s + ")" : s;
    }
}
