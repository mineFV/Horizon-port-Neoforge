package java.lang;

/** The model of java.lang.Long: boxes (valueOf's cache from -128 to 127 included) and texts. */
public final class Long extends Number implements Comparable<Long> {
    @SuppressWarnings("unchecked")
    public static final Class<Long> TYPE = (Class<Long>) Class.jPrimitive('J');

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

    public static int bitCount(long i) {
        int n = 0;
        for (int k = 0; k < 64; k++) n += (int) ((i >>> k) & 1);
        return n;
    }

    public static int numberOfLeadingZeros(long i) {
        if (i == 0) return 64;
        int n = 0;
        while ((i & 0x8000000000000000L) == 0) {
            n++;
            i <<= 1;
        }
        return n;
    }

    public static int numberOfTrailingZeros(long i) {
        if (i == 0) return 64;
        int n = 0;
        while ((i & 1) == 0) {
            n++;
            i >>>= 1;
        }
        return n;
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

    /**
     * An optional sign, then digits as Character.digit reads them in the radix (other scripts' and full-width digits
     * among them), within the type's range; else a NumberFormatException with the JDK's message.
     */
    public static long parseLong(String s, int radix) {
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
        long limit = negative ? Long.MIN_VALUE : -Long.MAX_VALUE;
        long multmin = limit / radix;
        long result = 0;
        while (i < len) {
            int digit = Character.digit(s.charAt(i++), radix);
            if (digit < 0 || result < multmin) throw NumberFormatException.forInputString(s, radix);
            result *= radix;
            if (result < limit + digit) throw NumberFormatException.forInputString(s, radix);
            result -= digit;
        }
        return negative ? result : -result;
    }

    public static long parseLong(String s) {
        return parseLong(s, 10);
    }

    public static Long valueOf(String s, int radix) {
        return Long.valueOf(parseLong(s, radix));
    }

    public static Long valueOf(String s) {
        return Long.valueOf(parseLong(s, 10));
    }

    /** The characters from beginIndex to endIndex, with the JDK's messages (the index of the bad one in the part). */
    public static long parseLong(CharSequence s, int beginIndex, int endIndex, int radix) {
        java.util.Objects.requireNonNull(s);
        int length = s.length();
        if (beginIndex < 0 || beginIndex > endIndex || endIndex > length) {
            throw new IndexOutOfBoundsException("Range [".concat(Integer.toString(beginIndex)).concat(", ").concat(Integer.toString(endIndex)).concat(") out of bounds for length ")
                    .concat(Integer.toString(length)));
        }
        NumberFormatException.checkRadix(radix);
        boolean negative = false;
        int i = beginIndex;
        long limit = -MAX_VALUE;
        if (i >= endIndex) throw new NumberFormatException("");
        char firstChar = s.charAt(i);
        if (firstChar < '0') {
            if (firstChar == '-') {
                negative = true;
                limit = MIN_VALUE;
            } else if (firstChar != '+') {
                throw forCharSequence(s, beginIndex, endIndex, i);
            }
            i++;
        }
        if (i >= endIndex) throw forCharSequence(s, beginIndex, endIndex, i);
        long multmin = limit / radix;
        long result = 0;
        while (i < endIndex) {
            int digit = Character.digit(s.charAt(i), radix);
            if (digit < 0 || result < multmin) throw forCharSequence(s, beginIndex, endIndex, i);
            result *= radix;
            if (result < limit + digit) throw forCharSequence(s, beginIndex, endIndex, i);
            i++;
            result -= digit;
        }
        return negative ? result : -result;
    }

    private static NumberFormatException forCharSequence(CharSequence s, int beginIndex, int endIndex, int errorIndex) {
        return new NumberFormatException("Error at index ".concat(Integer.toString(errorIndex - beginIndex)).concat(" in: \"")
                .concat(s.subSequence(beginIndex, endIndex).toString()).concat("\""));
    }

    /**
     * As an unsigned value: an optional '+', digits read from the left; the first trouble met is the one reported
     * (a digit the radix hasn't, or the value past 2^64 - 1), as the JDK the tests run on.
     */
    public static long parseUnsignedLong(String s, int radix) {
        if (s == null) throw new NumberFormatException("Cannot parse null string");
        NumberFormatException.checkRadix(radix);
        int len = s.length();
        if (len == 0) throw NumberFormatException.forInputString(s, radix);
        int i = 0;
        char firstChar = s.charAt(0);
        if (firstChar == '-') throw new NumberFormatException("Illegal leading minus sign on unsigned string ".concat(s).concat("."));
        if (firstChar == '+') i = 1;
        if (i >= len) throw NumberFormatException.forInputString(s, radix);
        long result = 0;
        for (; i < len; i++) {
            int d = Character.digit(s.charAt(i), radix);
            if (d < 0) throw NumberFormatException.forInputString(s, radix);
            long lo = (result & 0xffffffffL) * radix + d;
            long hi = (result >>> 32) * radix + (lo >>> 32);
            if ((hi >>> 32) != 0) throw new NumberFormatException("String value ".concat(s).concat(" exceeds range of unsigned long."));
            result = (hi << 32) | (lo & 0xffffffffL);
        }
        return result;
    }

    public static long parseUnsignedLong(String s) {
        return parseUnsignedLong(s, 10);
    }
}
