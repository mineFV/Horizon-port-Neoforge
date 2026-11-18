package net.neoforged.neoforge.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Not the game's: a config's file as NeoForge writes a new one (each table's values, then its sub-tables, indented
 * with tabs; a value's comment, its default and range, an enum's allowed values) and reads one (TOML's tables and
 * its values: booleans, numbers, texts, lists). Observed (`standins --horizon-src`).
 */
public final class HorizonConfigToml {
    private HorizonConfigToml() {}

    // ---- writing ----

    /** A table: its values, then its sub-tables, in the order they were defined. */
    private static final class Table {
        final List<String> path;
        final List<ModConfigSpec.ConfigValue<?>> values = new ArrayList<>();
        final Map<String, Table> tables = new LinkedHashMap<>();

        Table(List<String> path) {
            this.path = path;
        }
    }

    static String write(ModConfigSpec spec) {
        Table root = new Table(List.of());
        for (ModConfigSpec.ConfigValue<?> v : spec.values) {
            Table t = root;
            for (int i = 0; i < v.path.size() - 1; i++) {
                List<String> at = v.path.subList(0, i + 1);
                t = t.tables.computeIfAbsent(v.path.get(i), k -> new Table(List.copyOf(at)));
            }
            t.values.add(v);
        }
        StringBuilder out = new StringBuilder();
        writeTable(root, spec, out);
        return out.toString();
    }

    private static void writeTable(Table t, ModConfigSpec spec, StringBuilder out) {
        int depth = t.path.size();
        if (depth > 0 && !t.values.isEmpty()) {
            if (out.length() > 0) out.append('\n');
            String comment = spec.levelComments.get(t.path);
            if (comment != null) for (String line : comment.split("\n", -1)) out.append(tabs(depth - 1)).append('#').append(line).append('\n');
            out.append(tabs(depth - 1)).append('[').append(String.join(".", t.path)).append("]\n");
        }
        for (ModConfigSpec.ConfigValue<?> v : t.values) {
            for (String line : comment(v)) out.append(tabs(depth)).append('#').append(line).append('\n');
            out.append(tabs(depth)).append(v.path.get(v.path.size() - 1)).append(" = ").append(value(v.defaultValue.get())).append('\n');
        }
        for (Table sub : t.tables.values()) writeTable(sub, spec, out);
    }

    private static String tabs(int n) {
        return "\t".repeat(n);
    }

    /** Its comment's lines: the mod's, then its default and range, or an enum's allowed values. */
    private static List<String> comment(ModConfigSpec.ConfigValue<?> v) {
        List<String> lines = new ArrayList<>();
        if (v.comment != null) for (String line : v.comment.split("\n", -1)) lines.add(line);
        if (v.range != null) {
            lines.add(" Default: " + v.defaultValue.get());
            lines.add(" Range: " + range(v.range[0], v.range[1]));
        }
        if (v instanceof ModConfigSpec.EnumValue<?> e) {
            StringBuilder b = new StringBuilder("Allowed Values: ");
            Object[] constants = ((Enum<?>) e.defaultValue.get()).getDeclaringClass().getEnumConstants();
            for (int i = 0; i < constants.length; i++) b.append(i == 0 ? "" : ", ").append(((Enum<?>) constants[i]).name());
            lines.add(b.toString());
        }
        return lines;
    }

    /** Observed: an int range open at one end says "< max" or "> min"; a long's or a double's says both ends. */
    private static String range(Object min, Object max) {
        boolean noMin = min.equals(Integer.MIN_VALUE);
        boolean noMax = max.equals(Integer.MAX_VALUE);
        if (noMin && !noMax) return "< " + max;
        if (noMax && !noMin) return "> " + min;
        return min + " ~ " + max;
    }

    private static String value(Object o) {
        if (o instanceof String s) return quoted(s);
        if (o instanceof Enum<?> e) return quoted(e.name());
        if (o instanceof List<?> list) {
            StringBuilder b = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) b.append(i == 0 ? "" : ", ").append(value(list.get(i)));
            return b.append(']').toString();
        }
        return String.valueOf(o);
    }

    private static String quoted(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\t' -> b.append("\\t");
                case '\r' -> b.append("\\r");
                default -> b.append(c);
            }
        }
        return b.append('"').toString();
    }

    // ---- reading ----

    /** Each value's path and what it reads as (Boolean, Integer or Long, Double, String, List); null when the text isn't TOML it reads. */
    static Map<List<String>, Object> read(String text) {
        Map<List<String>, Object> out = new LinkedHashMap<>();
        List<String> table = List.of();
        for (String raw : text.split("\n", -1)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("[")) {
                int end = line.indexOf(']');
                if (end < 0) return null;
                table = List.of(line.substring(1, end).strip().split("\\."));
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) return null;
            List<String> path = new ArrayList<>(table);
            for (String part : line.substring(0, eq).strip().split("\\.")) path.add(part.strip());
            int[] at = {eq + 1};
            Object value = parse(line, at);
            if (value == null) return null;
            out.put(List.copyOf(path), value);
        }
        return out;
    }

    private static void space(String s, int[] at) {
        while (at[0] < s.length() && (s.charAt(at[0]) == ' ' || s.charAt(at[0]) == '\t')) at[0]++;
    }

    private static Object parse(String s, int[] at) {
        space(s, at);
        if (at[0] >= s.length()) return null;
        char c = s.charAt(at[0]);
        if (c == '"') {
            StringBuilder b = new StringBuilder();
            at[0]++;
            while (at[0] < s.length() && s.charAt(at[0]) != '"') {
                char d = s.charAt(at[0]++);
                if (d == '\\' && at[0] < s.length()) {
                    char e = s.charAt(at[0]++);
                    b.append(switch (e) {
                        case 'n' -> '\n';
                        case 't' -> '\t';
                        case 'r' -> '\r';
                        default -> e;
                    });
                } else {
                    b.append(d);
                }
            }
            if (at[0] >= s.length()) return null;
            at[0]++;
            return b.toString();
        }
        if (c == '[') {
            at[0]++;
            List<Object> list = new ArrayList<>();
            space(s, at);
            if (at[0] < s.length() && s.charAt(at[0]) == ']') {
                at[0]++;
                return list;
            }
            while (true) {
                Object e = parse(s, at);
                if (e == null) return null;
                list.add(e);
                space(s, at);
                if (at[0] >= s.length()) return null;
                char d = s.charAt(at[0]++);
                if (d == ']') return list;
                if (d != ',') return null;
            }
        }
        int start = at[0];
        while (at[0] < s.length() && ",] \t#".indexOf(s.charAt(at[0])) < 0) at[0]++;
        String word = s.substring(start, at[0]);
        if (word.equals("true")) return Boolean.TRUE;
        if (word.equals("false")) return Boolean.FALSE;
        return number(word);
    }

    /** A TOML number: an integer (an Integer when it fits, else a Long) or a float; null when it isn't one. */
    private static Object number(String word) {
        String w = word.replace("_", "");
        if (w.isEmpty()) return null;
        boolean negative = w.charAt(0) == '-';
        int i = w.charAt(0) == '-' || w.charAt(0) == '+' ? 1 : 0;
        if (i >= w.length()) return null;
        boolean integer = true;
        for (int k = i; k < w.length(); k++) if (w.charAt(k) < '0' || w.charAt(k) > '9') integer = false;
        if (integer) {
            long v = 0;
            for (int k = i; k < w.length(); k++) {
                int d = w.charAt(k) - '0';
                if (v > (Long.MAX_VALUE - d) / 10) return null;
                v = v * 10 + d;
            }
            if (negative) v = -v;
            return v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE ? (Object) (int) v : (Object) v;
        }
        return decimal(w, word);
    }

    /**
     * A float in decimal (digits, a point, an exponent), exactly as Double.parseDouble gives it where its digits and its
     * exponent are small enough for one exact multiplication or division (Clinger's fast path: up to 15 digits, a power
     * of ten up to 22); another isn't modelled and says so. Not a number at all: null.
     */
    private static Object decimal(String w, String word) {
        int i = 0;
        boolean negative = false;
        if (w.charAt(0) == '-' || w.charAt(0) == '+') {
            negative = w.charAt(0) == '-';
            i = 1;
        }
        long mantissa = 0;
        int digits = 0, scale = 0;
        boolean seenDigit = false, seenPoint = false;
        for (; i < w.length(); i++) {
            char c = w.charAt(i);
            if (c >= '0' && c <= '9') {
                seenDigit = true;
                if (mantissa == 0 && c == '0') {
                    if (seenPoint) scale--;
                    continue;
                }
                if (digits >= 15) throw new UnsupportedOperationException("reading this config number isn't modelled (horizon-port-neoforge): " + word);
                mantissa = mantissa * 10 + (c - '0');
                digits++;
                if (seenPoint) scale--;
            } else if (c == '.' && !seenPoint) {
                seenPoint = true;
            } else {
                break;
            }
        }
        if (!seenDigit) return null;
        if (i < w.length()) {
            char c = w.charAt(i);
            if (c != 'e' && c != 'E') return null;
            i++;
            boolean negativeExponent = false;
            if (i < w.length() && (w.charAt(i) == '-' || w.charAt(i) == '+')) negativeExponent = w.charAt(i++) == '-';
            if (i >= w.length()) return null;
            int exponent = 0;
            for (; i < w.length(); i++) {
                char d = w.charAt(i);
                if (d < '0' || d > '9') return null;
                exponent = exponent * 10 + (d - '0');
                if (exponent > 400) throw new UnsupportedOperationException("reading this config number isn't modelled (horizon-port-neoforge): " + word);
            }
            scale += negativeExponent ? -exponent : exponent;
        }
        double value;
        if (mantissa == 0) value = 0.0;
        else if (scale >= 0 && scale <= 22) value = mantissa * POWERS[scale];
        else if (scale < 0 && scale >= -22) value = mantissa / POWERS[-scale];
        else throw new UnsupportedOperationException("reading this config number isn't modelled (horizon-port-neoforge): " + word);
        return negative ? -value : value;
    }

    private static final double[] POWERS = {1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16, 1e17, 1e18, 1e19, 1e20, 1e21, 1e22};

    /** A value read, as a value of its default's kind takes it (a long from an integer, a double from a number, an enum from its name). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object convert(Object read, Object fallback) {
        if (read == null) return null;
        if (fallback instanceof Long && read instanceof Integer i) return (long) i;
        if (fallback instanceof Double && read instanceof Integer i) return (double) i;
        if (fallback instanceof Double && read instanceof Long l) return (double) l;
        if (fallback instanceof Enum<?> e && read instanceof String name) {
            for (Object c : e.getDeclaringClass().getEnumConstants()) if (((Enum<?>) c).name().equalsIgnoreCase(name)) return c;
            return null;
        }
        return read;
    }
}
