package net.minecraft.util;

/**
 * The stand-in for the game's Mth: what Mekanism uses of it, observed (`standins`, probe/MthProbe.java) on numbers
 * chosen to pin it down (zeros of both signs, halves, the ints' bounds, NaN, infinities). Its sine and cosine read the
 * game's table (HorizonSine) as Horizon does; its atan2 isn't here: the game reads it from tables built with
 * Math.asin and Math.cos, whose last bit Rust's functions don't promise.
 */
public class Mth {
    public static final float SQRT_OF_TWO = (float) Math.sqrt(2.0);

    public Mth() {}

    /**
     * The game's table at the angle's index: the angle times 10430.378 (65536 / 2π) in floats, cut to an int, its low 16
     * bits. Observed (`standins`): every entry, the angles on either side of every entry's edge, past the ints' range, NaN
     * and the infinities. Not Horizon's (Minecraft 26.3's, in doubles cut to a long), which picks other entries there.
     */
    public static float sin(float value) {
        return HorizonSine.TABLE[(int) (value * 10430.378F) & 65535];
    }

    /** The sine a quarter turn on: the index plus 16384, added in floats too. */
    public static float cos(float value) {
        return HorizonSine.TABLE[(int) (value * 10430.378F + 16384.0F) & 65535];
    }

    /** Observed: the int cast, one less when the cast went up (below MIN it wraps to MAX). */
    /** Observed (`standins`): start, plus the floor of delta times the distance. */
    public static int lerpInt(float delta, int start, int end) {
        return start + floor(delta * (end - start));
    }

    public static int floor(float f) {
        int i = (int) f;
        return f < (float) i ? i - 1 : i;
    }

    public static int floor(double d) {
        int i = (int) d;
        return d < (double) i ? i - 1 : i;
    }

    /** Observed: the int cast, one more when the cast went down (above MAX it wraps to MIN). */
    public static int ceil(float f) {
        int i = (int) f;
        return f > (float) i ? i + 1 : i;
    }

    public static int ceil(double d) {
        int i = (int) d;
        return d > (double) i ? i + 1 : i;
    }

    /** Observed: the maximum with min first, then the minimum with max (when they cross, max wins). */
    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    /** Observed: min when the value is below it, else the smaller of the value and max (NaN stays NaN). */
    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    /** Observed: within 1.0E-5 (a float), as a double or a float. */
    public static boolean equal(float a, float b) {
        return Math.abs(b - a) < 1.0E-5F;
    }

    public static boolean equal(double a, double b) {
        return Math.abs(b - a) < 1.0E-5F;
    }

    public static float sqrt(float f) {
        return (float) Math.sqrt(f);
    }

    public static double invSqrt(double d) {
        return 1.0 / Math.sqrt(d);
    }

    public static double length(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    public static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    public static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    /** Observed: the remainder by 360, brought into [-180, 180). */
    public static float wrapDegrees(float degrees) {
        float f = degrees % 360.0F;
        if (f >= 180.0F) f -= 360.0F;
        if (f < -180.0F) f += 360.0F;
        return f;
    }

    /** Observed: a lerp along the shorter way round, by wrapDegrees of the difference. */
    public static float rotLerp(float t, float a, float b) {
        return a + t * wrapDegrees(b - a);
    }

    /** Observed: a / b rounded up, as the negated floor division of -a by b. */
    public static int positiveCeilDiv(int a, int b) {
        return -Math.floorDiv(-a, b);
    }

    /**
     * Fitted to the game's answers (1694 of them, the ints' bounds included): from a, the floor of t times one step
     * less than b - a (in floats), plus one when t is above zero.
     */
    public static int lerpDiscrete(float t, int a, int b) {
        int span = b - a;
        return a + floor(t * (float) (span - 1)) + (t > 0.0F ? 1 : 0);
    }
}
