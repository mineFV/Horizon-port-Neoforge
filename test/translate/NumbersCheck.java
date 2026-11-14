import com.example.numbers.Numbers;
import com.example.numbers.Shapes;

/** Runs `Numbers` on the inputs `numbers_check.rs` gives its translation, and prints the results the same way. */
public final class NumbersCheck {
    static final int[] INTS = {0, 1, -1, 2, 7, 10, 11, -100, 1000000, 12345, -98765, Integer.MAX_VALUE, Integer.MIN_VALUE};
    static final long[] LONGS = {0, 1, -1, 27, 97, 1L << 40, -(1L << 50) + 3, Long.MAX_VALUE, Long.MIN_VALUE};
    static final float[] FLOATS = {0f, -0f, 1.5f, -1.5f, 2.5f, -2.5f, 0.49999997f, 3.4e38f, -7.25f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1e-40f};
    static final double[] DOUBLES = {0d, -0d, 1.5, -2.5, 0.49999999999999994, 1e300, -3.75, 4.5e15, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 9.2e18, -9.3e18};

    public static void main(String[] args) {
        for (int a : INTS) {
            for (int b : INTS) {
                p("mix", a + " " + b, Numbers.mix(a, b));
                p("div", a + " " + b, Numbers.div(a, b));
                if (b != 0) p("floorMod", a + " " + b, Numbers.floorMod(a, b));
            }
            for (int s : new int[] {0, 1, 5, 31, 32, 33, 63, 64, 65, -1}) {
                p("shl", a + " " + s, Numbers.shl(a, s));
            }
            p("neg", a, Numbers.neg(a));
            p("sign", a, Numbers.sign(a));
            p("incBoth", a, Numbers.incBoth(a));
            p("days", a, Numbers.days(a));
            p("sparse", a, Numbers.sparse(a));
            p("toByte", a, Numbers.toByte(a));
            p("toChar", a, (int) Numbers.toChar(a));
            p("toShort", a, Numbers.toShort(a));
            p("iabs", a, Numbers.iabs(a));
            p("twice", a, Numbers.twice(a));
            p("pick", a, Numbers.pick(a > 0, a, -a));
            p("isDigit", a, Numbers.isDigit((char) (a & 0xFFFF)));
        }
        for (int m = 0; m <= 13; m++) p("days", m, Numbers.days(m));
        for (int n = 0; n <= 25; n++) {
            p("sumTo", n, Numbers.sumTo(n * 1000));
            p("fact", n, Numbers.fact(n));
        }
        for (int c = 40; c <= 60; c++) p("isDigit", c, Numbers.isDigit((char) c));
        for (long a : LONGS) {
            for (int s : new int[] {0, 1, 13, 63, 64, 70}) {
                p("lmix", a + " " + s, Numbers.lmix(a, s));
                p("ushr", a + " " + s, Numbers.ushr(a, s));
            }
            for (long b : LONGS) {
                p("cmpL", a + " " + b, Numbers.cmpL(a, b));
                if (b != 0) p("floorDiv", a + " " + b, Numbers.floorDiv(a, b));
            }
            p("collatz", a, Numbers.collatz(a));
            p("l2f", a, Float.floatToIntBits(Numbers.l2f(a)));
        }
        for (float a : FLOATS) {
            for (float b : FLOATS) {
                p("less", f(a) + " " + f(b), Numbers.less(a, b));
                p("frem", f(a) + " " + f(b), Float.floatToIntBits(Numbers.frem(a, b)));
                p("fmin", f(a) + " " + f(b), Float.floatToIntBits(Numbers.fmin(a, b)));
            }
            p("f2i", f(a), Numbers.f2i(a));
            p("roundF", f(a), Numbers.roundF(a));
        }
        for (double a : DOUBLES) {
            for (double b : DOUBLES) {
                p("atLeast", d(a) + " " + d(b), Numbers.atLeast(a, b));
                p("clamp", d(a) + " " + d(b), Double.doubleToLongBits(Numbers.clamp(a, b, 10.0)));
            }
            p("poly", d(a), Double.doubleToLongBits(Numbers.poly(a)));
            p("d2l", d(a), Numbers.d2l(a));
            p("roundD", d(a), Numbers.roundD(a));
            p("sig", d(a), Double.doubleToLongBits(Numbers.sig(a)));
        }
        // Objects: built and answered with numbers inside Shapes.
        for (int n = 0; n <= 12; n++) {
            p("areas", n, Shapes.areas(n));
            p("perimeters", n, Shapes.perimeters(n));
            p("made", n, Shapes.made(n));
            p("list", n, Shapes.list(n));
            p("statics", n, Shapes.statics(n));
            p("lambdas", n, Shapes.lambdas(n));
            p("casts", n, Shapes.casts(n));
            p("arrays", n, Shapes.arrays(n));
            p("same", n, Shapes.same(n));
        }
    }

    static String f(float x) {
        return Integer.toHexString(Float.floatToIntBits(x));
    }

    static String d(double x) {
        return Long.toHexString(Double.doubleToLongBits(x));
    }

    static void p(String name, Object args, Object result) {
        System.out.println(name + " " + args + " -> " + result);
    }
}
