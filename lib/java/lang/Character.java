package java.lang;

/** The model of java.lang.Character: boxes (valueOf's cache from 0 to 127 included). Its Unicode tables aren't modelled. */
public final class Character implements java.io.Serializable, Comparable<Character> {
    @SuppressWarnings("unchecked")
    public static final Class<Character> TYPE = (Class<Character>) Class.jPrimitive('C');

    public static final char MIN_VALUE = '\u0000';
    public static final char MAX_VALUE = '￿';
    private final char value;

    public Character(char value) {
        this.value = value;
    }

    private static final Character[] CACHE = new Character[128];

    static {
        for (int i = 0; i < CACHE.length; i++) CACHE[i] = new Character((char) i);
    }

    public static Character valueOf(char c) {
        if (c <= 127) return CACHE[c];
        return new Character(c);
    }

    public char charValue() {
        return value;
    }

    public static int hashCode(char value) {
        return value;
    }

    public int hashCode() {
        return value;
    }

    public boolean equals(Object obj) {
        return obj instanceof Character c && value == c.value;
    }

    public static int compare(char x, char y) {
        return x - y;
    }

    public int compareTo(Character other) {
        return compare(value, other.value);
    }

    public static String toString(char c) {
        return String.valueOf(c);
    }

    public String toString() {
        return String.valueOf(value);
    }

    public static boolean isHighSurrogate(char ch) {
        return ch >= '\uD800' && ch <= '\uDBFF';
    }

    public static boolean isLowSurrogate(char ch) {
        return ch >= '\uDC00' && ch <= '\uDFFF';
    }

    public static boolean isSurrogate(char ch) {
        return ch >= '\uD800' && ch <= '\uDFFF';
    }
}
