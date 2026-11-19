package it.unimi.dsi.fastutil.chars;

/** Not fastutil's: what its SafeMath does for chars (observed, `standins`). */
final class HorizonChars {
    private HorizonChars() {}

    static char safeIntToChar(int value) {
        if (value < Character.MIN_VALUE || value > Character.MAX_VALUE) throw new IllegalArgumentException(value + " can't be represented as char");
        return (char) value;
    }
}
