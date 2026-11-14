package java.lang;

/** The model of java.lang.Float: boxes (no cache, as the JDK's), the bits of a value, comparisons with NaN and -0.0 ordered. */
public final class Float extends Number implements Comparable<Float> {
    private final float value;

    public Float(float value) {
        this.value = value;
    }

    public static Float valueOf(float d) {
        return new Float(d);
    }

    public int intValue() {
        return (int) value;
    }

    public long longValue() {
        return (long) value;
    }

    public float floatValue() {
        return (float) value;
    }

    public double doubleValue() {
        return (double) value;
    }

    public static boolean isNaN(float v) {
        return v != v;
    }

    public boolean isNaN() {
        return isNaN(value);
    }

    public static boolean isInfinite(float v) {
        return v == 1.0 / 0.0 || v == -1.0 / 0.0;
    }

    public static boolean isFinite(float v) {
        return !isNaN(v) && !isInfinite(v);
    }

    /** Its bits, every NaN as the one canonical NaN. */
    public static native int floatToIntBits(float value);

    public static native int floatToRawIntBits(float value);


    public static int compare(float f1, float f2) {
        if (f1 < f2) return -1;
        if (f1 > f2) return 1;
        int a = floatToIntBits(f1), b = floatToIntBits(f2);
        return a == b ? 0 : (a < b ? -1 : 1);
    }

    public int compareTo(Float other) {
        return compare(value, other.value);
    }

    public boolean equals(Object obj) {
        return obj instanceof Float other && floatToIntBits(other.value) == floatToIntBits(value);
    }

    public static String toString(float d) {
        return String.floatText(d);
    }

    public String toString() {
        return toString(value);
    }

    public static float sum(float a, float b) {
        return a + b;
    }

    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    public static float min(float a, float b) {
        return Math.min(a, b);
    }

    public static native float intBitsToFloat(int bits);

    public static int hashCode(float value) {
        return floatToIntBits(value);
    }

    public int hashCode() {
        return hashCode(value);
    }
}
