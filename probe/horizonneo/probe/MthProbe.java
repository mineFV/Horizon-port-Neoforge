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
        sines();
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

    /** 65536 / 2π, rounded to a double: the table's entries are a turn's 65536ths. */
    static final double SCALE = 10430.378350470453;

    /** A hash of the sine's and cosine's bits over the angles. */
    static String hashed(float[] angles) {
        int hs = 1, hc = 1;
        for (float f : angles) {
            hs = 31 * hs + Float.floatToRawIntBits(Mth.sin(f));
            hc = 31 * hc + Float.floatToRawIntBits(Mth.cos(f));
        }
        return hs + " " + hc;
    }

    /**
     * The game's sine table and how an angle picks its entry: every entry by the angle at its middle (in blocks of 4096,
     * hashed), the angles on either side of the entries' edges, angles past the ints' range, NaN, infinities, and floats
     * at random, of any bits.
     */
    static void sines() {
        for (int block = 0; block < 16; block++) {
            int from = block * 4096;
            p("mth sine table " + from, () -> {
                float[] a = new float[4096];
                for (int i = 0; i < a.length; i++) a[i] = (float) ((from + i + 0.5) / SCALE);
                return hashed(a);
            });
        }
        for (int block = 0; block < 16; block++) {
            int from = block * 4096;
            p("mth sine edges " + from, () -> {
                float[] a = new float[3 * 4096];
                for (int i = 0; i < 4096; i++) {
                    float f = (float) ((from + i) / SCALE);
                    a[3 * i] = Math.nextDown(f);
                    a[3 * i + 1] = f;
                    a[3 * i + 2] = Math.nextUp(f);
                }
                return hashed(a);
            });
        }
        float[] extremes = {0f, -0f, 1e-30f, Float.MIN_VALUE, -Float.MIN_VALUE, 1f, -1f, (float) Math.PI, (float) -Math.PI, (float) (Math.PI / 2), (float) (2 * Math.PI), 100f, -100f,
            1e4f, 2e5f, 205887.4f, 205888f, -205888f, 3e5f, -3e5f, 1e6f, 1e7f, 1e9f, -1e9f, 8.8e14f, 1e15f, -1e15f, 1e18f, 1e20f, Float.MAX_VALUE, -Float.MAX_VALUE, Float.NaN,
            Float.intBitsToFloat(0x7f800001), Float.intBitsToFloat(0xffc00000), Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};
        for (float f : extremes) {
            p("mth sine of " + Float.floatToRawIntBits(f), () -> Float.floatToRawIntBits(Mth.sin(f)) + " " + Float.floatToRawIntBits(Mth.cos(f)));
        }
        float[] ranges = {1f, 10f, 1e3f, 1e5f, 1e7f};
        for (float range : ranges) {
            p("mth sine random " + range, () -> {
                java.util.Random r = new java.util.Random(Float.floatToRawIntBits(range));
                float[] a = new float[100000];
                for (int i = 0; i < a.length; i++) a[i] = (r.nextFloat() * 2 - 1) * range;
                return hashed(a);
            });
        }
        p("mth sine any bits", () -> {
            java.util.Random r = new java.util.Random(7);
            float[] a = new float[200000];
            for (int i = 0; i < a.length; i++) a[i] = Float.intBitsToFloat(r.nextInt());
            return hashed(a);
        });
        net.minecraft.world.phys.Vec3[] vectors = {new net.minecraft.world.phys.Vec3(1, 2, 3), new net.minecraft.world.phys.Vec3(-0.5, 0, 0.25),
            new net.minecraft.world.phys.Vec3(1e10, -1e-10, 0), new net.minecraft.world.phys.Vec3(0.0625, 0.9375, -0.0), new net.minecraft.world.phys.Vec3(Double.NaN, 1, Double.POSITIVE_INFINITY)};
        float[] angles = {0f, -0f, (float) (Math.PI / 2), (float) Math.PI, (float) (-Math.PI / 2), (float) (3 * Math.PI / 2), (float) Math.toRadians(90), (float) Math.toRadians(-270), 1f, 0.1f,
            123.456f, -7.5f, 1e9f, Float.NaN};
        for (int v = 0; v < vectors.length; v++) {
            net.minecraft.world.phys.Vec3 vec = vectors[v];
            for (float angle : angles) {
                p("vec rot " + v + " " + Float.floatToRawIntBits(angle), () -> vec.xRot(angle) + " " + vec.yRot(angle) + " " + vec.zRot(angle));
            }
        }
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
