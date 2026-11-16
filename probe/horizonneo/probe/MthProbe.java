package horizonneo.probe;

import net.minecraft.util.Mth;

/** The game's Mth, as GameProbe observes the rest (see there): pure arithmetic on numbers chosen to pin it down. */
final class MthProbe {
    private MthProbe() {}

    static final double[] D = {0.0, -0.0, 0.5, -0.5, 1.0, -1.0, 1.5, -1.5, 2.0000001, -2.9999999, 179.9, 180.0, 180.1, -180.0,
        -180.1, 359.99, 360.0, 540.0, -540.0, 1e-10, 1e9, -1e9, 2147483646.5, 2147483647.0, 2147483648.0, -2147483648.0,
        -2147483649.5, 1e19, -1e19, 1e300, Double.MIN_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
    static final int[] I = {0, 1, -1, 2, 3, 7, 10, -10, 100, Integer.MAX_VALUE, Integer.MIN_VALUE};

    static void run() {
        p("mth sqrt2", () -> Float.floatToIntBits(Mth.SQRT_OF_TWO));
        for (double a : D) {
            float f = (float) a;
            String k = "mth " + a;
            p(k + " ceil floor", () -> Mth.ceil(a) + " " + Mth.ceil(f) + " " + Mth.floor(a) + " " + Mth.floor(f));
            p(k + " sqrt", () -> Float.floatToIntBits(Mth.sqrt(f)) + " " + Double.doubleToLongBits(Mth.invSqrt(a)));
            p(k + " wrap", () -> Float.floatToIntBits(Mth.wrapDegrees(f)));
            for (double b : D) {
                float g = (float) b;
                String kk = k + " " + b;
                p(kk + " equal", () -> Mth.equal(a, b) + " " + Mth.equal(f, g));
                p(kk + " length", () -> Double.doubleToLongBits(Mth.length(a, b)) + " " + Double.doubleToLongBits(Mth.length(a, b, a - b)));
            }
        }
        double[] some = {0.0, -0.0, 0.25, 0.5, 1.0, 1.5, -1.0, -0.5, 10.0, 359.0, 1e9, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double t : some) {
            for (double a : some) {
                for (double b : some) {
                    float ft = (float) t, fa = (float) a, fb = (float) b;
                    String k = "mth3 " + t + " " + a + " " + b;
                    p(k, () -> Double.doubleToLongBits(Mth.lerp(t, a, b)) + " " + Float.floatToIntBits(Mth.lerp(ft, fa, fb)) + " "
                            + Double.doubleToLongBits(Mth.clamp(t, a, b)) + " " + Float.floatToIntBits(Mth.clamp(ft, fa, fb)) + " "
                            + Float.floatToIntBits(Mth.rotLerp(ft, fa, fb)));
                }
            }
        }
        for (int a : I) {
            for (int b : I) {
                for (int c : I) p("mthi " + a + " " + b + " " + c, () -> Mth.clamp(a, b, c));
                p("mthi " + a + " " + b + " ceilDiv", () -> Mth.positiveCeilDiv(a, b));
                for (double t : some) p("mthi " + a + " " + b + " discrete " + t, () -> Mth.lerpDiscrete((float) t, a, b));
            }
        }
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
