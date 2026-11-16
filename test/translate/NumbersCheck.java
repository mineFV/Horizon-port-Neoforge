import com.example.numbers.Boxes;
import com.example.numbers.Gathered;
import com.example.numbers.Numbers;
import com.example.numbers.Places;
import com.example.numbers.Shapes;
import com.example.numbers.Texts;

/** Runs `Numbers` on the inputs `numbers_check.rs` gives its translation, and prints the results the same way. */
public final class NumbersCheck {
    static final int[] INTS = {0, 1, -1, 2, 7, 10, 11, -100, 1000000, 12345, -98765, Integer.MAX_VALUE, Integer.MIN_VALUE};
    static final long[] LONGS = {0, 1, -1, 27, 97, 1L << 40, -(1L << 50) + 3, Long.MAX_VALUE, Long.MIN_VALUE};
    static final float[] FLOATS = {0f, -0f, 1.5f, -1.5f, 2.5f, -2.5f, 0.49999997f, 3.4e38f, -7.25f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1e-40f};
    static final double[] DOUBLES = {0d, -0d, 1.5, -2.5, 0.49999999999999994, 1e300, -3.75, 4.5e15, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 9.2e18, -9.3e18};

    public static void main(String[] args) {
        for (int a : INTS) {
            for (int b : INTS) {
                p("mix", a + " " + b, () -> Numbers.mix(a, b));
                p("div", a + " " + b, () -> Numbers.div(a, b));
                if (b != 0) p("floorMod", a + " " + b, () -> Numbers.floorMod(a, b));
            }
            for (int s : new int[] {0, 1, 5, 31, 32, 33, 63, 64, 65, -1}) {
                p("shl", a + " " + s, () -> Numbers.shl(a, s));
            }
            p("neg", a, () -> Numbers.neg(a));
            p("sign", a, () -> Numbers.sign(a));
            p("incBoth", a, () -> Numbers.incBoth(a));
            p("days", a, () -> Numbers.days(a));
            p("sparse", a, () -> Numbers.sparse(a));
            p("toByte", a, () -> Numbers.toByte(a));
            p("toChar", a, () -> (int) Numbers.toChar(a));
            p("toShort", a, () -> Numbers.toShort(a));
            p("iabs", a, () -> Numbers.iabs(a));
            p("twice", a, () -> Numbers.twice(a));
            p("pick", a, () -> Numbers.pick(a > 0, a, -a));
            p("isDigit", a, () -> Numbers.isDigit((char) (a & 0xFFFF)));
        }
        for (int m : range(0, 13)) p("days", m, () -> Numbers.days(m));
        for (int n : range(0, 25)) {
            p("sumTo", n, () -> Numbers.sumTo(n * 1000));
            p("fact", n, () -> Numbers.fact(n));
        }
        for (int c : range(40, 60)) p("isDigit", c, () -> Numbers.isDigit((char) c));
        for (long a : LONGS) {
            for (int s : new int[] {0, 1, 13, 63, 64, 70}) {
                p("lmix", a + " " + s, () -> Numbers.lmix(a, s));
                p("ushr", a + " " + s, () -> Numbers.ushr(a, s));
            }
            for (long b : LONGS) {
                p("cmpL", a + " " + b, () -> Numbers.cmpL(a, b));
                if (b != 0) p("floorDiv", a + " " + b, () -> Numbers.floorDiv(a, b));
            }
            p("collatz", a, () -> Numbers.collatz(a));
            p("l2f", a, () -> Float.floatToIntBits(Numbers.l2f(a)));
        }
        for (float a : FLOATS) {
            for (float b : FLOATS) {
                p("less", f(a) + " " + f(b), () -> Numbers.less(a, b));
                p("frem", f(a) + " " + f(b), () -> Float.floatToIntBits(Numbers.frem(a, b)));
                p("fmin", f(a) + " " + f(b), () -> Float.floatToIntBits(Numbers.fmin(a, b)));
            }
            p("f2i", f(a), () -> Numbers.f2i(a));
            p("roundF", f(a), () -> Numbers.roundF(a));
        }
        for (double a : DOUBLES) {
            for (double b : DOUBLES) {
                p("atLeast", d(a) + " " + d(b), () -> Numbers.atLeast(a, b));
                p("clamp", d(a) + " " + d(b), () -> Double.doubleToLongBits(Numbers.clamp(a, b, 10.0)));
            }
            p("poly", d(a), () -> Double.doubleToLongBits(Numbers.poly(a)));
            p("d2l", d(a), () -> Numbers.d2l(a));
            p("roundD", d(a), () -> Numbers.roundD(a));
            p("sig", d(a), () -> Double.doubleToLongBits(Numbers.sig(a)));
        }
        // Objects: built and answered with numbers inside Shapes.
        for (int n : range(0, 12)) {
            p("areas", n, () -> Shapes.areas(n));
            p("perimeters", n, () -> Shapes.perimeters(n));
            p("made", n, () -> Shapes.made(n));
            p("list", n, () -> Shapes.list(n));
            p("statics", n, () -> Shapes.statics(n));
            p("lambdas", n, () -> Shapes.lambdas(n));
            p("casts", n, () -> Shapes.casts(n));
            p("arrays", n, () -> Shapes.arrays(n));
            p("same", n, () -> Shapes.same(n));
            p("callNever", n, () -> Shapes.callNever(null, n));
        }
        // The game's positions and directions, through the translator's stand-ins (game/).
        for (int a : INTS) {
            p("packed", a, () -> Places.packed(a, a >> 1, -a));
            p("eastX", a, () -> Places.eastX(a));
            for (int b : INTS) p("compare", a + " " + b, () -> Places.compare(a, b));
        }
        // Horizon's own test of the packed form (position.rs, packed_signed_limits_and_java_overflow).
        p("packed", "limits", () -> Places.packed(-33554432, -2048, 33554431));
        for (long l : LONGS) {
            p("unpacked", l, () -> Places.unpackedX(l) + " " + Places.unpackedY(l) + " " + Places.unpackedZ(l));
            for (int d : range(0, 7)) p("walk", l + " " + d, () -> Places.walk(l, d));
        }
        for (int n : range(0, 12)) p("keys", n, () -> Places.keys(n));
        for (int d : range(0, 5)) {
            p("opposite", d, () -> Places.opposite(d));
            p("steps", d, () -> Places.steps(d));
            p("nameLength", d, () -> Places.nameLength(d));
            p("sorted", d, () -> Places.sorted(d));
        }
        // Exceptions and texts.
        for (int n : range(0, 12)) {
            p("divide", n, () -> Texts.divide(10, n - 3));
            p("finallyCounts", n, () -> Texts.finallyCounts(n));
            p("finallyReturns", n, () -> Texts.finallyReturns(n));
            p("jvm", n, () -> Texts.jvm(n));
            p("uncaught", n, () -> Texts.uncaught(n));
            p("causes", n, () -> Texts.causes(n));
            p("initializers", n, () -> Texts.initializers(n));
            p("rethrow", n, () -> Texts.rethrow(n));
            p("join", n, () -> Texts.join(n));
            p("literals", n, () -> Texts.literals(n));
            p("methods", n, () -> Texts.methods(n));
            p("outOfBounds", n, () -> Texts.outOfBounds(n - 2));
        }
        // Records, enums, boxes, Objects, java.util.function; collections and Optional.
        for (int n : range(0, 12)) {
            p("records", n, () -> Boxes.records(n));
            p("enums", n, () -> Boxes.enums(n));
            p("boxes", n, () -> Boxes.boxes(n));
            p("bits", n, () -> Boxes.bits(n));
            p("objects", n, () -> Boxes.objects(n));
            p("functions", n, () -> Boxes.functions(n));
            p("lists", n, () -> Gathered.lists(n));
            p("maps", n, () -> Gathered.maps(n));
            p("sets", n, () -> Gathered.sets(n));
            p("immutables", n, () -> Gathered.immutables(n));
            p("optionals", n, () -> Gathered.optionals(n));
            p("arrayHelpers", n, () -> Gathered.arrays(n));
            p("builders", n, () -> Gathered.builders(n));
            p("classes", n, () -> Gathered.classes(n));
            p("enumCollections", n, () -> Gathered.enumCollections(n));
            p("streams", n, () -> Gathered.streams(n));
            p("misc", n, () -> Gathered.misc(n));
            p("patterns", n, () -> Gathered.patterns(n));
            p("monitors", n, () -> Gathered.monitors(n));
            p("queues", n, () -> Gathered.queues(n));
            p("bytes", n, () -> Gathered.bytes(n));
            p("randoms", n, () -> Gathered.randoms(n));
            p("trees", n, () -> Gathered.trees(n));
            p("threads", n, () -> Gathered.threads(n));
            p("executors", n, () -> Gathered.executors(n));
            p("unicode", n, () -> Gathered.unicode(n));
            p("concurrentMaps", n, () -> Gathered.concurrentMaps(n));
            p("futures", n, () -> Gathered.futures(n));
            p("extras", n, () -> Gathered.extras(n));
            p("splits", n, () -> Gathered.splits(n));
        }
        for (double d : DOUBLES) p("doubleText", d(d), () -> Texts.doubleText(d));
        for (float f : FLOATS) p("floatText", f(f), () -> Texts.floatText(f));
        double[] edges = {1e-3, 9.999999999999998e-4, 1e7, 9999999.999999998, 1e-323, 4.9e-324, Double.MAX_VALUE, Double.MIN_NORMAL, 0.1, 0.3, 2e22, 1.0E23, 5e-324};
        for (double d : edges) p("doubleText", d(d), () -> Texts.doubleText(d));
        // The same random values on both sides: a 64-bit linear congruential generator, read as doubles and floats.
        long x = 0x2545F4914F6CDD1DL;
        for (int i = 0; i < 3000; i++) {
            x = x * 6364136223846793005L + 1442695040888963407L;
            double d = Double.longBitsToDouble(x);
            float f = Float.intBitsToFloat((int) (x >>> 32));
            p("doubleText", d(d), () -> Texts.doubleText(d));
            p("floatText", f(f), () -> Texts.floatText(f));
        }
    }

    static int[] range(int from, int to) {
        int[] out = new int[to - from + 1];
        for (int i = 0; i < out.length; i++) out[i] = from + i;
        return out;
    }

    static String f(float x) {
        return Integer.toHexString(Float.floatToIntBits(x));
    }

    static String d(double x) {
        return Long.toHexString(Double.doubleToLongBits(x));
    }

    /** A call whose result is printed, or what it throws. */
    interface Call {
        Object run() throws Throwable;
    }

    /**
     * The result, or "throws" and the exception: its class, and its message but for a NullPointerException or a
     * ClassCastException, whose messages the translation doesn't reproduce (lib/README.md).
     */
    static void p(String name, Object args, Call call) {
        String shown;
        try {
            shown = String.valueOf(call.run());
        } catch (Throwable t) {
            boolean bare = t instanceof NullPointerException || t instanceof ClassCastException || t.getMessage() == null;
            shown = "throws " + t.getClass().getName() + (bare ? "" : ": " + t.getMessage());
        }
        System.out.println(name + " " + args + " -> " + shown);
    }
}
