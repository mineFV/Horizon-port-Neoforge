package java.lang;

/** The model of java.lang.Integer: boxes (valueOf's cache from -128 to 127 included) and texts. parseInt isn't modelled yet. */
public final class Integer extends Number implements Comparable<Integer> {
    @SuppressWarnings("unchecked")
    public static final Class<Integer> TYPE = (Class<Integer>) Class.jPrimitive('I');

    public static final int MIN_VALUE = 0x80000000;
    public static final int MAX_VALUE = 0x7fffffff;
    public static final int SIZE = 32;
    public static final int BYTES = 4;

    private final int value;

    public Integer(int value) {
        this.value = value;
    }

    /** The objects valueOf gives from -128 to 127: always the same ones, as the JDK's cache. */
    private static final Integer[] CACHE = new Integer[127 - (-128) + 1];

    static {
        for (int i = 0; i < CACHE.length; i++) CACHE[i] = new Integer((int) (i + (-128)));
    }

    public static Integer valueOf(int i) {
        if (i >= -128 && i <= 127) return CACHE[(int) i - (-128)];
        return new Integer(i);
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

    public static int hashCode(int value) {
        return value;
    }

    public int hashCode() {
        return hashCode(value);
    }

    public boolean equals(Object obj) {
        return obj instanceof Integer other && value == other.value;
    }

    public static int compare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    public int compareTo(Integer other) {
        return compare(value, other.value);
    }

    public String toString() {
        return Long.toString(value);
    }

    public static String toString(int i) {
        return Long.toString(i);
    }

    public static String toHexString(int i) {
        return Long.unsigned(i & 0xffffffffL, 4);
    }

    public static String toBinaryString(int i) {
        return Long.unsigned(i & 0xffffffffL, 1);
    }

    public static String toOctalString(int i) {
        return Long.unsigned(i & 0xffffffffL, 3);
    }

    public static int sum(int a, int b) {
        return a + b;
    }

    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    public static int min(int a, int b) {
        return Math.min(a, b);
    }

    public static int signum(int i) {
        return (i >> 31) | (-i >>> 31);
    }

    public static int bitCount(int i) {
        int n = 0;
        for (int k = 0; k < 32; k++) n += (i >>> k) & 1;
        return n;
    }

    public static int numberOfLeadingZeros(int i) {
        if (i == 0) return 32;
        int n = 0;
        while ((i & 0x80000000) == 0) {
            n++;
            i <<= 1;
        }
        return n;
    }

    public static int numberOfTrailingZeros(int i) {
        if (i == 0) return 32;
        int n = 0;
        while ((i & 1) == 0) {
            n++;
            i >>>= 1;
        }
        return n;
    }

    public static int compareUnsigned(int x, int y) {
        return compare(x + MIN_VALUE, y + MIN_VALUE);
    }
}
