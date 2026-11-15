package java.lang;

/** The model of java.lang.NumberFormatException. */
public class NumberFormatException extends IllegalArgumentException {
    public NumberFormatException() {}

    public NumberFormatException(String message) {
        super(message);
    }

    static NumberFormatException forInputString(String s, int radix) {
        return new NumberFormatException("For input string: \"".concat(s).concat("\"").concat(radix == 10 ? "" : " under radix ".concat(Integer.toString(radix))));
    }

    static void checkRadix(int radix) {
        if (radix < Character.MIN_RADIX) throw new NumberFormatException("radix ".concat(Integer.toString(radix)).concat(" less than Character.MIN_RADIX"));
        if (radix > Character.MAX_RADIX) throw new NumberFormatException("radix ".concat(Integer.toString(radix)).concat(" greater than Character.MAX_RADIX"));
    }
}
