package net.minecraft.util;

/**
 * Not the game's: the game's sine table, which Mth.sin and Mth.cos read, made when first asked. Entry i is the sine of
 * i / 10430.378350470453 (65536 / 2π) rounded to a double, then to a float, as Horizon builds it for Minecraft 26.3
 * (horizon_core::geometry::trig, MIT), the same table as 1.21.1's (observed). The sine is Horizon's correctly rounded one (horizon_core::jmath::exact, MIT),
 * in Java's double arithmetic: the same on the JVM and translated. HotSpot's Math.sin, which the game's table is built
 * with, rounds a few arguments in a thousand otherwise, by one bit of a double, which a float doesn't keep: observed
 * equal to the game's table, all 65536 entries (`standins`, probe/MthProbe.java).
 */
final class HorizonSine {
    private HorizonSine() {}

    /** 65536 / 2π, rounded to a double. */
    static final double SCALE = 10430.378350470453;

    private static float[] table() {
        float[] t = new float[65536];
        for (int i = 0; i < t.length; i++) t[i] = (float) sin(i / SCALE);
        return t;
    }

    /** A value hi + lo, with |lo| at most half an ulp of hi. */
    private record Dd(double hi, double lo) {}

    /** a + b exactly, as a sum and its rounding error (Knuth). */
    private static Dd twoSum(double a, double b) {
        double s = a + b;
        double v = s - a;
        return new Dd(s, (a - (s - v)) + (b - v));
    }

    /** a + b exactly when |a| >= |b| (Dekker). */
    private static Dd fastTwoSum(double a, double b) {
        double s = a + b;
        return new Dd(s, b - (s - a));
    }

    /** a * b exactly, as a product and its rounding error (Dekker's split, no fused multiply-add). */
    private static Dd twoProd(double a, double b) {
        double p = a * b;
        double t = 134217729.0 * a; // 2^27 + 1
        double ah = t - (t - a), al = a - ah;
        t = 134217729.0 * b;
        double bh = t - (t - b), bl = b - bh;
        return new Dd(p, ((ah * bh - p) + ah * bl + al * bh) + al * bl);
    }

    private static Dd add(Dd a, Dd b) {
        Dd s = twoSum(a.hi, b.hi);
        Dd t = twoSum(a.lo, b.lo);
        s = fastTwoSum(s.hi, s.lo + t.hi);
        return fastTwoSum(s.hi, s.lo + t.lo);
    }

    private static Dd mul(Dd a, Dd b) {
        Dd p = twoProd(a.hi, b.hi);
        return fastTwoSum(p.hi, p.lo + (a.hi * b.lo + a.lo * b.hi));
    }

    /** Divided by a small whole number (exact in a double). */
    private static Dd div(Dd a, double d) {
        double q = a.hi / d;
        Dd p = twoProd(q, d);
        double r = ((a.hi - p.hi) - p.lo + a.lo) / d;
        return fastTwoSum(q, r);
    }

    private static Dd neg(Dd a) {
        return new Dd(-a.hi, -a.lo);
    }

    /** 2/π, rounded to a double. */
    private static final double FRAC_2_PI = Double.longBitsToDouble(0x3FE45F306DC9C883L);

    /** π/2 = P1 + P2 + P3 + P4 to 2^-160: the first three have 33 significant bits, so k Pn is exact for |k| < 2^20. */
    private static final double P1 = Double.longBitsToDouble(0x3FF921FB54400000L);
    private static final double P2 = Double.longBitsToDouble(0x3DD0B4611A600000L);
    private static final double P3 = Double.longBitsToDouble(0x3BA3198A2E000000L);
    private static final double P4 = Double.longBitsToDouble(0x397B839A252049C1L);

    /** x - k π/2 for the k closest to x / (π/2) (half away from zero), for 0 <= |x| <= 2^20. */
    private static Dd reduce(double x, double k) {
        return add(add(add(new Dd(x - k * P1, 0.0), new Dd(-(k * P2), 0.0)), new Dd(-(k * P3), 0.0)), neg(twoProd(k, P4)));
    }

    /** The sum of xs rounded once, exactly (Shewchuk's partials, with the half-way fix-up a running sum can't see). */
    private static double exactSum(double[] xs, int count) {
        double[] partials = new double[64];
        int len = 0;
        for (int m = 0; m < count; m++) {
            double x = xs[m];
            int i = 0;
            for (int j = 0; j < len; j++) {
                double y = partials[j];
                if (Math.abs(x) < Math.abs(y)) {
                    double swap = x;
                    x = y;
                    y = swap;
                }
                double hi = x + y;
                double lo = y - (hi - x);
                if (lo != 0.0) partials[i++] = lo;
                x = hi;
            }
            partials[i] = x;
            len = i + 1;
        }
        if (len == 0) return 0.0;
        int n = len - 1;
        double hi = partials[n];
        double lo = 0.0;
        while (n > 0) {
            double x = hi;
            n--;
            double y = partials[n];
            hi = x + y;
            lo = y - (hi - x);
            if (lo != 0.0) break;
        }
        if (n > 0 && ((lo < 0.0 && partials[n - 1] < 0.0) || (lo > 0.0 && partials[n - 1] > 0.0))) {
            double y = lo * 2.0;
            double x = hi + y;
            if (y == x - hi) hi = x;
        }
        return hi;
    }

    /**
     * sin r or cos r for |r| <= π/4 and a little: Taylor's series to below 2^-110, each term a double-double, the terms
     * summed exactly.
     */
    private static double series(Dd r, boolean cos) {
        Dd r2 = mul(r, r);
        double[] parts = new double[32];
        Dd term = cos ? new Dd(1.0, 0.0) : r;
        double first = cos ? 1.0 : 2.0;
        parts[0] = term.hi;
        parts[1] = term.lo;
        int len = 2;
        for (int n = 1; n <= 14; n++) {
            double k = 2.0 * n + first - 1.0;
            term = neg(div(mul(term, r2), k * (k - 1.0)));
            parts[len] = term.hi;
            parts[len + 1] = term.lo;
            len += 2;
        }
        return exactSum(parts, len);
    }

    /** Taylor's coefficients 1/n!, signs alternating, for the tails in doubles: sin's from r^7, cos's from r^8. */
    private static final double[] SIN_TAIL = {-1.0 / 5040.0, 1.0 / 362880.0, -1.0 / 39916800.0, 1.0 / 6227020800.0, -1.0 / 1307674368000.0, 1.0 / 355687428096000.0,
            -1.0 / 121645100408832000.0, 1.0 / 51090942171709440000.0, -1.0 / 25852016738884976640000.0};
    private static final double[] COS_TAIL = {1.0 / 40320.0, -1.0 / 3628800.0, 1.0 / 479001600.0, -1.0 / 87178291200.0, 1.0 / 20922789888000.0, -1.0 / 6402373705728000.0,
            1.0 / 2432902008176640000.0, -1.0 / 1124000727777607680000.0};

    private static double horner(double[] c, double x) {
        double acc = 0.0;
        for (int i = c.length - 1; i >= 0; i--) acc = acc * x + c[i];
        return acc;
    }

    /** sin r or cos r to about 2^-64: the first terms as double-doubles, the small rest in doubles. */
    private static Dd fast(Dd r, boolean cos) {
        Dd r2 = mul(r, r);
        double x2 = r2.hi;
        if (cos) {
            Dd t2 = neg(div(r2, 2.0));
            Dd t4 = div(mul(r2, r2), 24.0);
            Dd t6 = neg(div(mul(t4, r2), 30.0));
            double tail = x2 * x2 * x2 * x2 * horner(COS_TAIL, x2);
            return add(add(add(add(new Dd(1.0, 0.0), t2), t4), t6), new Dd(tail, 0.0));
        }
        Dd t3 = neg(div(mul(r2, r), 6.0));
        Dd t5 = neg(div(mul(t3, r2), 20.0));
        double tail = r.hi * x2 * x2 * x2 * horner(SIN_TAIL, x2);
        return add(add(add(r, t3), t5), new Dd(tail, 0.0));
    }

    /** sin x rounded to the closest double, for 0 <= x <= 2^20 (the table's arguments). */
    static double sin(double x) {
        if (x < Double.longBitsToDouble(0x3E40000000000000L)) {
            // |x| < 2^-27: sin x rounds to x (0 included).
            if (x >= 0) return x;
        }
        if (!(x >= 0 && x <= 1048576.0)) throw new IllegalArgumentException("the sine table's argument isn't in [0, 2^20]: " + x);
        double k = (double) Math.round(x * FRAC_2_PI);
        Dd r = reduce(x, k);
        // sin x is sin r, cos r, -sin r, -cos r in quadrants 0 to 3.
        int q = (int) ((long) k & 3);
        boolean wantCos = (q & 1) == 1;
        Dd v = fast(r, wantCos);
        double e = Math.abs(v.hi) * Double.longBitsToDouble(0x3C30000000000000L); // 2^-60
        double a = v.hi + (v.lo - e), b = v.hi + (v.lo + e);
        double s = a == b ? a : series(r, wantCos);
        return q >= 2 ? -s : s;
    }

    /** Made last: a class's initializer sets its statics in order, and the table needs the constants above. */
    static final float[] TABLE = table();
}
