import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Writes what the library model's NumberFormat reads of each locale, as Rust statics, from the JDK that runs it
 * (build.sh runs it with the JDK that builds the tool): the integer and number instances' affixes, grouping and
 * fraction digits, and their symbols. Data of the JDK's CLDR version, no code of the JDK's.
 *
 * A locale's key is "language_script_country". Kept: every locale the JDK has (but those with a variant or
 * extensions), and each language with each ISO country where the JDK's data isn't the language's own: a key that
 * isn't kept, with a country that is an ISO one, has its language's data. A locale whose instances aren't of the
 * shape the model formats (checked by formatting numbers both ways), or whose texts Java 21 and 25 don't agree on,
 * has no data: the model refuses it.
 */
public final class NumberFormats {
    private NumberFormats() {}

    /**
     * Where Java 21 (NeoForge 1.21.1's) and Java 25 give other texts, observed on 2026-11-23 (21.0.13 and 25.0.5) over
     * every key kept and every language with every ISO country: which one a server runs can't be told, so the model
     * refuses them. These languages differ whole (their digits, signs or separators changed); these keys alone else.
     */
    static final Set<String> DIFFER_LANGUAGES = Set.of("am", "ar", "blo", "ff", "ht", "ie", "km", "lij", "lmo", "my", "nds",
            "nqo", "nso", "oc", "sd", "szl", "tn", "tok", "vec", "vmw");
    static final Set<String> DIFFER_KEYS = Set.of("en__CZ", "en__ES", "en__FR", "en__HU", "en__ID", "en__IT", "en__NO", "en__PL",
            "en__PT", "en__RO", "en__SK", "es__JP");

    public static void main(String[] args) throws Exception {
        Map<String, String> data = new TreeMap<>();
        Map<String, Integer> records = new LinkedHashMap<>();
        records.put("", 0);
        Set<String> languages = new TreeSet<>();
        int unsupported = 0;
        for (Locale l : NumberFormat.getAvailableLocales()) {
            if (!l.getVariant().isEmpty() || l.hasExtensions()) continue;
            String k = key(l);
            String r = pair(l);
            if (r == null) unsupported++;
            data.put(k, r);
            if (l.getScript().isEmpty()) languages.add(l.getLanguage());
        }
        // A language with a country the JDK has no locale for: kept where its data isn't the language's.
        int combos = 0;
        for (String language : languages) {
            String own = data.containsKey(language + "__") ? data.get(language + "__") : pair(Locale.of(language));
            for (String country : Locale.getISOCountries()) {
                String k = language + "__" + country;
                if (data.containsKey(k)) continue;
                String r = pair(Locale.of(language, country));
                if (!java.util.Objects.equals(r, own)) {
                    data.put(k, r);
                    combos++;
                }
            }
            if (!data.containsKey(language + "__")) data.put(language + "__", own);
        }
        StringBuilder out = new StringBuilder();
        out.append("// NumberFormat's data as the JDK ").append(System.getProperty("java.version")).append(" that built this tool has it: ")
                .append(data.size()).append(" keys, ").append(combos).append(" of them a language with a country the JDK has no locale for.\n");
        StringBuilder locales = new StringBuilder();
        for (Map.Entry<String, String> e : data.entrySet()) {
            String r = e.getValue();
            if (r == null || DIFFER_KEYS.contains(e.getKey()) || DIFFER_LANGUAGES.contains(e.getKey().substring(0, e.getKey().indexOf('_')))) r = "";
            Integer index = records.get(r);
            if (index == null) {
                index = records.size();
                records.put(r, index);
            }
            locales.append("(\"").append(e.getKey()).append("\", ").append(index).append("), ");
        }
        out.append("/// Each record: the integer instance's fields, then the number instance's, separated by U+0001 (NumberFormat.jData).\n");
        out.append("#[allow(dead_code)]\nstatic JNUM_RECORDS: &[&str] = &[");
        for (String r : records.keySet()) out.append(rust(r)).append(", ");
        out.append("];\n");
        out.append("/// A key and its record (0: none, the model refuses it), sorted by key.\n");
        out.append("#[allow(dead_code)]\nstatic JNUM_LOCALES: &[(&str, u16)] = &[").append(locales).append("];\n");
        out.append("/// The ISO countries: a key with another country has no data here.\n");
        out.append("#[allow(dead_code)]\nstatic JNUM_COUNTRIES: &[&str] = &[");
        for (String c : new TreeSet<>(Set.of(Locale.getISOCountries()))) out.append("\"").append(c).append("\", ");
        out.append("];\n");
        Files.writeString(Path.of(args[0]), out, StandardCharsets.UTF_8);
        if (unsupported > 0) System.err.println("NumberFormats: " + unsupported + " locales of a shape the model doesn't format, refused");
    }

    static String key(Locale l) {
        return l.getLanguage() + "_" + l.getScript() + "_" + l.getCountry();
    }

    /** The integer instance's fields and the number instance's, or null when either isn't of the shape modelled. */
    static String pair(Locale l) {
        String i = fields(NumberFormat.getIntegerInstance(l), 0), n = fields(NumberFormat.getNumberInstance(l), -1);
        return i == null || n == null ? null : i + "\u0001" + n;
    }

    /**
     * pattern, positive prefix, positive suffix, negative prefix, negative suffix, grouping size (0: none), maximum
     * fraction digits, zero digit, grouping separator, decimal separator, infinity, NaN; separated by U+0002.
     */
    static String fields(NumberFormat nf, int fraction) {
        if (!(nf instanceof DecimalFormat f)) return null;
        DecimalFormatSymbols s = f.getDecimalFormatSymbols();
        if (f.getMultiplier() != 1 || f.getRoundingMode() != RoundingMode.HALF_EVEN || f.getMinimumIntegerDigits() != 1
                || f.getMaximumIntegerDigits() != Integer.MAX_VALUE || f.getMinimumFractionDigits() != 0
                || f.isDecimalSeparatorAlwaysShown() || f.toPattern().contains("E")
                || (fraction >= 0 && f.getMaximumFractionDigits() != fraction) || f.getMaximumFractionDigits() > 6) return null;
        int grouping = f.isGroupingUsed() ? f.getGroupingSize() : 0;
        String r = String.join("\u0002", f.toPattern(), f.getPositivePrefix(), f.getPositiveSuffix(), f.getNegativePrefix(),
                f.getNegativeSuffix(), Integer.toString(grouping), Integer.toString(f.getMaximumFractionDigits()),
                String.valueOf(s.getZeroDigit()), String.valueOf(s.getGroupingSeparator()), String.valueOf(s.getDecimalSeparator()),
                s.getInfinity(), s.getNaN());
        return agrees(f, r) ? r : null;
    }

    static final long[] LONGS = {0, 1, -1, 7, 12, 123, 1234, -12345, 123456, -1234567, 9876543210L, Long.MAX_VALUE, Long.MIN_VALUE};
    static final double[] DOUBLES = {0.0, -0.0, 0.5, -0.5, 1.5, 2.5, -2.5, 0.0005, 2.675, 9.9995, -999.9995, 1234.5678,
        -1234567.891, 0.001, 0.1234567, 1e-9, 123456789012.345, 9.999999e15, Double.NaN, Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY};

    /**
     * Whether the fields give the JDK's texts, formatted as the model formats (a check of the data, not the model), and
     * its hash is the one the model gives (observed: 423132 and the positive prefix's).
     */
    static boolean agrees(DecimalFormat f, String record) {
        String[] r = record.split("\u0002", -1);
        if (f.hashCode() != 423132 + r[1].hashCode()) return false;
        for (long v : LONGS) if (!f.format(v).equals(format(r, v < 0, Long.toString(v).replace("-", "")))) return false;
        for (double d : DOUBLES) {
            String expected;
            if (Double.isNaN(d)) expected = r[11];
            else if (Double.isInfinite(d)) expected = (d < 0 ? r[3] : r[1]) + r[10] + (d < 0 ? r[4] : r[2]);
            else expected = format(r, Double.doubleToRawLongBits(d) < 0, rounded(Math.abs(d), Integer.parseInt(r[6])));
            if (!f.format(d).equals(expected)) return false;
        }
        return true;
    }

    /** The shortest digits of a double, rounded half-even to some fraction digits, a final 5 settled by the exact value. */
    static String rounded(double a, int fraction) {
        BigDecimal shortest = new BigDecimal(Double.toString(a));
        BigDecimal scaled = shortest.movePointRight(fraction);
        BigDecimal floor = new BigDecimal(scaled.toBigInteger());
        int c = scaled.subtract(floor).compareTo(new BigDecimal("0.5"));
        int x = new BigDecimal(a).compareTo(shortest);
        boolean up = c > 0 || (c == 0 && (x > 0 || (x == 0 && floor.toBigInteger().testBit(0))));
        BigDecimal r = (up ? floor.add(BigDecimal.ONE) : floor).movePointLeft(fraction).stripTrailingZeros();
        return r.signum() == 0 ? "0" : r.toPlainString();
    }

    /** A plain decimal ("1234.5") with the fields' symbols, grouping and affixes. */
    static String format(String[] r, boolean negative, String plain) {
        int dot = plain.indexOf('.');
        String whole = dot < 0 ? plain : plain.substring(0, dot), fraction = dot < 0 ? "" : plain.substring(dot + 1);
        int grouping = Integer.parseInt(r[5]);
        char zero = r[7].charAt(0);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < whole.length(); i++) {
            if (grouping > 0 && i > 0 && (whole.length() - i) % grouping == 0) b.append(r[8]);
            b.append((char) (zero + (whole.charAt(i) - '0')));
        }
        if (!fraction.isEmpty()) {
            b.append(r[9]);
            for (char c : fraction.toCharArray()) b.append((char) (zero + (c - '0')));
        }
        return (negative ? r[3] : r[1]) + b + (negative ? r[4] : r[2]);
    }

    /** A Rust string literal. */
    static String rust(String s) {
        StringBuilder b = new StringBuilder("\"");
        s.codePoints().forEach(c -> {
            if (c == '"' || c == '\\') b.append('\\').append((char) c);
            else if (c < 0x20 || c > 0x7e) b.append(String.format("\\u{%x}", c));
            else b.append((char) c);
        });
        return b.append('"').toString();
    }
}
