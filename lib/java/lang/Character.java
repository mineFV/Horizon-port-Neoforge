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

    public static int toCodePoint(char high, char low) {
        return ((high << 10) + low) + (0x010000 - (0xD800 << 10) - 0xDC00);
    }

    public static boolean isSurrogate(char ch) {
        return ch >= '\uD800' && ch <= '\uDFFF';
    }

    // ---- Unicode: the tables of the JDK that builds the tool (tools/UnicodeTables.java) ----

    public static final int MIN_RADIX = 2;
    public static final int MAX_RADIX = 36;
    public static final char MIN_HIGH_SURROGATE = '\uD800';
    public static final char MAX_HIGH_SURROGATE = '\uDBFF';
    public static final char MIN_LOW_SURROGATE = '\uDC00';
    public static final char MAX_LOW_SURROGATE = '\uDFFF';
    public static final char MIN_SURROGATE = MIN_HIGH_SURROGATE;
    public static final char MAX_SURROGATE = MAX_LOW_SURROGATE;
    public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x010000;
    public static final int MIN_CODE_POINT = 0x000000;
    public static final int MAX_CODE_POINT = 0X10FFFF;
    public static final int SIZE = 16;
    public static final int BYTES = 2;

    public static final byte UNASSIGNED = 0;
    public static final byte UPPERCASE_LETTER = 1;
    public static final byte LOWERCASE_LETTER = 2;
    public static final byte TITLECASE_LETTER = 3;
    public static final byte MODIFIER_LETTER = 4;
    public static final byte OTHER_LETTER = 5;
    public static final byte NON_SPACING_MARK = 6;
    public static final byte ENCLOSING_MARK = 7;
    public static final byte COMBINING_SPACING_MARK = 8;
    public static final byte DECIMAL_DIGIT_NUMBER = 9;
    public static final byte LETTER_NUMBER = 10;
    public static final byte OTHER_NUMBER = 11;
    public static final byte SPACE_SEPARATOR = 12;
    public static final byte LINE_SEPARATOR = 13;
    public static final byte PARAGRAPH_SEPARATOR = 14;
    public static final byte CONTROL = 15;
    public static final byte FORMAT = 16;
    public static final byte PRIVATE_USE = 18;
    public static final byte SURROGATE = 19;
    public static final byte DASH_PUNCTUATION = 20;
    public static final byte START_PUNCTUATION = 21;
    public static final byte END_PUNCTUATION = 22;
    public static final byte CONNECTOR_PUNCTUATION = 23;
    public static final byte OTHER_PUNCTUATION = 24;
    public static final byte MATH_SYMBOL = 25;
    public static final byte CURRENCY_SYMBOL = 26;
    public static final byte MODIFIER_SYMBOL = 27;
    public static final byte OTHER_SYMBOL = 28;
    public static final byte INITIAL_QUOTE_PUNCTUATION = 29;
    public static final byte FINAL_QUOTE_PUNCTUATION = 30;

    public static int getType(int codePoint) {
        return jType(codePoint);
    }

    public static int getType(char ch) {
        return jType(ch);
    }

    public static int toLowerCase(int codePoint) {
        return jLower(codePoint);
    }

    public static char toLowerCase(char ch) {
        return (char) jLower(ch);
    }

    public static int toUpperCase(int codePoint) {
        return jUpper(codePoint);
    }

    public static char toUpperCase(char ch) {
        return (char) jUpper(ch);
    }

    public static boolean isLowerCase(int codePoint) {
        return jProperty(0, codePoint);
    }

    public static boolean isLowerCase(char ch) {
        return jProperty(0, ch);
    }

    public static boolean isUpperCase(int codePoint) {
        return jProperty(1, codePoint);
    }

    public static boolean isUpperCase(char ch) {
        return jProperty(1, ch);
    }

    public static boolean isAlphabetic(int codePoint) {
        return jProperty(2, codePoint);
    }

    public static boolean isTitleCase(int codePoint) {
        return getType(codePoint) == TITLECASE_LETTER;
    }

    public static boolean isTitleCase(char ch) {
        return getType(ch) == TITLECASE_LETTER;
    }

    public static boolean isLetter(int codePoint) {
        int t = getType(codePoint);
        return t >= UPPERCASE_LETTER && t <= OTHER_LETTER;
    }

    public static boolean isLetter(char ch) {
        return isLetter((int) ch);
    }

    public static boolean isDigit(int codePoint) {
        return getType(codePoint) == DECIMAL_DIGIT_NUMBER;
    }

    public static boolean isDigit(char ch) {
        return isDigit((int) ch);
    }

    public static boolean isLetterOrDigit(int codePoint) {
        return isLetter(codePoint) || isDigit(codePoint);
    }

    public static boolean isLetterOrDigit(char ch) {
        return isLetterOrDigit((int) ch);
    }

    public static boolean isSpaceChar(int codePoint) {
        int t = getType(codePoint);
        return t == SPACE_SEPARATOR || t == LINE_SEPARATOR || t == PARAGRAPH_SEPARATOR;
    }

    public static boolean isSpaceChar(char ch) {
        return isSpaceChar((int) ch);
    }

    /** A space separator but a non-breaking one, or one of the controls the specification lists. */
    public static boolean isWhitespace(int codePoint) {
        return (isSpaceChar(codePoint) && codePoint != 0x00A0 && codePoint != 0x2007 && codePoint != 0x202F) || (codePoint >= 0x9 && codePoint <= 0xD)
                || (codePoint >= 0x1C && codePoint <= 0x1F);
    }

    public static boolean isWhitespace(char ch) {
        return isWhitespace((int) ch);
    }

    public static boolean isISOControl(int codePoint) {
        return codePoint <= 0x9F && (codePoint >= 0x7F || (codePoint >>> 5 == 0));
    }

    public static boolean isISOControl(char ch) {
        return isISOControl((int) ch);
    }

    public static int digit(int codePoint, int radix) {
        if (radix < MIN_RADIX || radix > MAX_RADIX) return -1;
        int v = jDigit(codePoint);
        return v >= 0 && v < radix ? v : -1;
    }

    public static int digit(char ch, int radix) {
        return digit((int) ch, radix);
    }

    public static char forDigit(int digit, int radix) {
        if ((digit >= radix) || (digit < 0)) return '\0';
        if ((radix < MIN_RADIX) || (radix > MAX_RADIX)) return '\0';
        if (digit < 10) return (char) ('0' + digit);
        return (char) ('a' - 10 + digit);
    }

    public static boolean isValidCodePoint(int codePoint) {
        int plane = codePoint >>> 16;
        return plane < ((MAX_CODE_POINT + 1) >>> 16);
    }

    public static boolean isBmpCodePoint(int codePoint) {
        return codePoint >>> 16 == 0;
    }

    public static boolean isSupplementaryCodePoint(int codePoint) {
        return codePoint >= MIN_SUPPLEMENTARY_CODE_POINT && codePoint < MAX_CODE_POINT + 1;
    }

    public static int charCount(int codePoint) {
        return codePoint >= MIN_SUPPLEMENTARY_CODE_POINT ? 2 : 1;
    }

    public static char highSurrogate(int codePoint) {
        return (char) ((codePoint >>> 10) + (MIN_HIGH_SURROGATE - (MIN_SUPPLEMENTARY_CODE_POINT >>> 10)));
    }

    public static char lowSurrogate(int codePoint) {
        return (char) ((codePoint & 0x3ff) + MIN_LOW_SURROGATE);
    }

    public static char[] toChars(int codePoint) {
        if (isBmpCodePoint(codePoint)) return new char[] {(char) codePoint};
        if (isValidCodePoint(codePoint)) return new char[] {highSurrogate(codePoint), lowSurrogate(codePoint)};
        throw new IllegalArgumentException("Not a valid Unicode code point: 0x".concat(upperHex(codePoint)));
    }

    private static String upperHex(int value) {
        char[] digits = Integer.toHexString(value).toCharArray();
        for (int i = 0; i < digits.length; i++) if (digits[i] >= 'a') digits[i] = (char) (digits[i] - 32);
        return new String(digits);
    }

    public static String toString(int codePoint) {
        return new String(toChars(codePoint));
    }

    /** The code point's lower (upper) case as a text where it isn't its simple one, else null. Not the JDK's API. */
    static native String jLowerText(int codePoint);

    static native String jUpperText(int codePoint);

    /** String.toLowerCase's "cased" for a final sigma. Not the JDK's API. */
    static boolean jCased(int codePoint) {
        return jProperty(3, codePoint);
    }

    private static native int jType(int codePoint);

    private static native int jLower(int codePoint);

    private static native int jUpper(int codePoint);

    /** 0: lower case, 1: upper case, 2: alphabetic, 3: cased (for a final sigma). */
    private static native boolean jProperty(int which, int codePoint);

    /** Character.digit's value in radix 36, -1 for none. */
    private static native int jDigit(int codePoint);
}
