package java.lang;

/** The model of java.lang.Integer: boxes (valueOf's cache from -128 to 127 included) and texts. */
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

    public static int rotateLeft(int i, int distance) {
        return (i << distance) | (i >>> -distance);
    }

    public static int rotateRight(int i, int distance) {
        return (i >>> distance) | (i << -distance);
    }

    /**
     * An optional sign, then digits as Character.digit reads them in the radix (other scripts' and full-width digits
     * among them), within the type's range; else a NumberFormatException with the JDK's message.
     */
    public static int parseInt(String s, int radix) {
        if (s == null) throw new NumberFormatException("Cannot parse null string");
        NumberFormatException.checkRadix(radix);
        int len = s.length();
        if (len == 0) throw NumberFormatException.forInputString("", radix);
        int i = 0;
        char firstChar = s.charAt(0);
        boolean negative = false;
        if (firstChar == '-' || firstChar == '+') {
            negative = firstChar == '-';
            if (len == 1) throw NumberFormatException.forInputString(s, radix);
            i = 1;
        }
        // Accumulated negatively, so that the minimum fits.
        int limit = negative ? Integer.MIN_VALUE : -Integer.MAX_VALUE;
        int multmin = limit / radix;
        int result = 0;
        while (i < len) {
            int digit = Character.digit(s.charAt(i++), radix);
            if (digit < 0 || result < multmin) throw NumberFormatException.forInputString(s, radix);
            result *= radix;
            if (result < limit + digit) throw NumberFormatException.forInputString(s, radix);
            result -= digit;
        }
        return negative ? result : -result;
    }

    public static int parseInt(String s) {
        return parseInt(s, 10);
    }

    public static Integer valueOf(String s, int radix) {
        return Integer.valueOf(parseInt(s, radix));
    }

    public static Integer valueOf(String s) {
        return Integer.valueOf(parseInt(s, 10));
    }
}
