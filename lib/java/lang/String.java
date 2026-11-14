package java.lang;

/**
 * The model of java.lang.String: its UTF-16 units in a char array, its hash as the API specifies it. The exceptions
 * it throws carry the messages of the JDK the tests run on. Not modelled yet: case conversions, formatting,
 * regular expressions, encodings, code points beyond indexOf.
 */
public final class String implements java.io.Serializable, Comparable<String>, CharSequence {
    private final char[] value;
    private int hash;
    private boolean hashIsZero;

    public String() {
        this.value = new char[0];
    }

    public String(String original) {
        this.value = original.value;
        this.hash = original.hash;
        this.hashIsZero = original.hashIsZero;
    }

    public String(char[] value) {
        this(value, 0, value.length);
    }

    public String(char[] value, int offset, int count) {
        int length = value.length;
        if ((offset | count) < 0 || offset > length - count) {
            throw new StringIndexOutOfBoundsException("Range [".concat(Integer.toString(offset)).concat(", ").concat(Integer.toString(offset))
                    .concat(" + ").concat(Integer.toString(count)).concat(") out of bounds for length ").concat(Integer.toString(length)));
        }
        char[] copy = new char[count];
        for (int i = 0; i < count; i++) copy[i] = value[offset + i];
        this.value = copy;
    }

    /** A string over these units, which nothing else changes (the model's own, and the translator's). */
    String(char[] value, boolean shared) {
        this.value = value;
    }

    public int length() {
        return value.length;
    }

    public boolean isEmpty() {
        return value.length == 0;
    }

    public char charAt(int index) {
        if (index < 0 || index >= value.length) throw new StringIndexOutOfBoundsException(outOfBounds(index, value.length));
        return value[index];
    }

    static String outOfBounds(int index, int length) {
        return "Index ".concat(Integer.toString(index)).concat(" out of bounds for length ").concat(Integer.toString(length));
    }

    static String range(int begin, int end, int length) {
        return "Range [".concat(Integer.toString(begin)).concat(", ").concat(Integer.toString(end)).concat(") out of bounds for length ")
                .concat(Integer.toString(length));
    }

    public boolean equals(Object anObject) {
        if (this == anObject) return true;
        if (anObject instanceof String other) {
            char[] a = value, b = other.value;
            if (a.length != b.length) return false;
            for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false;
            return true;
        }
        return false;
    }

    public int hashCode() {
        int h = hash;
        if (h == 0 && !hashIsZero) {
            for (char c : value) h = 31 * h + c;
            if (h == 0) hashIsZero = true;
            else hash = h;
        }
        return h;
    }

    public int compareTo(String other) {
        char[] a = value, b = other.value;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            if (a[i] != b[i]) return a[i] - b[i];
        }
        return a.length - b.length;
    }

    public String concat(String str) {
        if (str.isEmpty()) return this;
        char[] out = new char[value.length + str.value.length];
        for (int i = 0; i < value.length; i++) out[i] = value[i];
        for (int i = 0; i < str.value.length; i++) out[value.length + i] = str.value[i];
        return new String(out, true);
    }

    public String substring(int beginIndex) {
        return substring(beginIndex, value.length);
    }

    public String substring(int beginIndex, int endIndex) {
        int length = value.length;
        if (beginIndex < 0 || beginIndex > endIndex || endIndex > length) throw new StringIndexOutOfBoundsException(range(beginIndex, endIndex, length));
        if (beginIndex == 0 && endIndex == length) return this;
        char[] out = new char[endIndex - beginIndex];
        for (int i = beginIndex; i < endIndex; i++) out[i - beginIndex] = value[i];
        return new String(out, true);
    }

    public int indexOf(int ch) {
        return indexOf(ch, 0);
    }

    public int indexOf(int ch, int fromIndex) {
        if (fromIndex < 0) fromIndex = 0;
        if (ch >= 0 && ch < 0x10000) {
            for (int i = fromIndex; i < value.length; i++) if (value[i] == ch) return i;
            return -1;
        }
        if (ch < 0 || ch > 0x10FFFF) return -1;
        char high = (char) ((ch >>> 10) + (0xD800 - (0x010000 >>> 10))), low = (char) ((ch & 0x3ff) + 0xDC00);
        for (int i = fromIndex; i < value.length - 1; i++) if (value[i] == high && value[i + 1] == low) return i;
        return -1;
    }

    public int lastIndexOf(int ch) {
        if (ch >= 0 && ch < 0x10000) {
            for (int i = value.length - 1; i >= 0; i--) if (value[i] == ch) return i;
            return -1;
        }
        if (ch < 0 || ch > 0x10FFFF) return -1;
        char high = (char) ((ch >>> 10) + (0xD800 - (0x010000 >>> 10))), low = (char) ((ch & 0x3ff) + 0xDC00);
        for (int i = value.length - 2; i >= 0; i--) if (value[i] == high && value[i + 1] == low) return i;
        return -1;
    }

    public int indexOf(String str) {
        return indexOf(str, 0);
    }

    public int indexOf(String str, int fromIndex) {
        char[] s = str.value;
        if (fromIndex < 0) fromIndex = 0;
        if (fromIndex > value.length) return s.length == 0 ? value.length : -1;
        for (int i = fromIndex; i <= value.length - s.length; i++) {
            if (regionMatches(i, s)) return i;
        }
        return -1;
    }

    private boolean regionMatches(int at, char[] s) {
        for (int k = 0; k < s.length; k++) if (value[at + k] != s[k]) return false;
        return true;
    }

    public boolean startsWith(String prefix, int toffset) {
        if (toffset < 0 || toffset > value.length - prefix.value.length) return false;
        return regionMatches(toffset, prefix.value);
    }

    public boolean startsWith(String prefix) {
        return startsWith(prefix, 0);
    }

    public boolean endsWith(String suffix) {
        return startsWith(suffix, value.length - suffix.value.length);
    }

    public boolean contains(CharSequence s) {
        return indexOf(s.toString()) >= 0;
    }

    public String replace(char oldChar, char newChar) {
        if (oldChar == newChar) return this;
        int i = indexOf(oldChar);
        if (i < 0) return this;
        char[] out = new char[value.length];
        for (int k = 0; k < value.length; k++) out[k] = value[k] == oldChar ? newChar : value[k];
        return new String(out, true);
    }

    public char[] toCharArray() {
        char[] out = new char[value.length];
        for (int i = 0; i < value.length; i++) out[i] = value[i];
        return out;
    }

    public String trim() {
        int begin = 0, end = value.length;
        while (begin < end && value[begin] <= ' ') begin++;
        while (end > begin && value[end - 1] <= ' ') end--;
        return begin > 0 || end < value.length ? substring(begin, end) : this;
    }

    public String repeat(int count) {
        if (count < 0) throw new IllegalArgumentException("count is negative: ".concat(Integer.toString(count)));
        if (count == 1) return this;
        char[] out = new char[value.length * count];
        for (int r = 0; r < count; r++) for (int i = 0; i < value.length; i++) out[r * value.length + i] = value[i];
        return new String(out, true);
    }

    public String toString() {
        return this;
    }

    public native String intern();

    public static String valueOf(Object obj) {
        return obj == null ? "null" : obj.toString();
    }

    public static String valueOf(char[] data) {
        return new String(data);
    }

    public static String valueOf(boolean b) {
        return b ? "true" : "false";
    }

    public static String valueOf(char c) {
        return new String(new char[] {c}, true);
    }

    public static String valueOf(int i) {
        return Integer.toString(i);
    }

    public static String valueOf(long l) {
        return Long.toString(l);
    }

    public static String valueOf(float f) {
        return floatText(f);
    }

    public static String valueOf(double d) {
        return doubleText(d);
    }

    /** Float.toString and Double.toString: the shortest decimal that reads back as the value, as the JDK writes it. */
    static native String floatText(float f);

    static native String doubleText(double d);

    /** A String value in a string concatenation: itself, or "null". */
    static String jtext(String s) {
        return s == null ? "null" : s;
    }

    /** What a string concatenation (invokedynamic makeConcatWithConstants) makes of its parts, already texts. */
    static String jconcat(String[] parts) {
        int n = 0;
        for (String p : parts) n += p.value.length;
        char[] out = new char[n];
        int at = 0;
        for (String p : parts) {
            for (char c : p.value) out[at++] = c;
        }
        return new String(out, true);
    }
}
