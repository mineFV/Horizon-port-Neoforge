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
 * of bounds, a failed cast, an int divided by zero), this panics with the exception's name; a method that catches
 * or throws exceptions isn't translated yet, nor one that uses texts, Java's library beyond Math, or the game.
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
    }

    // ---- what can be translated ----

    static Check check(String owner, ClassData.Method m, ClassData.Pool pool, JavaWorld world) {
        List<Need> needs = new ArrayList<>();
        if (m.body() == null) return new Check("it has no body", needs);
        if (!m.body().handlers().isEmpty()) return new Check("it catches exceptions: try/catch isn't translated yet", needs);
        if ((m.access() & 0x0020) != 0) return new Check("it is synchronized: monitors aren't translated yet", needs);
        List<Insn> code;
        try {
            code = Bytecode.decode(m.body().code());
        } catch (Bytecode.Invalid e) {
            return new Check("its bytecode doesn't decode: " + e.getMessage(), needs);
        }
        for (Insn i : code) {
            String why = need(owner, i, pool, world, needs);
            if (why != null) return new Check(why, needs);
        }
        return new Check(null, needs);
    }

    /** What one instruction needs, added to needs; why it can't be translated, else null. */
    private static String need(String owner, Insn i, ClassData.Pool pool, JavaWorld world, List<Need> needs) {
        int op = i.op();
        switch (op) {
            case LDC, LDC_W, LDC2_W -> {
                Object c = pool.constant(i.index());
                if (c instanceof Number) return null;
                return c instanceof String ? "it uses a text: texts aren't translated yet" : "it uses a class or another constant: classes as values aren't translated yet";
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
                    if (m == null) return "it calls " + display(call) + ", which isn't found in the jar";
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
                    String[] d = pool.dynamic(i.index());
                    return d[0].equals("makeConcatWithConstants") ? "it joins texts: texts aren't translated yet"
                            : "it uses an invokedynamic that isn't a plain lambda (" + d[0] + "): not translated yet";
                }
                String why = lambdaWhy(site, world);
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
                switch (site.kind()) {
                    case 5, 9 -> needs.add(new Need("dispatch", null, site.owner(), site.name(), site.descriptor()));
                    case 8 -> {
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
                if (outside(c) != null) return "it makes an object of " + outside(c) + ": " + c.replace('/', '.');
                if (world.cls(c) == null) return "it makes an object of another library or mod: " + c.replace('/', '.');
                if (!world.concrete(c)) return "it makes an object whose superclass is outside the jar: " + c.replace('/', '.');
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
                return "it throws an exception: exceptions aren't translated yet";
            }
            case MONITORENTER, MONITOREXIT -> {
                return "it has a synchronized block: monitors aren't translated yet";
            }
            case JSR, JSR_W, RET -> {
                return "it uses a subroutine (class files before Java 7): not translated";
            }
            default -> {
                return null;
            }
        }
    }

    /** Why a lambda site can't be translated: its values would need boxing, or another adaptation. */
    private static String lambdaWhy(ClassData.LambdaSite site, JavaWorld world) {
        if (outside(site.owner()) != null) return "it makes a lambda that runs " + outside(site.owner()) + ": " + site.owner().replace('/', '.') + "." + site.name();
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
            String a = given.get(k), b = taken.get(k);
            if (primitive(a) != primitive(b) || (primitive(a) && !a.equals(b))) return "it makes a lambda that boxes or widens a value (" + a + " to " + b + "): not translated yet";
        }
        String r = ClassData.returns(site.erased()), ir = site.kind() == 8 ? "L" + site.owner() + ";" : ClassData.returns(site.descriptor());
        if (r.equals("V")) return null;
        if (ir.equals("V") || primitive(r) != primitive(ir) || (primitive(r) && !r.equals(ir))) return "it makes a lambda whose result needs boxing or widening (" + ir + " to " + r + "): not translated yet";
        return null;
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
        if (outside(c) != null) return outside(c);
        return c.startsWith("[") || world.cls(c) != null ? null : "another library or mod";
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
            case "floorDiv(II)I" -> "jfloor_div_i32($0, $1)";
            case "floorDiv(JJ)J" -> "jfloor_div_i64($0, $1)";
            case "floorMod(II)I" -> "jfloor_mod_i32($0, $1)";
            case "floorMod(JJ)J" -> "jfloor_mod_i64($0, $1)";
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
            Map.entry("jfloor_div_i32", "fn jfloor_div_i32(a: i32, b: i32) -> i32 {\n    let q = a.wrapping_div(b);\n    if (a % b != 0) && ((a ^ b) < 0) { q - 1 } else { q }\n}"),
            Map.entry("jfloor_div_i64", "fn jfloor_div_i64(a: i64, b: i64) -> i64 {\n    let q = a.wrapping_div(b);\n    if (a.wrapping_rem(b) != 0) && ((a ^ b) < 0) { q - 1 } else { q }\n}"),
            Map.entry("jfloor_mod_i32", "fn jfloor_mod_i32(a: i32, b: i32) -> i32 {\n    let m = a.wrapping_rem(b);\n    if m != 0 && ((m ^ b) < 0) { m + b } else { m }\n}"),
            Map.entry("jfloor_mod_i64", "fn jfloor_mod_i64(a: i64, b: i64) -> i64 {\n    let m = a.wrapping_rem(b);\n    if m != 0 && ((m ^ b) < 0) { m + b } else { m }\n}")));

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
            List<Integer> starts = new ArrayList<>(leaders);
            Map<Integer, Integer> blockOf = new HashMap<>();
            for (int b = 0; b < starts.size(); b++) blockOf.put(starts.get(b), b);

            // Each block written in turn, from the stack it starts with: the first that reaches a block decides it.
            Map<Integer, List<Character>> entry = new HashMap<>();
            entry.put(0, List.of());
            Map<Integer, List<String>> bodies = new TreeMap<>();
            boolean flows = false;
            Deque<Integer> work = new ArrayDeque<>(List.of(0));
            while (!work.isEmpty()) {
                int b = work.poll();
                if (bodies.containsKey(b)) continue;
                int from = starts.get(b), to = b + 1 < starts.size() ? starts.get(b + 1) : code.size();
                Block block = new Block(entry.get(b), blockOf);
                bodies.put(b, block.run(from, to));
                flows |= !block.next.isEmpty();
                for (int next : block.next) {
                    List<Character> had = entry.putIfAbsent(next, block.exit);
                    if (had != null && !had.equals(block.exit)) throw new Bytecode.Invalid("the stack differs where two paths meet, at block " + next);
                    work.add(next);
                }
            }

            StringBuilder out = new StringBuilder();
            out.append("/// `").append(owner.replace('/', '.')).append('.').append(m.name()).append(m.descriptor()).append("`, translated from its bytecode.\n");
            out.append("#[allow(non_snake_case, unused_mut, unused_assignments, unused_variables, unused_parens, unreachable_code, clippy::all)]\n");
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
            out.append(')').append(r.equals("V") ? "" : " -> " + rust(r)).append(" {\n");
            converted.forEach(c -> out.append(c).append('\n'));
            for (Map<Character, String> types : locals.values()) {
                for (Map.Entry<Character, String> e : types.entrySet()) {
                    if (declared.add(e.getValue())) out.append("    let mut ").append(e.getValue()).append(": ").append(kindType(e.getKey())).append(" = ").append(zero(e.getKey())).append(";\n");
                }
            }
            stackVars.forEach((name, t) -> out.append("    let mut ").append(name).append(": ").append(kindType(t)).append(" = ").append(zero(t)).append(";\n"));
            if (!flows) {
                for (String line : bodies.get(0)) out.append("    ").append(line).append('\n');
            } else {
                out.append("    let mut block: u32 = 0;\n    loop {\n        match block {\n");
                bodies.forEach((b, lines) -> {
                    out.append("            ").append(b).append(" => {\n");
                    for (String line : lines) out.append("                ").append(line).append('\n');
                    out.append("            }\n");
                });
                out.append("            _ => unreachable!(),\n        }\n    }\n");
            }
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
            List<Character> exit = List.of();

            Block(List<Character> entry, Map<Integer, Integer> blockOf) {
                this.blockOf = blockOf;
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
                    pushTemp(t, getter(t) + "(jaget(" + ref(array) + ", " + index.e() + "))");
                } else if (op >= IASTORE && op <= SASTORE) {
                    V value = stack.removeLast(), index = stack.removeLast(), array = stack.removeLast();
                    String a = val(array), x = index.e(), v = val(value);
                    settle();
                    lines.add(switch (op) {
                        case 84 -> "jbastore(&" + a + ", " + x + ", " + v + ");";
                        case 85 -> "japut(&" + a + ", " + x + ", Value::I((" + paren(value) + " as u16) as i32));";
                        case 86 -> "japut(&" + a + ", " + x + ", Value::I((" + paren(value) + " as i16) as i32));";
                        case 83 -> "jaastore(&" + a + ", " + x + ", " + v + ");";
                        default -> "japut(&" + a + ", " + x + ", " + wrap("IJFD".charAt(op - IASTORE), v) + ");";
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
                    lines.add("return " + to(ClassData.returns(m.descriptor()), val(v)) + ";");
                    return true;
                } else if (op == RETURN) {
                    lines.add("return;");
                    return true;
                } else if (op == GETSTATIC || op == PUTSTATIC) {
                    String[] t = pool.member(i.index());
                    JavaWorld.FieldAt f = world.field(t[0], t[1], t[2], true);
                    String init = needsInit(owner, f.declaring(), world) ? names.init(f.declaring()) + "(); " : "";
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
                    pushTemp(f.kind(), getter(f.kind()) + "(jget(" + ref(o) + ", " + f.slot() + "))");
                } else if (op == PUTFIELD) {
                    String[] t = pool.member(i.index());
                    JavaWorld.FieldAt f = world.field(t[0], t[1], t[2], false);
                    V v = stack.removeLast(), o = stack.removeLast();
                    String value = val(v), object = ref(o);
                    settle();
                    lines.add("jput(" + object + ", " + f.slot() + ", " + wrap(f.kind(), narrow(f.descriptor(), value, v)) + ");");
                } else if (op >= INVOKEVIRTUAL && op <= INVOKEINTERFACE) {
                    call(i);
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
                        lines.add(names.init(c) + "();");
                    }
                    pushTemp('A', "jnew(" + names.id(c) + ", b\"" + world.layout(c) + "\")");
                } else if (op == NEWARRAY || op == ANEWARRAY) {
                    V n = stack.removeLast();
                    String type = op == NEWARRAY ? JavaWorld.PRIMITIVE_ARRAYS.get(arrayKind(i.value())) : "[" + arrayComponent(pool.className(i.index()));
                    char k = JavaWorld.kind(type.substring(1));
                    settle();
                    pushTemp('A', "jnewarray(" + names.id(type) + ", b'" + k + "', " + n.e() + ")");
                } else if (op == ARRAYLENGTH) {
                    V a = stack.removeLast();
                    pushTemp('I', "jalen(" + ref(a) + ")");
                } else if (op == CHECKCAST) {
                    V v = stack.removeLast();
                    String value = val(v);
                    settle();
                    pushTemp('A', "jcheckcast(" + value + ", " + names.typeTest(pool.className(i.index())) + ")");
                } else if (op == INSTANCEOF) {
                    V v = stack.removeLast();
                    pushTemp('I', "jinstanceof(" + ref(v) + ", " + names.typeTest(pool.className(i.index())) + ")");
                } else {
                    throw new IllegalStateException("not translated: " + Bytecode.name(op));
                }
                return false;
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
                            lines.add(names.init(declaring) + "();");
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
                    expr = name + "(" + String.join(", ", converted) + ")";
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
                String e = integer
                        ? paren(a) + "." + new String[] {"wrapping_add", "wrapping_sub", "wrapping_mul", "wrapping_div", "wrapping_rem"}[kind] + "(" + b.e() + ")"
                        : paren(a) + " " + "+-*/%".charAt(kind) + " " + paren(b);
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
