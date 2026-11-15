package java.lang;

/**
 * The model of java.lang.StringBuilder: its UTF-16 units in a char array that grows, the JDK's messages. How much
 * the array grows by isn't observable, so it isn't the JDK's; capacity() isn't modelled, nor Appendable, code points
 * beyond reverse, or chars().
 */
public final class StringBuilder implements java.io.Serializable, Comparable<StringBuilder>, CharSequence {
    private char[] value;
    private int count;

    public StringBuilder() {
        value = new char[16];
    }

    public StringBuilder(int capacity) {
        if (capacity < 0) throw new NegativeArraySizeException(Integer.toString(capacity));
        value = new char[capacity];
    }

    public StringBuilder(String str) {
        value = new char[str.length() + 16];
        append(str);
    }

    public StringBuilder(CharSequence seq) {
        value = new char[seq.length() + 16];
        append(seq);
    }

    private void ensure(int minimum) {
        if (minimum <= value.length) return;
        char[] bigger = new char[Math.max(minimum, value.length * 2 + 2)];
        for (int i = 0; i < count; i++) bigger[i] = value[i];
        value = bigger;
    }

    public int length() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public char charAt(int index) {
        if (index < 0 || index >= count) throw new StringIndexOutOfBoundsException(String.outOfBounds(index, count));
        return value[index];
    }

    public void setCharAt(int index, char ch) {
        if (index < 0 || index >= count) throw new StringIndexOutOfBoundsException(String.outOfBounds(index, count));
        value[index] = ch;
    }

    public void setLength(int newLength) {
        if (newLength < 0) throw new StringIndexOutOfBoundsException("String index out of range: ".concat(Integer.toString(newLength)));
        ensure(newLength);
        for (int i = count; i < newLength; i++) value[i] = '\0';
        count = newLength;
    }

    public StringBuilder append(Object obj) {
        return append(String.valueOf(obj));
    }

    public StringBuilder append(String str) {
        if (str == null) str = "null";
        int len = str.length();
        ensure(count + len);
        for (int i = 0; i < len; i++) value[count + i] = str.charAt(i);
        count += len;
        return this;
    }

    public StringBuilder append(StringBuilder sb) {
        if (sb == null) return append("null");
        return append((CharSequence) sb);
    }

    public StringBuilder append(CharSequence s) {
        if (s == null) return append("null");
        if (s instanceof String str) return append(str);
        return append(s, 0, s.length());
    }

    public StringBuilder append(CharSequence s, int start, int end) {
        if (s == null) s = "null";
        if (start < 0 || start > end || end > s.length()) throw new IndexOutOfBoundsException(String.range(start, end, s.length()));
        ensure(count + end - start);
        for (int i = start; i < end; i++) value[count++] = s.charAt(i);
        return this;
    }

    public StringBuilder append(char[] str) {
        ensure(count + str.length);
        for (char c : str) value[count++] = c;
        return this;
    }

    public StringBuilder append(char[] str, int offset, int len) {
        int end = offset + len;
        if (offset < 0 || offset > end || end > str.length) throw new IndexOutOfBoundsException(String.range(offset, end, str.length));
        ensure(count + len);
        for (int i = offset; i < end; i++) value[count++] = str[i];
        return this;
    }

    public StringBuilder append(boolean b) {
        return append(b ? "true" : "false");
    }

    public StringBuilder appendCodePoint(int codePoint) {
        if (Character.isBmpCodePoint(codePoint)) return append((char) codePoint);
        if (!Character.isValidCodePoint(codePoint)) throw new IllegalArgumentException("Not a valid Unicode code point: 0x".concat(Integer.toHexString(codePoint).toUpperCase(java.util.Locale.ROOT)));
        append(Character.highSurrogate(codePoint));
        return append(Character.lowSurrogate(codePoint));
    }

    public StringBuilder append(char c) {
        ensure(count + 1);
        value[count++] = c;
        return this;
    }

    public StringBuilder append(int i) {
        return append(Integer.toString(i));
    }

    public StringBuilder append(long lng) {
        return append(Long.toString(lng));
    }

    public StringBuilder append(float f) {
        return append(String.valueOf(f));
    }

    public StringBuilder append(double d) {
        return append(String.valueOf(d));
    }

    public StringBuilder delete(int start, int end) {
        if (end > count) end = count;
        if (start < 0 || start > end || end > count) throw new StringIndexOutOfBoundsException(String.range(start, end, count));
        int len = end - start;
        if (len > 0) {
            for (int i = end; i < count; i++) value[i - len] = value[i];
            count -= len;
        }
        return this;
    }

    public StringBuilder deleteCharAt(int index) {
        if (index < 0 || index >= count) throw new StringIndexOutOfBoundsException(String.outOfBounds(index, count));
        return delete(index, index + 1);
    }

    public StringBuilder replace(int start, int end, String str) {
        if (end > count) end = count;
        if (start < 0 || start > end || end > count) throw new StringIndexOutOfBoundsException(String.range(start, end, count));
        String tail = new String(value, end, count - end);
        count = start;
        append(str);
        return append(tail);
    }

    public StringBuilder insert(int offset, String str) {
        if (offset < 0 || offset > count) throw new StringIndexOutOfBoundsException(String.range(offset, count, count));
        String tail = new String(value, offset, count - offset);
        count = offset;
        append(str);
        return append(tail);
    }

    public StringBuilder insert(int offset, Object obj) {
        return insert(offset, String.valueOf(obj));
    }

    public StringBuilder insert(int offset, char c) {
        return insert(offset, String.valueOf(c));
    }

    public StringBuilder insert(int offset, boolean b) {
        return insert(offset, String.valueOf(b));
    }

    public StringBuilder insert(int offset, int i) {
        return insert(offset, String.valueOf(i));
    }

    public StringBuilder insert(int offset, long l) {
        return insert(offset, String.valueOf(l));
    }

    public StringBuilder insert(int offset, float f) {
        return insert(offset, String.valueOf(f));
    }

    public StringBuilder insert(int offset, double d) {
        return insert(offset, String.valueOf(d));
    }

    public int indexOf(String str) {
        return toString().indexOf(str);
    }

    public int indexOf(String str, int fromIndex) {
        return toString().indexOf(str, fromIndex);
    }

    public int lastIndexOf(String str) {
        String s = toString();
        for (int i = count - str.length(); i >= 0; i--) if (s.startsWith(str, i)) return i;
        return -1;
    }

    /** As the JDK's: a surrogate pair stays in its order. */
    public StringBuilder reverse() {
        for (int i = 0, j = count - 1; i < j; i++, j--) {
            char c = value[i];
            value[i] = value[j];
            value[j] = c;
        }
        for (int i = 0; i < count - 1; i++) {
            if (Character.isLowSurrogate(value[i]) && Character.isHighSurrogate(value[i + 1])) {
                char c = value[i];
                value[i] = value[i + 1];
                value[i + 1] = c;
                i++;
            }
        }
        return this;
    }

    public String substring(int start) {
        return substring(start, count);
    }

    public String substring(int start, int end) {
        if (start < 0 || start > end || end > count) throw new StringIndexOutOfBoundsException(String.range(start, end, count));
        return new String(value, start, end - start);
    }

    public int compareTo(StringBuilder another) {
        return toString().compareTo(another.toString());
    }

    public String toString() {
        return new String(value, 0, count);
    }
}
