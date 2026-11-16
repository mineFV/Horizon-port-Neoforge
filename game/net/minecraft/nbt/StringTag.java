package net.minecraft.nbt;

/** The stand-in for the game's StringTag; its text and quoting observed (`standins`). */
public class StringTag implements Tag {
    /** Observed: the tag of "" is one object. */
    private static final StringTag EMPTY = new StringTag("");

    private final String data;

    private StringTag(String data) {
        this.data = data;
    }

    /** Observed: EMPTY for "", a new tag otherwise; null throws a NullPointerException. */
    public static StringTag valueOf(String data) {
        return data.isEmpty() ? EMPTY : new StringTag(data);
    }

    @Override
    public byte getId() {
        return 8;
    }

    @Override
    public String toString() {
        return quoteAndEscape(data);
    }

    @Override
    public StringTag copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof StringTag other && data.equals(other.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String getAsString() {
        return data;
    }

    /**
     * Observed: the text between quotes, the delimiter being the other quote than the first one in it (" when it has
     * none), with a backslash before each backslash and each delimiter; nothing else is escaped.
     */
    public static String quoteAndEscape(String s) {
        StringBuilder b = new StringBuilder(" ");
        char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                b.append('\\');
            } else if (c == '"' || c == '\'') {
                if (quote == 0) quote = c == '"' ? '\'' : '"';
                if (quote == c) b.append('\\');
            }
            b.append(c);
        }
        if (quote == 0) quote = '"';
        b.setCharAt(0, quote);
        b.append(quote);
        return b.toString();
    }
}
