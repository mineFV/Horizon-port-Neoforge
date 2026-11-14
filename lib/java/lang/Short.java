package java.lang;

/** The model of java.lang.Short: boxes (valueOf's cache from -128 to 127 included). */
public final class Short extends Number implements Comparable<Short> {
    private final short value;

    public Short(short value) {
        this.value = value;
    }

    /** The objects valueOf gives from -128 to 127: always the same ones, as the JDK's cache. */
    private static final Short[] CACHE = new Short[127 - (-128) + 1];

    static {
        for (int i = 0; i < CACHE.length; i++) CACHE[i] = new Short((short) (i + (-128)));
    }

    public static Short valueOf(short i) {
        if (i >= -128 && i <= 127) return CACHE[(int) i - (-128)];
        return new Short(i);
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

    public static int hashCode(short value) {
        return (int) value;
    }

    public int hashCode() {
        return hashCode(value);
    }

    public boolean equals(Object obj) {
        return obj instanceof Short other && value == other.value;
    }

    /** As the JDK specifies it for Short: the difference. */
    public static int compare(short x, short y) {
        return x - y;
    }

    public int compareTo(Short other) {
        return compare(value, other.value);
    }

    public String toString() {
        return Long.toString(value);
    }

    public static String toString(short s) {
        return Long.toString(s);
    }
}
