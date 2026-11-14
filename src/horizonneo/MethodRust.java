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
 * This is the first step of the translator (CLAUDE.md, phase 4): static methods of primitive values only (int,
 * long, float, double, and boolean, byte, char and short at their edges), with no exception handler, calling
 * Math and each other. Anything else is refused, with the first thing in the way: nothing is guessed.
 *
 * The control flow is kept as the bytecode has it: each basic block is an arm of a `match` in a `loop`, which
 * is exact for any flow (a method of one block is written straight). Values left on the operand stack between
 * blocks go through variables of their own (`s0`, `s1`...), each local slot is a variable of its type (`l3`,
 * or `l3_j` where a slot holds several types).
 */
final class MethodRust {
    private MethodRust() {}

    /** A method a call names. */
    record Call(String owner, String name, String descriptor) {}

    /** What the check found: why the method can't be translated (null when it can), and the methods of the mod it calls. */
    record Check(String why, List<Call> calls) {}

    /** The translation: the function's text, and the helpers of this tool's own it uses (`jcmpl_f32`...). */
    record Done(String rust, Set<String> helpers) {}

    /** Gives a called method's Rust name; null when it isn't translated. */
    interface Callees {
        String name(Call call);
    }

    // ---- what can be translated ----

    static Check check(ClassData.Method m, ClassData.Pool pool) {
        List<Call> calls = new ArrayList<>();
        if (m.body() == null) return new Check("it has no body", calls);
        if (!m.isStatic()) return new Check("an instance method: objects aren't translated yet", calls);
        if (m.name().equals("<clinit>")) return new Check("a static initializer: static fields aren't translated yet", calls);
        for (String p : ClassData.parameters(m.descriptor())) {
            if (!primitive(p)) return new Check("it takes an object (" + p + "): objects aren't translated yet", calls);
        }
        String r = ClassData.returns(m.descriptor());
        if (!r.equals("V") && !primitive(r)) return new Check("it returns an object (" + r + "): objects aren't translated yet", calls);
        if (!m.body().handlers().isEmpty()) return new Check("it catches exceptions: try/catch isn't translated yet", calls);
        List<Insn> code;
        try {
            code = Bytecode.decode(m.body().code());
        } catch (Bytecode.Invalid e) {
            return new Check("its bytecode doesn't decode: " + e.getMessage(), calls);
        }
        for (Insn i : code) {
            int op = i.op();
            if (op == LDC || op == LDC_W || op == LDC2_W) {
                Object c = pool.constant(i.index());
                if (!(c instanceof Number)) return new Check("it uses " + (c instanceof String ? "a text" : "a class or another constant") + ": objects aren't translated yet", calls);
                continue;
            }
            if (op == INVOKESTATIC) {
                String[] t = pool.member(i.index());
                Call call = new Call(t[0], t[1], t[2]);
                if (library(call) != null) continue;
                if (t[0].startsWith("java/") || t[0].startsWith("javax/")) return new Check("it calls " + display(call) + ", which this translator doesn't know", calls);
                if (t[0].startsWith("net/minecraft/") || t[0].startsWith("net/neoforged/") || t[0].startsWith("com/mojang/")) {
                    return new Check("it calls the game: " + display(call), calls);
                }
                calls.add(call);
                continue;
            }
            if (!supported(op)) return new Check("it uses " + Bytecode.name(op) + why(op), calls);
        }
        return new Check(null, calls);
    }

    private static boolean supported(int op) {
        return op == NOP || (op >= ICONST_M1 && op <= SIPUSH) || (op >= ILOAD && op <= DLOAD) || (op >= ISTORE && op <= ISTORE + 3)
                || (op >= POP && op <= SWAP) || (op >= IADD && op <= IINC) || (op >= I2L && op <= I2S) || (op >= LCMP && op <= DCMPG)
                || (op >= IFEQ && op <= IF_ICMPLE) || op == GOTO || op == GOTO_W || op == TABLESWITCH || op == LOOKUPSWITCH
                || (op >= IRETURN && op <= DRETURN) || op == RETURN;
    }

    private static String why(int op) {
        if (op == ACONST_NULL || op == ALOAD || op == ASTORE || op == ARETURN || op == IF_ACMPEQ || op == IF_ACMPNE || op == IFNULL || op == IFNONNULL
                || op == NEW || op == CHECKCAST || op == INSTANCEOF || op == GETFIELD || op == PUTFIELD || op == INVOKEVIRTUAL || op == INVOKESPECIAL
                || op == INVOKEINTERFACE || op == MONITORENTER || op == MONITOREXIT) return ": objects aren't translated yet";
        if ((op >= IALOAD && op <= SALOAD) || (op >= IASTORE && op <= SASTORE) || op == NEWARRAY || op == ANEWARRAY || op == MULTIANEWARRAY
                || op == ARRAYLENGTH) return ": arrays aren't translated yet";
        if (op == GETSTATIC || op == PUTSTATIC) return ": static fields aren't translated yet";
        if (op == INVOKEDYNAMIC) return ": lambdas and string concatenation aren't translated yet";
        if (op == ATHROW) return ": exceptions aren't translated yet";
        if (op == JSR || op == JSR_W || op == RET) return ": subroutines (class files before Java 7) aren't translated";
        return "";
    }

    static boolean primitive(String d) {
        return d.length() == 1 && "IJFDZBCS".contains(d);
    }

    static String display(Call c) {
        return c.owner().replace('/', '.') + "." + c.name() + c.descriptor();
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

    /** This tool's helpers, each written once in the translated module when used. */
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

    /** A value on the operand stack: its type (I, J, F, D) and its Rust expression; simple when a name or a literal. */
    private record V(char t, String e, boolean simple) {
        boolean wide() {
            return t == 'J' || t == 'D';
        }
    }

    /** Writes the function. The method must have passed `check`, and each call it makes must have a name. */
    static Done write(String owner, ClassData.Method m, ClassData.Pool pool, String function, Callees callees) {
        return new Writer(owner, m, pool, function, callees).write();
    }

    private static final class Writer {
        final String owner;
        final ClassData.Method m;
        final ClassData.Pool pool;
        final String function;
        final Callees callees;
        final List<Insn> code;
        final Map<Integer, Integer> at = new HashMap<>();
        final Set<String> helpers = new TreeSet<>();
        /** Each slot's types, and the name of its variable for each. */
        final Map<Integer, Map<Character, String>> locals = new TreeMap<>();
        /** The stack variables used between blocks: name to type. */
        final Map<String, Character> stackVars = new TreeMap<>();
        int temps;

        Writer(String owner, ClassData.Method m, ClassData.Pool pool, String function, Callees callees) {
            this.owner = owner;
            this.m = m;
            this.pool = pool;
            this.function = function;
            this.callees = callees;
            this.code = Bytecode.decode(m.body().code());
            for (int i = 0; i < code.size(); i++) at.put(code.get(i).pc(), i);
        }

        Done write() {
            // Parameters: their slots and types.
            List<String> params = ClassData.parameters(m.descriptor());
            int slot = 0;
            List<int[]> paramSlots = new ArrayList<>();
            for (String p : params) {
                char t = stackType(p);
                type(slot, t);
                paramSlots.add(new int[] {slot});
                slot += t == 'J' || t == 'D' ? 2 : 1;
            }
            for (Insn i : code) {
                int op = i.op();
                if (op >= ILOAD && op <= DLOAD) type(i.index(), "IJFD".charAt(op - ILOAD));
                else if (op >= ISTORE && op <= ISTORE + 3) type(i.index(), "IJFD".charAt(op - ISTORE));
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
                Block block = new Block(entry.get(b), blockOf, starts);
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
            for (int k = 0; k < params.size(); k++) {
                int s = paramSlots.get(k)[0];
                String p = params.get(k);
                String name = locals.get(s).get(stackType(p));
                if (k > 0) out.append(", ");
                if (p.equals("I") || p.equals("J") || p.equals("F") || p.equals("D")) {
                    out.append("mut ").append(name).append(": ").append(rust(p));
                } else {
                    out.append("p").append(s).append(": ").append(rust(p));
                    converted.add("    let mut " + name + ": i32 = p" + s + " as i32;");
                }
            }
            String r = ClassData.returns(m.descriptor());
            out.append(')').append(r.equals("V") ? "" : " -> " + rust(r)).append(" {\n");
            converted.forEach(c -> out.append(c).append('\n'));
            Set<String> declared = new TreeSet<>();
            for (int k = 0; k < params.size(); k++) declared.add(locals.get(paramSlots.get(k)[0]).get(stackType(params.get(k))));
            for (Map<Character, String> types : locals.values()) {
                for (Map.Entry<Character, String> e : types.entrySet()) {
                    if (declared.add(e.getValue())) out.append("    let mut ").append(e.getValue()).append(": ").append(rust(String.valueOf(e.getKey()))).append(" = ").append(zero(e.getKey())).append(";\n");
                }
            }
            stackVars.forEach((name, t) -> out.append("    let mut ").append(name).append(": ").append(rust(String.valueOf(t))).append(" = ").append(zero(t)).append(";\n"));
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
            final List<Integer> starts;
            final List<Integer> next = new ArrayList<>();
            List<Character> exit = List.of();

            Block(List<Character> entry, Map<Integer, Integer> blockOf, List<Integer> starts) {
                this.blockOf = blockOf;
                this.starts = starts;
                for (int d = 0; d < entry.size(); d++) stack.addLast(new V(entry.get(d), stackVar(d, entry.get(d)), true));
            }

            List<String> run(int from, int to) {
                for (int k = from; k < to; k++) {
                    Insn i = code.get(k);
                    if (step(i)) return lines;
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
                if (op >= ICONST_M1 && op <= ICONST_5) push('I', literal(op - 3));
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
                } else if (op >= ILOAD && op <= DLOAD) {
                    char t = "IJFD".charAt(op - ILOAD);
                    push(t, local(i.index(), t));
                } else if (op >= ISTORE && op <= ISTORE + 3) {
                    char t = "IJFD".charAt(op - ISTORE);
                    V v = stack.removeLast();
                    settle();
                    lines.add(local(i.index(), t) + " = " + v.e() + ";");
                } else if (op == IINC) {
                    settle();
                    String l = local(i.index(), 'I');
                    lines.add(l + " = " + l + ".wrapping_add(" + literal(i.value()) + ");");
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
                } else if (op >= IFEQ && op <= IF_ICMPLE) {
                    String cond;
                    if (op <= IFLE) {
                        V v = stack.removeLast();
                        cond = paren(v) + " " + COMPARE[op - IFEQ] + " 0";
                    } else {
                        V b = stack.removeLast(), a = stack.removeLast();
                        cond = paren(a) + " " + COMPARE[op - IF_ICMPEQ] + " " + paren(b);
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
                } else if (op >= IRETURN && op <= DRETURN) {
                    V v = stack.removeLast();
                    lines.add("return " + back(ClassData.returns(m.descriptor()), v.e()) + ";");
                    return true;
                } else if (op == RETURN) {
                    lines.add("return;");
                    return true;
                } else if (op == INVOKESTATIC) {
                    call(i);
                } else {
                    throw new IllegalStateException("not translated: " + Bytecode.name(op));
                }
                return false;
            }

            void call(Insn i) {
                String[] t = pool.member(i.index());
                Call call = new Call(t[0], t[1], t[2]);
                List<String> params = ClassData.parameters(call.descriptor());
                String[] args = new String[params.size()];
                for (int k = params.size() - 1; k >= 0; k--) args[k] = stack.removeLast().e();
                String r = ClassData.returns(call.descriptor());
                String lib = library(call);
                String expr;
                if (lib != null) {
                    expr = lib;
                    for (int k = 0; k < args.length; k++) expr = expr.replace("$" + k, k < args.length && !simpleText(args[k]) ? "(" + args[k] + ")" : args[k]);
                    for (String h : HELPERS.keySet()) if (expr.startsWith(h + "(")) helpers.add(h);
                } else {
                    String name = callees.name(call);
                    if (name == null) throw new IllegalStateException("not translated: " + display(call));
                    List<String> converted = new ArrayList<>();
                    for (int k = 0; k < args.length; k++) converted.add(to(params.get(k), args[k]));
                    expr = name + "(" + String.join(", ", converted) + ")";
                }
                // A call happens where it is: its result goes into a variable at once.
                settle();
                if (r.equals("V")) {
                    lines.add(expr + ";");
                } else {
                    String back = lib != null ? expr : from(r, expr);
                    char st = stackType(r);
                    stack.addLast(new V(st, temp(rust(String.valueOf(st)), back), true));
                }
            }

            void arithmetic(int op) {
                int kind = (op - IADD) / 4, width = (op - IADD) % 4;
                if (op >= 116 && op <= 119) {
                    // neg
                    V a = stack.removeLast();
                    char t = a.t();
                    push(t, t == 'I' || t == 'J' ? paren(a) + ".wrapping_neg()" : "-" + paren(a), false);
                    return;
                }
                if (op >= 120) {
                    // shifts and bitwise: ishl lshl ishr lshr iushr lushr iand land ior lor ixor lxor
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
                String e;
                if (integer) {
                    e = paren(a) + "." + new String[] {"wrapping_add", "wrapping_sub", "wrapping_mul", "wrapping_div", "wrapping_rem"}[kind] + "(" + b.e() + ")";
                } else {
                    e = paren(a) + " " + "+-*/%".charAt(kind) + " " + paren(b);
                }
                if (integer && kind >= 3) {
                    // Division by zero throws where it is in Java: it is done here, not later.
                    settle();
                    stack.addLast(new V(t, temp(rust(String.valueOf(t)), e), true));
                } else {
                    push(t, e, false);
                }
            }

            void convert(int op) {
                V a = stack.removeLast();
                String e = switch (op) {
                    case 133 -> cast(a, "i64");
                    case 134 -> cast(a, "f32");
                    case 135 -> cast(a, "f64");
                    case 136 -> cast(a, "i32");
                    case 137 -> cast(a, "f32");
                    case 138 -> cast(a, "f64");
                    case 139 -> cast(a, "i32");
                    case 140 -> cast(a, "i64");
                    case 141 -> cast(a, "f64");
                    case 142 -> cast(a, "i32");
                    case 143 -> cast(a, "i64");
                    case 144 -> cast(a, "f32");
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
                        if (a.wide()) {
                            top.add(a);
                        } else {
                            top.add(0, a);
                            top.add(0, settled(stack.removeLast()));
                        }
                        // Under it: one wide value or two narrow ones.
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

            /** A value made a variable when it isn't one, so that it can be used twice. */
            V settled(V v) {
                if (v.simple()) return v;
                return new V(v.t(), temp(rust(String.valueOf(v.t())), v.e()), true);
            }

            /**
             * Every value on the stack that reads a variable is made a variable of its own, so that what is
             * written next (a local, a call) can't change it.
             */
            void settle() {
                List<V> all = new ArrayList<>(stack);
                stack.clear();
                for (V v : all) {
                    boolean literal = v.simple() && !v.e().startsWith("l") && !v.e().startsWith("s") && !v.e().startsWith("t");
                    stack.addLast(literal ? v : new V(v.t(), temp(rust(String.valueOf(v.t())), v.e()), true));
                }
            }

            /** The stack handed to the next block, in the stack variables: read all first, then written. */
            void spill() {
                List<V> all = new ArrayList<>(stack);
                exit = all.stream().map(V::t).toList();
                List<String> values = new ArrayList<>();
                for (int d = 0; d < all.size(); d++) {
                    String target = stackVar(d, all.get(d).t());
                    values.add(all.get(d).e().equals(target) ? null : temp(rust(String.valueOf(all.get(d).t())), all.get(d).e()));
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

        static String back(String d, String e) {
            return to(d, e);
        }
    }

    // ---- small things ----

    private static final String[] COMPARE = {"==", "!=", "<", ">=", ">", "<="};

    private static boolean ends(int op) {
        return (op >= IFEQ && op <= RETURN) || op == GOTO_W || op == IFNULL || op == IFNONNULL || op == ATHROW;
    }

    static char stackType(String d) {
        return switch (d) {
            case "J" -> 'J';
            case "F" -> 'F';
            case "D" -> 'D';
            default -> 'I';
        };
    }

    static String rust(String d) {
        return switch (d) {
            case "I" -> "i32";
            case "J" -> "i64";
            case "F" -> "f32";
            case "D" -> "f64";
            case "Z" -> "bool";
            case "B" -> "i8";
            case "C" -> "u16";
            case "S" -> "i16";
            default -> throw new IllegalArgumentException(d);
        };
    }

    private static String zero(char t) {
        return switch (t) {
            case 'J' -> "0i64";
            case 'F' -> "0.0f32";
            case 'D' -> "0.0f64";
            default -> "0i32";
        };
    }

    private static String paren(V v) {
        return v.simple() && !v.e().startsWith("-") ? v.e() : "(" + v.e() + ")";
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
