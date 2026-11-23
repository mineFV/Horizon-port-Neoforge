package java.text;

/**
 * The model of java.text.DecimalFormat, as a locale's integer and number instances are: its affixes, grouping,
 * symbols and fraction digits from the JDK's data for the locale (numbers.rs, which build.sh writes). A long's text is
 * its digits grouped; a double's (observed against the JDK 21 and 25, lib/README.md) is Double.toString's digits
 * rounded half-even to the fraction digits, a 5 that is their last digit rounded by the double's exact value; -0 and
 * what rounds to zero below it keep their sign. From 1e16 up the JDK's digits aren't Double.toString's: refused.
 * equals is the same locale and instance; hashCode, observed, 423132 and the positive prefix's. Not modelled:
 * patterns of one's own, the symbols' object, the setters, parsing.
 */
public class DecimalFormat extends NumberFormat {
    private static final int PATTERN = 0, POSITIVE_PREFIX = 1, POSITIVE_SUFFIX = 2, NEGATIVE_PREFIX = 3, NEGATIVE_SUFFIX = 4,
            GROUPING = 5, MAXIMUM_FRACTION = 6, ZERO = 7, GROUPING_SEPARATOR = 8, DECIMAL_SEPARATOR = 9, INFINITY = 10, NAN = 11;

    /** The locale's key, or "!" and why it can't be told. */
    private final String key;
    private final boolean integer;
    /** The fields of the instance, or null where the model has no data (refused when used). */
    private final String[] fields;

    DecimalFormat(String key, boolean integer) {
        this.key = key;
        this.integer = integer;
        String record = key.startsWith("!") ? null : jData(key);
        this.fields = record == null ? null : split(split(record, '\u0001')[integer ? 0 : 1], '\u0002');
    }

    private static String[] split(String text, char separator) {
        java.util.ArrayList<String> parts = new java.util.ArrayList<>();
        int start = 0;
        for (int i = text.indexOf(separator); i >= 0; i = text.indexOf(separator, start)) {
            parts.add(text.substring(start, i));
            start = i + 1;
        }
        parts.add(text.substring(start));
        return parts.toArray(new String[0]);
    }

    private String[] fields() {
        if (fields != null) return fields;
        if (key.startsWith("!")) {
            throw new UnsupportedOperationException("the default locale's number format can't be told (horizon-port-neoforge): " + key.substring(1));
        }
        throw new UnsupportedOperationException("the number format of the locale " + key
                + " isn't in the model: the JDK has none, or Java 21's and 25's differ (horizon-port-neoforge)");
    }

    public String getPositivePrefix() {
        return fields()[POSITIVE_PREFIX];
    }

    public String getPositiveSuffix() {
        return fields()[POSITIVE_SUFFIX];
    }

    public String getNegativePrefix() {
        return fields()[NEGATIVE_PREFIX];
    }

    public String getNegativeSuffix() {
        return fields()[NEGATIVE_SUFFIX];
    }

    public String toPattern() {
        return fields()[PATTERN];
    }

    /** Its grouping size: the JDK's data has one where grouping is used. */
    public int getGroupingSize() {
        int size = Integer.parseInt(fields()[GROUPING]);
        if (size == 0) throw new UnsupportedOperationException("the grouping size of a format that doesn't group isn't in the model (horizon-port-neoforge)");
        return size;
    }

    public int getMultiplier() {
        fields();
        return 1;
    }

    public boolean isDecimalSeparatorAlwaysShown() {
        fields();
        return false;
    }

    @Override
    public boolean isGroupingUsed() {
        return Integer.parseInt(fields()[GROUPING]) > 0;
    }

    @Override
    public boolean isParseIntegerOnly() {
        fields();
        return integer;
    }

    @Override
    public int getMaximumIntegerDigits() {
        fields();
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMinimumIntegerDigits() {
        fields();
        return 1;
    }

    @Override
    public int getMaximumFractionDigits() {
        return Integer.parseInt(fields()[MAXIMUM_FRACTION]);
    }

    @Override
    public int getMinimumFractionDigits() {
        fields();
        return 0;
    }

    public Object clone() {
        return new DecimalFormat(key, integer);
    }

    public boolean equals(Object obj) {
        return obj instanceof DecimalFormat other && key.equals(other.key) && integer == other.integer;
    }

    public int hashCode() {
        return 423132 + fields()[POSITIVE_PREFIX].hashCode();
    }

    @Override
    String jText(long number) {
        String[] f = fields();
        boolean negative = number < 0;
        String digits = Long.toString(number);
        return text(f, negative, negative ? digits.substring(1) : digits, "");
    }

    @Override
    String jText(double number) {
        String[] f = fields();
        if (number != number) return f[NAN];
        boolean negative = Double.doubleToRawLongBits(number) < 0;
        double a = Math.abs(number);
        if (Double.isInfinite(a)) return (negative ? f[NEGATIVE_PREFIX] : f[POSITIVE_PREFIX]) + f[INFINITY] + (negative ? f[NEGATIVE_SUFFIX] : f[POSITIVE_SUFFIX]);
        if (a >= 1e16) {
            throw new UnsupportedOperationException("DecimalFormat's digits of a double from 1e16 up aren't Double.toString's (horizon-port-neoforge)");
        }
        // Double.toString's digits, without the point (no zeros at either end), and how many come before it.
        String shown = Double.toString(a);
        int e = shown.indexOf('E');
        String mantissa = e < 0 ? shown : shown.substring(0, e);
        int dot = mantissa.indexOf('.');
        String digits = mantissa.substring(0, dot) + mantissa.substring(dot + 1);
        int point = dot + (e < 0 ? 0 : Integer.parseInt(shown.substring(e + 1)));
        int start = 0;
        while (start < digits.length() && digits.charAt(start) == '0') {
            start++;
            point--;
        }
        int end = digits.length();
        while (end > start && digits.charAt(end - 1) == '0') end--;
        digits = digits.substring(start, end);
        // Rounded half-even to the fraction digits.
        int keep = point + Integer.parseInt(f[MAXIMUM_FRACTION]);
        if (keep < digits.length()) {
            boolean up = false;
            if (keep >= 0) {
                char next = digits.charAt(keep);
                if (next > '5' || (next == '5' && keep + 1 < digits.length())) {
                    up = true;
                } else if (next == '5') {
                    // The 5 is the digits' last: the double's exact value says which way.
                    int scale = digits.length() - point;
                    int c = jCompare(a, Long.parseLong(digits), scale);
                    if (c == 2) throw new UnsupportedOperationException("a double's exact value against its digits couldn't be told (horizon-port-neoforge)");
                    up = c > 0 || (c == 0 && keep > 0 && (digits.charAt(keep - 1) - '0') % 2 == 1);
                }
            }
            String kept = keep > 0 ? digits.substring(0, keep) : "";
            if (up) {
                char[] d = kept.toCharArray();
                int i = d.length - 1;
                while (i >= 0 && d[i] == '9') d[i--] = '0';
                if (i >= 0) {
                    d[i]++;
                    kept = new String(d);
                } else {
                    kept = "1" + new String(d);
                    point++;
                }
            }
            digits = kept;
        }
        // The whole part and the fraction, without the fraction's trailing zeros.
        String whole, fraction;
        if (point <= 0) {
            whole = "0";
            fraction = "0".repeat(-point) + digits;
        } else if (point >= digits.length()) {
            whole = digits + "0".repeat(point - digits.length());
            fraction = "";
        } else {
            whole = digits.substring(0, point);
            fraction = digits.substring(point);
        }
        int last = fraction.length();
        while (last > 0 && fraction.charAt(last - 1) == '0') last--;
        fraction = fraction.substring(0, last);
        if (whole.isEmpty()) whole = "0";
        return text(f, negative, whole, fraction);
    }

    /** The digits in the locale's zero, grouped, its decimal separator before a fraction, and the affixes. */
    private static String text(String[] f, boolean negative, String whole, String fraction) {
        int grouping = Integer.parseInt(f[GROUPING]);
        char zero = f[ZERO].charAt(0);
        StringBuilder b = new StringBuilder();
        b.append(negative ? f[NEGATIVE_PREFIX] : f[POSITIVE_PREFIX]);
        for (int i = 0; i < whole.length(); i++) {
            if (grouping > 0 && i > 0 && (whole.length() - i) % grouping == 0) b.append(f[GROUPING_SEPARATOR]);
            b.append((char) (zero + (whole.charAt(i) - '0')));
        }
        if (!fraction.isEmpty()) {
            b.append(f[DECIMAL_SEPARATOR]);
            for (int i = 0; i < fraction.length(); i++) b.append((char) (zero + (fraction.charAt(i) - '0')));
        }
        b.append(negative ? f[NEGATIVE_SUFFIX] : f[POSITIVE_SUFFIX]);
        return b.toString();
    }

    /** The exact value of a (finite, not negative) against digits × 10^-scale: -1, 0, 1; 2 where it can't be told. */
    static native int jCompare(double a, long digits, int scale);
}
