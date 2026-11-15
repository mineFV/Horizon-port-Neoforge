package java.lang;

/** The model of java.lang.Double: boxes (no cache, as the JDK's), the bits of a value, comparisons with NaN and -0.0 ordered. */
public final class Double extends Number implements Comparable<Double> {
    @SuppressWarnings("unchecked")
    public static final Class<Double> TYPE = (Class<Double>) Class.jPrimitive('D');

    private final double value;

    public Double(double value) {
        this.value = value;
    }

    public static Double valueOf(double d) {
        return new Double(d);
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

    public static boolean isNaN(double v) {
        return v != v;
    }

    public boolean isNaN() {
        return isNaN(value);
    }

    public static boolean isInfinite(double v) {
        return v == 1.0 / 0.0 || v == -1.0 / 0.0;
    }

    public static boolean isFinite(double v) {
        return !isNaN(v) && !isInfinite(v);
    }

    /** Its bits, every NaN as the one canonical NaN. */
    public static native long doubleToLongBits(double value);

    public static native long doubleToRawLongBits(double value);


    public static int compare(double f1, double f2) {
        if (f1 < f2) return -1;
        if (f1 > f2) return 1;
        long a = doubleToLongBits(f1), b = doubleToLongBits(f2);
        return a == b ? 0 : (a < b ? -1 : 1);
    }

    public int compareTo(Double other) {
        return compare(value, other.value);
    }

    public boolean equals(Object obj) {
        return obj instanceof Double other && doubleToLongBits(other.value) == doubleToLongBits(value);
    }

    public static String toString(double d) {
        return String.doubleText(d);
    }

    public String toString() {
        return toString(value);
    }

    public static double sum(double a, double b) {
        return a + b;
    }

    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    public static double min(double a, double b) {
        return Math.min(a, b);
    }

    public static native double longBitsToDouble(long bits);

    public static int hashCode(double value) {
        long bits = doubleToLongBits(value);
        return (int) (bits ^ (bits >>> 32));
    }

    public int hashCode() {
        return hashCode(value);
    }
}
