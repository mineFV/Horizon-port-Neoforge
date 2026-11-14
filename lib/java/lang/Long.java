package java.lang;

/** The model of java.lang.Long: for now, its texts. */
public final class Long {
    public static final long MIN_VALUE = 0x8000000000000000L;
    public static final long MAX_VALUE = 0x7fffffffffffffffL;

    private Long() {}

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
}
