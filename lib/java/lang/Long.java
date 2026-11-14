package java.lang;

/** The model of java.lang.Long: boxes (valueOf's cache from -128 to 127 included) and texts. parseLong isn't modelled yet. */
public final class Long extends Number implements Comparable<Long> {
    public static final long MIN_VALUE = 0x8000000000000000L;
    public static final long MAX_VALUE = 0x7fffffffffffffffL;
    public static final int SIZE = 64;
    public static final int BYTES = 8;

    private final long value;

    public Long(long value) {
        this.value = value;
    }

    /** The objects valueOf gives from -128 to 127: always the same ones, as the JDK's cache. */
    private static final Long[] CACHE = new Long[127 - (-128) + 1];

    static {
        for (int i = 0; i < CACHE.length; i++) CACHE[i] = new Long((long) (i + (-128)));
    }

    public static Long valueOf(long i) {
        if (i >= -128 && i <= 127) return CACHE[(int) i - (-128)];
        return new Long(i);
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

    public static int hashCode(long value) {
        return (int) (value ^ (value >>> 32));
    }

    public int hashCode() {
        return hashCode(value);
    }

    public boolean equals(Object obj) {
        return obj instanceof Long other && value == other.value;
    }

    public static int compare(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    public int compareTo(Long other) {
        return compare(value, other.value);
    }

    public String toString() {
        return toString(value);
    }

    public static String toString(long i) {
        if (i == 0) return "0";
        boolean negative = i < 0;
        char[] buf = new char[20];
        int at = buf.length;
        // Digits of the negative value, so that MIN_VALUE needs no special case.
        long n = negative ? i : -i;
        while (n != 0) {
            buf[--at] = (char) ('0' - (int) (n % 10));
            n /= 10;
        }
        if (negative) buf[--at] = '-';
        return new String(buf, at, buf.length - at);
    }

    public static String toHexString(long i) {
        return unsigned(i, 4);
    }

    public static String toBinaryString(long i) {
        return unsigned(i, 1);
    }

    public static String toOctalString(long i) {
        return unsigned(i, 3);
    }

    /** i, unsigned, in base 2 to the shift. */
    static String unsigned(long i, int shift) {
        if (i == 0) return "0";
        char[] buf = new char[64];
        int at = buf.length;
        long mask = (1L << shift) - 1;
        while (i != 0) {
            buf[--at] = "0123456789abcdef".charAt((int) (i & mask));
            i >>>= shift;
        }
        return new String(buf, at, buf.length - at);
    }

    public static long sum(long a, long b) {
        return a + b;
    }

    public static long max(long a, long b) {
        return Math.max(a, b);
    }

    public static long min(long a, long b) {
        return Math.min(a, b);
    }

    public static int signum(long i) {
        return (int) ((i >> 63) | (-i >>> 63));
    }
}
