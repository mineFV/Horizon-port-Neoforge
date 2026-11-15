package java.nio.charset;

/**
 * The model of java.nio.charset.Charset: the three of StandardCharsets that String.getBytes encodes into. Not
 * modelled: forName, decoding, encoders and decoders, the other charsets.
 */
public abstract class Charset implements Comparable<Charset> {
    private final String name;

    protected Charset(String canonicalName, String[] aliases) {
        this.name = canonicalName;
    }

    public final String name() {
        return name;
    }

    public String displayName() {
        return name;
    }

    public final String toString() {
        return name;
    }

    /** By name, ignoring case: the names are ASCII, whose case is all there is to ignore. */
    public final int compareTo(Charset that) {
        String a = name(), b = that.name();
        for (int i = 0, n = Math.min(a.length(), b.length()); i < n; i++) {
            char x = lower(a.charAt(i)), y = lower(b.charAt(i));
            if (x != y) return x - y;
        }
        return a.length() - b.length();
    }

    private static char lower(char c) {
        return c >= 'A' && c <= 'Z' ? (char) (c + 32) : c;
    }

    public final int hashCode() {
        return name().hashCode();
    }

    public final boolean equals(Object ob) {
        if (this == ob) return true;
        if (!(ob instanceof Charset)) return false;
        return name.equals(((Charset) ob).name());
    }

    /** Which of the three: 0 UTF-8, 1 ISO-8859-1, 2 US-ASCII. Not the JDK's API. */
    abstract int jKind();

    /** The units' bytes in this charset, as String.getBytes gives them. Not the JDK's API. */
    public final byte[] jEncode(char[] units) {
        int kind = jKind();
        byte[] out = new byte[kind == 0 ? units.length * 3 : units.length];
        int n = 0;
        for (int i = 0; i < units.length; i++) {
            char c = units[i];
            boolean pair = Character.isHighSurrogate(c) && i + 1 < units.length && Character.isLowSurrogate(units[i + 1]);
            if (kind != 0) {
                int limit = kind == 1 ? 0xFF : 0x7F;
                if (c <= limit) {
                    out[n++] = (byte) c;
                } else {
                    // A pair is one character: one '?' for both units.
                    if (pair) i++;
                    out[n++] = (byte) '?';
                }
            } else if (c < 0x80) {
                out[n++] = (byte) c;
            } else if (c < 0x800) {
                out[n++] = (byte) (0xC0 | (c >> 6));
                out[n++] = (byte) (0x80 | (c & 0x3F));
            } else if (pair) {
                int cp = Character.toCodePoint(c, units[++i]);
                if (out.length < n + 4) out = java.util.Arrays.copyOf(out, out.length + 4);
                out[n++] = (byte) (0xF0 | (cp >> 18));
                out[n++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
                out[n++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
                out[n++] = (byte) (0x80 | (cp & 0x3F));
            } else if (Character.isSurrogate(c)) {
                // A surrogate without its other half: the replacement byte.
                out[n++] = (byte) '?';
            } else {
                out[n++] = (byte) (0xE0 | (c >> 12));
                out[n++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                out[n++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        return java.util.Arrays.copyOf(out, n);
    }
}
