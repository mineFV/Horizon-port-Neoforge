package horizonneo;

import horizonneo.Bytecode.Insn;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The translator's parts that the differential run (run-test.sh, == translate) doesn't reach on its own. */
final class TranslatorTest {
    public static void main(String[] args) {
        Checks c = new Checks("Translator");

        // A lookupswitch at offset 1: padded to offset 4, its default, two pairs. Then wide iinc, wide iload, goto back.
        byte[] code = {
            (byte) 0x1a,                                     // 0: iload_0
            (byte) 0xab, 0, 0,                               // 1: lookupswitch, padding to 4
            0, 0, 0, 27,                                     // default: 1 + 27 = 28
            0, 0, 0, 2,                                      // two pairs
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x9c, 0, 0, 0, 27,   // -100 -> 28
            0, 0, 0, 7, 0, 0, 0, 27,                         // 7 -> 28
            (byte) 0xc4, (byte) 0x84, 1, 0, (byte) 0xff, (byte) 0xfe,          // 28: wide iinc 256 -2
            (byte) 0xc4, 0x15, 1, 0,                         // 34: wide iload 256
            (byte) 0xa7, (byte) 0xff, (byte) 0xe2,           // 38: goto -30, to 8
        };
        // The goto goes to 8, inside the switch's numbers: still in the code, so it decodes; it is the layout under test.
        List<Insn> insns = Bytecode.decode(code);
        c.same("instructions", 5, insns.size());
        c.same("iload_0 as iload 0", List.of(Bytecode.ILOAD, 0), List.of(insns.get(0).op(), insns.get(0).index()));
        Insn sw = insns.get(1);
        c.same("lookupswitch: default first, then each case", List.of(28, 28, 28), List.of(sw.targets()[0], sw.targets()[1], sw.targets()[2]));
        c.same("its keys", List.of(-100, 7), List.of(sw.keys()[0], sw.keys()[1]));
        c.same("wide iinc: slot and increment", List.of(Bytecode.IINC, 256, -2), List.of(insns.get(2).op(), insns.get(2).index(), insns.get(2).value()));
        c.same("wide iload", List.of(Bytecode.ILOAD, 256), List.of(insns.get(3).op(), insns.get(3).index()));
        c.same("goto: where it lands", 8, insns.get(4).targets()[0]);
        try {
            Bytecode.decode(new byte[] {(byte) 0xa7, 0, 10});
            c.yes("a branch outside the code is refused", false);
        } catch (Bytecode.Invalid e) {
            c.yes("a branch outside the code is refused", e.getMessage().contains("outside the code"));
        }
        try {
            Bytecode.decode(new byte[] {(byte) 0x11, 0});
            c.yes("an instruction cut short is refused", false);
        } catch (Bytecode.Invalid e) {
            c.yes("an instruction cut short is refused", e.getMessage().contains("runs past the end"));
        }
        c.same("names", "if_icmpge", Bytecode.name(162));

        // Names.
        c.same("snake case of a nested class", "math_util_inner", JarRust.snake("MathUtil$Inner"));
        c.same("a run of capitals", "hud_overlay", JarRust.snake("HUDOverlay"));
        c.same("a lambda", "lambda_tick_0", JarRust.snake("lambda$tick$0"));
        c.same("a name that starts with a digit", "m_2_d", JarRust.snake("2D"));
        JarRust.Id a = new JarRust.Id("a/B", "f", "(I)I"), b = new JarRust.Id("a/B", "f", "(J)J"), other = new JarRust.Id("x/B", "g", "()V");
        Map<JarRust.Id, String> names = JarRust.names(Set.of(a, b, other));
        c.same("overloads by their parameters", List.of("b__f__i", "b__f__j", "b__g"), List.of(names.get(a), names.get(b), names.get(other)));
        JarRust.Id same1 = new JarRust.Id("a/B", "h", "()V"), same2 = new JarRust.Id("c/B", "h", "()V");
        Map<JarRust.Id, String> clash = JarRust.names(Set.of(same1, same2));
        c.yes("two classes of one simple name: still two names", !clash.get(same1).equals(clash.get(same2)));

        // Literals that Rust reads back as the same value.
        c.same("int min", "i32::MIN", MethodRust.literal(Integer.MIN_VALUE));
        c.same("a negative long", "(-5i64)", MethodRust.literal(-5L));
        c.same("a float", "1.0e10f32", MethodRust.literal(1e10f));
        c.same("negative zero", "(-0.0f64)", MethodRust.literal(-0.0));
        c.same("NaN by its bits", "f32::from_bits(0x7fc00000)", MethodRust.literal(Float.NaN));

        // The library: only what has an exact counterpart.
        c.same("Math.abs(int) wraps as Java's does", "$0.wrapping_abs()", MethodRust.library(new MethodRust.Call("java/lang/Math", "abs", "(I)I")));
        c.same("Math.sin isn't exact in Rust: none", null, MethodRust.library(new MethodRust.Call("java/lang/Math", "sin", "(D)D")));
        c.same("a reason, without the method's own words", "it takes an object", JarRust.kind("it takes an object (Lfoo/Bar;): objects aren't translated yet"));
        System.exit(c.done());
    }
}
