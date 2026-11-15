package java.util;

/** The model of java.util.StringJoiner: a prefix, the elements joined by the delimiter, a suffix. */
public final class StringJoiner {
    private final String prefix;
    private final String delimiter;
    private final String suffix;
    private StringBuilder joined;
    private String emptyValue;

    public StringJoiner(CharSequence delimiter) {
        this(delimiter, "", "");
    }

    public StringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        Objects.requireNonNull(prefix, "The prefix must not be null");
        Objects.requireNonNull(delimiter, "The delimiter must not be null");
        Objects.requireNonNull(suffix, "The suffix must not be null");
        this.prefix = prefix.toString();
        this.delimiter = delimiter.toString();
        this.suffix = suffix.toString();
    }

    public StringJoiner setEmptyValue(CharSequence emptyValue) {
        this.emptyValue = Objects.requireNonNull(emptyValue, "The empty value must not be null").toString();
        return this;
    }

    public String toString() {
        if (joined == null && emptyValue != null) return emptyValue;
        return prefix.concat(joined == null ? "" : joined.toString()).concat(suffix);
    }

    public StringJoiner add(CharSequence newElement) {
        String elt = String.valueOf(newElement);
        if (joined == null) {
            joined = new StringBuilder();
        } else {
            joined.append(delimiter);
        }
        joined.append(elt);
        return this;
    }

    public StringJoiner merge(StringJoiner other) {
        Objects.requireNonNull(other);
        if (other.joined == null) return this;
        String part = other.joined.toString();
        if (joined == null) {
            joined = new StringBuilder();
        } else {
            joined.append(delimiter);
        }
        joined.append(part);
        return this;
    }

    public int length() {
        return joined == null && emptyValue != null ? emptyValue.length() : prefix.length() + (joined == null ? 0 : joined.length()) + suffix.length();
    }
}
