package java.lang;

/**
 * The model of java.lang.String: its UTF-16 units in a char array, its hash as the API specifies it. The exceptions
 * it throws carry the messages of the JDK the tests run on. Case conversions are the root locale's (the model's
 * Locale has no other rules), with the JDK's final sigma; the no-argument ones (the default locale) aren't
 * modelled, nor formatting, regular expressions, decoding.
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

    /** The code point at index: a surrogate pair's, from its high surrogate. */
    public int codePointAt(int index) {
        char high = charAt(index);
        if (Character.isHighSurrogate(high) && index + 1 < value.length) {
            char low = value[index + 1];
            if (Character.isLowSurrogate(low)) return ((high - 0xD800) << 10) + (low - 0xDC00) + 0x10000;
        }
        return high;
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

    public byte[] getBytes(java.nio.charset.Charset charset) {
        if (charset == null) throw new NullPointerException();
        return charset.jEncode(value);
    }

    /** In the default charset: UTF-8, as the JDK's has been since Java 18 (JEP 400). */
    public byte[] getBytes() {
        return java.nio.charset.StandardCharsets.UTF_8.jEncode(value);
    }

    // ---- case, code points, joining ----

    public int codePointBefore(int index) {
        int i = index - 1;
        if (i < 0 || i >= value.length) throw new StringIndexOutOfBoundsException("Index " .concat(Integer.toString(index)).concat(" out of bounds for length ").concat(Integer.toString(value.length)));
        char low = value[i];
        if (Character.isLowSurrogate(low) && i > 0 && Character.isHighSurrogate(value[i - 1])) return Character.toCodePoint(value[i - 1], low);
        return low;
    }

    public int codePointCount(int beginIndex, int endIndex) {
        if (beginIndex < 0 || beginIndex > endIndex || endIndex > value.length) {
            throw new IndexOutOfBoundsException("begin ".concat(Integer.toString(beginIndex)).concat(", end ").concat(Integer.toString(endIndex)).concat(", length ")
                    .concat(Integer.toString(value.length)));
        }
        int n = endIndex - beginIndex;
        for (int i = beginIndex; i < endIndex - 1; i++) {
            if (Character.isHighSurrogate(value[i]) && Character.isLowSurrogate(value[i + 1])) {
                n--;
                i++;
            }
        }
        return n;
    }

    /** As the root locale lowers it: the same String when nothing changes and it holds no surrogate (as the JDK's). */
    public String toLowerCase(java.util.Locale locale) {
        if (locale == null) throw new NullPointerException();
        int len = value.length;
        int first = 0;
        for (; first < len; first++) {
            char c = value[first];
            if (Character.isSurrogate(c) || c != Character.toLowerCase(c) || Character.jLowerText(c) != null) break;
        }
        if (first == len) return this;
        StringBuilder b = new StringBuilder(len);
        for (int i = 0; i < first; i++) b.append(value[i]);
        for (int i = first; i < len; ) {
            int cp = WordBreaks.codePointAt(value, i);
            if (cp == 0x03A3) {
                b.append(finalCased(i) ? '\u03C2' : '\u03C3');
            } else {
                String text = Character.jLowerText(cp);
                if (text != null) b.append(text);
                else b.appendCodePoint(Character.toLowerCase(cp));
            }
            i += Character.charCount(cp);
        }
        return b.toString();
    }

    /** A capital sigma at index ends a word: a cased letter before it in its word, none after it (the JDK's test). */
    private boolean finalCased(int index) {
        int ch;
        for (int i = index; (i >= 0) && !WordBreaks.isBoundary(value, i); i -= Character.charCount(ch)) {
            ch = codePointBefore(i);
            if (Character.jCased(ch)) {
                int len = value.length;
                for (i = index + Character.charCount(WordBreaks.codePointAt(value, index)); (i < len) && !WordBreaks.isBoundary(value, i); i += Character.charCount(ch)) {
                    ch = WordBreaks.codePointAt(value, i);
                    if (Character.jCased(ch)) return false;
                }
                return true;
            }
        }
        return false;
    }

    /** As the root locale raises it (ß is SS): the same String when nothing changes and it holds no surrogate. */
    public String toUpperCase(java.util.Locale locale) {
        if (locale == null) throw new NullPointerException();
        int len = value.length;
        int first = 0;
        for (; first < len; first++) {
            char c = value[first];
            if (Character.isSurrogate(c) || c != Character.toUpperCase(c) || Character.jUpperText(c) != null) break;
        }
        if (first == len) return this;
        StringBuilder b = new StringBuilder(len);
        for (int i = 0; i < first; i++) b.append(value[i]);
        for (int i = first; i < len; ) {
            int cp = WordBreaks.codePointAt(value, i);
            String text = Character.jUpperText(cp);
            if (text != null) b.append(text);
            else b.appendCodePoint(Character.toUpperCase(cp));
            i += Character.charCount(cp);
        }
        return b.toString();
    }

    public boolean equalsIgnoreCase(String anotherString) {
        return (this == anotherString) ? true : (anotherString != null) && (anotherString.value.length == value.length) && regionMatches(true, 0, anotherString, 0, value.length);
    }

    public boolean regionMatches(int toffset, String other, int ooffset, int len) {
        return regionMatches(false, toffset, other, ooffset, len);
    }

    /** Ignoring case, by code points: equal, or equal raised, or equal raised then lowered (as the JDK's since 16). */
    public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len) {
        if ((ooffset < 0) || (toffset < 0) || (toffset > (long) value.length - len) || (ooffset > (long) other.value.length - len)) return false;
        if (!ignoreCase) {
            for (int k = 0; k < len; k++) if (value[toffset + k] != other.value[ooffset + k]) return false;
            return true;
        }
        int tstart = toffset, ostart = ooffset;
        int tlast = toffset + len;
        while (toffset < tlast) {
            char c1 = value[toffset++];
            char c2 = other.value[ooffset++];
            if (c1 == c2) continue;
            int cp1 = c1, cp2 = c2;
            if (Character.isHighSurrogate(c1) && Character.isHighSurrogate(c2) && toffset < tlast && Character.isLowSurrogate(value[toffset])
                    && Character.isLowSurrogate(other.value[ooffset])) {
                cp1 = Character.toCodePoint(c1, value[toffset++]);
                cp2 = Character.toCodePoint(c2, other.value[ooffset++]);
            } else if (Character.isLowSurrogate(c1) && Character.isLowSurrogate(c2) && toffset - 2 >= tstart && ooffset - 2 >= ostart
                    && Character.isHighSurrogate(value[toffset - 2]) && Character.isHighSurrogate(other.value[ooffset - 2])) {
                // The pairs differ in their low halves: compared as code points.
                cp1 = Character.toCodePoint(value[toffset - 2], c1);
                cp2 = Character.toCodePoint(other.value[ooffset - 2], c2);
            }
            if (cp1 == cp2) continue;
            int u1 = Character.toUpperCase(cp1);
            int u2 = Character.toUpperCase(cp2);
            if (u1 == u2) continue;
            if (Character.toLowerCase(u1) == Character.toLowerCase(u2)) continue;
            return false;
        }
        return true;
    }

    public int compareToIgnoreCase(String str) {
        int n1 = value.length, n2 = str.value.length;
        int lim = Math.min(n1, n2);
        for (int k = 0; k < lim; k++) {
            char c1 = value[k], c2 = str.value[k];
            if (c1 != c2) {
                int cp1 = c1, cp2 = c2;
                if (Character.isHighSurrogate(c1) && Character.isHighSurrogate(c2) && k + 1 < lim && Character.isLowSurrogate(value[k + 1]) && Character.isLowSurrogate(str.value[k + 1])) {
                    cp1 = Character.toCodePoint(c1, value[k + 1]);
                    cp2 = Character.toCodePoint(c2, str.value[k + 1]);
                    k++;
                } else if (Character.isLowSurrogate(c1) && Character.isLowSurrogate(c2) && k > 0 && Character.isHighSurrogate(value[k - 1]) && Character.isHighSurrogate(str.value[k - 1])) {
                    cp1 = Character.toCodePoint(value[k - 1], c1);
                    cp2 = Character.toCodePoint(str.value[k - 1], c2);
                }
                cp1 = Character.toUpperCase(cp1);
                cp2 = Character.toUpperCase(cp2);
                if (cp1 != cp2) {
                    cp1 = Character.toLowerCase(cp1);
                    cp2 = Character.toLowerCase(cp2);
                    if (cp1 != cp2) return cp1 - cp2;
                }
            }
        }
        return n1 - n2;
    }

    /** Every occurrence, from the left; an empty target puts the replacement around every unit. */
    public String replace(CharSequence target, CharSequence replacement) {
        String trgtStr = target.toString();
        String replStr = replacement.toString();
        int thisLen = value.length;
        int trgtLen = trgtStr.length();
        if (trgtLen > 0) {
            int j = indexOf(trgtStr);
            if (j < 0) return this;
            StringBuilder b = new StringBuilder();
            int i = 0;
            do {
                for (int k = i; k < j; k++) b.append(value[k]);
                b.append(replStr);
                i = j + trgtLen;
            } while (j < thisLen && (j = indexOf(trgtStr, i)) > 0);
            for (int k = i; k < thisLen; k++) b.append(value[k]);
            return b.toString();
        }
        StringBuilder b = new StringBuilder();
        b.append(replStr);
        for (int k = 0; k < thisLen; k++) {
            b.append(value[k]);
            b.append(replStr);
        }
        return b.toString();
    }

    public int lastIndexOf(String str) {
        return lastIndexOf(str, value.length);
    }

    public int lastIndexOf(String str, int fromIndex) {
        char[] s = str.value;
        int rightIndex = value.length - s.length;
        if (fromIndex > rightIndex) fromIndex = rightIndex;
        for (int i = fromIndex; i >= 0; i--) if (regionMatches(i, s)) return i;
        return -1;
    }

    public boolean isBlank() {
        for (int i = 0; i < value.length; ) {
            int cp = WordBreaks.codePointAt(value, i);
            if (!Character.isWhitespace(cp)) return false;
            i += Character.charCount(cp);
        }
        return true;
    }

    public String strip() {
        int left = 0, right = value.length;
        while (left < right && Character.isWhitespace(WordBreaks.codePointAt(value, left))) left += Character.charCount(WordBreaks.codePointAt(value, left));
        while (right > left && Character.isWhitespace(codePointBefore(right))) right -= Character.charCount(codePointBefore(right));
        return left == 0 && right == value.length ? this : substring(left, right);
    }

    public static String join(CharSequence delimiter, CharSequence... elements) {
        java.util.Objects.requireNonNull(delimiter);
        java.util.Objects.requireNonNull(elements);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) b.append(delimiter);
            b.append(String.valueOf(elements[i]));
        }
        return b.toString();
    }

    public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
        java.util.Objects.requireNonNull(delimiter);
        java.util.Objects.requireNonNull(elements);
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (CharSequence cs : elements) {
            if (!first) b.append(delimiter);
            b.append(String.valueOf(cs));
            first = false;
        }
        return b.toString();
    }

    public static String copyValueOf(char[] data) {
        return new String(data);
    }

    public static String copyValueOf(char[] data, int offset, int count) {
        return new String(data, offset, count);
    }

    public CharSequence subSequence(int beginIndex, int endIndex) {
        return substring(beginIndex, endIndex);
    }

    // ---- split: the JDK's fast path alone (the translator lets a call in only with such a separator) ----

    public String[] split(String regex) {
        return split(regex, 0);
    }

    /**
     * By a separator of one plain character (or a backslash and one that isn't a letter nor a digit), as the JDK
     * splits without a regular expression: no separator gives this String alone; a limit of 0 drops the trailing
     * empty parts. Any other separator is a regular expression, which the model hasn't.
     */
    public String[] split(String regex, int limit) {
        char ch;
        if (regex.length() == 1) ch = regex.charAt(0);
        else if (regex.length() == 2 && regex.charAt(0) == '\\') ch = regex.charAt(1);
        else throw new AssertionError("a regular expression: not modelled (the translator lets no call in with one)");
        int matchCount = 0;
        int off = 0;
        int next;
        boolean limited = limit > 0;
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        while ((next = indexOf(ch, off)) != -1) {
            if (!limited || matchCount < limit - 1) {
                list.add(substring(off, next));
                off = next + 1;
                ++matchCount;
            } else {
                int last = length();
                list.add(substring(off, last));
                off = last;
                ++matchCount;
                break;
            }
        }
        if (off == 0) return new String[] {this};
        if (!limited || matchCount < limit) list.add(substring(off, length()));
        int resultSize = list.size();
        if (limit == 0) {
            while (resultSize > 0 && list.get(resultSize - 1).isEmpty()) resultSize--;
        }
        String[] result = new String[resultSize];
        for (int k = 0; k < resultSize; k++) result[k] = list.get(k);
        return result;
    }
}
