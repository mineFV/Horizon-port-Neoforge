package java.lang;

/** The model of java.lang.Integer: for now, its texts. Boxing comes with the rest of the library. */
public final class Integer {
    public static final int MIN_VALUE = 0x80000000;
    public static final int MAX_VALUE = 0x7fffffff;

    private Integer() {}

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
}
