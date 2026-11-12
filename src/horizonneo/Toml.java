package horizonneo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Reads the TOML that mod metadata is written in (META-INF/neoforge.mods.toml): tables and arrays of
 * tables, dotted and quoted keys, the four kinds of string, numbers, booleans, arrays and inline
 * tables. Dates and times are kept as their text.
 *
 * It follows TOML 1.0, except where a real file gains from it being looser: a bare key may hold `$`,
 * `{` and `}`, so a header left over from a build template (`[[dependencies.${mod_id}]]`) still reads,
 * and a key set twice to the same value is a warning, not an error.
 *
 * Values are String, Long, Double, Boolean, List (arrays) and Map (tables, in the file's order).
 */
final class Toml {
    /** A parsed file: its root table, and what was odd in it but harmless. */
    record Doc(Map<String, Object> root, List<String> warnings) {}

    /** Text that isn't TOML, with the line where it stops making sense. */
    static final class Problem extends Exception {
        private static final long serialVersionUID = 1L;

        Problem(int line, String message) {
            super("line " + line + ": " + message);
        }
    }

    private static final Pattern INTEGER = Pattern.compile("[+-]?[0-9]+");
    private static final Pattern RADIX = Pattern.compile("0[xob][0-9a-fA-F]+");
    private static final Pattern FLOAT = Pattern.compile("[+-]?[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?");
    private static final Pattern DATE = Pattern.compile(
            "[0-9]{4}-[0-9]{2}-[0-9]{2}([Tt ][0-9]{2}:[0-9]{2}(:[0-9]{2}(\\.[0-9]+)?)?)?([Zz]|[+-][0-9]{2}:[0-9]{2})?"
                    + "|[0-9]{2}:[0-9]{2}(:[0-9]{2}(\\.[0-9]+)?)?");

    private final String s;
    private int at;
    private int line = 1;
    private final Map<String, Object> root = new LinkedHashMap<>();
    private final List<String> warnings = new ArrayList<>();

    private Toml(String text) {
        // A byte order mark some editors put first.
        this.s = text.startsWith("﻿") ? text.substring(1) : text;
    }

    static Doc parse(String text) throws Problem {
        Toml toml = new Toml(text);
        toml.document();
        return new Doc(toml.root, List.copyOf(toml.warnings));
    }

    // ---- reading what was parsed ----

    /** `value` as a table; an empty one when it isn't a table. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> table(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    /** `value` as an array of tables: the tables in it, in order; none when it isn't an array. */
    static List<Map<String, Object>> tables(Object value) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object o : list) if (o instanceof Map<?, ?>) out.add(table(o));
        }
        return out;
    }

    /** The text at `key`, or null when it is missing or isn't text. */
    static String text(Map<String, Object> table, String key) {
        return table.get(key) instanceof String text ? text : null;
    }

    /** `value` as a list of texts: the strings in it; none when it isn't an array. */
    static List<String> texts(Object value) {
        List<String> out = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object o : list) if (o instanceof String text) out.add(text);
        }
        return out;
    }

    // ---- the file ----

    private void document() throws Problem {
        Map<String, Object> table = root;
        while (true) {
            skipBlank();
            if (at >= s.length()) return;
            if (s.charAt(at) == '[') table = header();
            else pair(table);
            endOfStatement();
        }
    }

    /** `[a.b]` or `[[a.b]]`: the table the lines after it fill. */
    private Map<String, Object> header() throws Problem {
        boolean array = s.startsWith("[[", at);
        String close = array ? "]]" : "]";
        at += close.length();
        List<String> path = keyPath();
        if (!s.startsWith(close, at)) throw problem("expected `" + close + "` to close the table's name");
        at += close.length();

        Map<String, Object> parent = root;
        for (String name : path.subList(0, path.size() - 1)) parent = child(parent, name);
        String name = path.get(path.size() - 1);
        Object existing = parent.get(name);
        if (!array) {
            if (existing != null && !(existing instanceof Map<?, ?>)) throw problem("`" + name + "` already has a value, so it can't be a table");
            return child(parent, name);
        }
        List<Object> list;
        if (existing == null) {
            list = new ArrayList<>();
            parent.put(name, list);
        } else if (existing instanceof List<?> && tables(existing).size() == ((List<?>) existing).size()) {
            list = items(existing);
        } else {
            throw problem("`" + name + "` already has a value, so it can't be an array of tables");
        }
        Map<String, Object> made = new LinkedHashMap<>();
        list.add(made);
        return made;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> items(Object list) {
        return (List<Object>) list;
    }

    /** The table `name` in `parent`, made if it is missing; for an array of tables, its last one. */
    private Map<String, Object> child(Map<String, Object> parent, String name) throws Problem {
        Object existing = parent.get(name);
        if (existing == null) {
            Map<String, Object> made = new LinkedHashMap<>();
            parent.put(name, made);
            return made;
        }
        if (existing instanceof Map<?, ?>) return table(existing);
        if (existing instanceof List<?> list && !list.isEmpty() && list.get(list.size() - 1) instanceof Map<?, ?> last) return table(last);
        throw problem("`" + name + "` is a value, so nothing can be nested in it");
    }

    /** `key = value`, into `table` (through the tables a dotted key names). */
    private void pair(Map<String, Object> table) throws Problem {
        int keyLine = line;
        List<String> path = keyPath();
        if (at >= s.length() || s.charAt(at) != '=') throw problem("expected `=` after the key `" + String.join(".", path) + "`");
        at++;
        skipInline();
        Object value = value();
        Map<String, Object> into = table;
        for (String name : path.subList(0, path.size() - 1)) into = child(into, name);
        String key = path.get(path.size() - 1);
        if (!into.containsKey(key)) {
            into.put(key, value);
        } else if (Objects.equals(into.get(key), value)) {
            warnings.add("line " + keyLine + ": `" + key + "` is set twice, to the same value");
        } else {
            throw new Problem(keyLine, "`" + key + "` is set twice, to different values");
        }
    }

    // ---- keys ----

    /** One key, or several joined by dots; blank space around each part is skipped. */
    private List<String> keyPath() throws Problem {
        List<String> parts = new ArrayList<>();
        while (true) {
            skipInline();
            parts.add(keyPart());
            skipInline();
            if (at >= s.length() || s.charAt(at) != '.') return parts;
            at++;
        }
    }

    private String keyPart() throws Problem {
        if (at >= s.length()) throw problem("a key is missing");
        char c = s.charAt(at);
        if (c == '"') {
            at++;
            return basic(false);
        }
        if (c == '\'') {
            at++;
            return literal(false);
        }
        int from = at;
        while (at < s.length() && bare(s.charAt(at))) at++;
        if (at == from) throw problem("a key is missing");
        return s.substring(from, at);
    }

    /** What a key without quotes may hold: anything that doesn't end a key or start something else. */
    private static boolean bare(char c) {
        return !Character.isWhitespace(c) && ".=[]\"'#,".indexOf(c) < 0;
    }

    // ---- values ----

    private Object value() throws Problem {
        if (at >= s.length()) throw problem("a value is missing");
        switch (s.charAt(at)) {
            case '"' -> {
                boolean multi = s.startsWith("\"\"\"", at);
                at += multi ? 3 : 1;
                return basic(multi);
            }
            case '\'' -> {
                boolean multi = s.startsWith("'''", at);
                at += multi ? 3 : 1;
                return literal(multi);
            }
            case '[' -> {
                at++;
                return array();
            }
            case '{' -> {
                at++;
                return inlineTable();
            }
            default -> {
                return scalar();
            }
        }
    }

    /** A "basic" string, after its opening quotes: escapes apply. */
    private String basic(boolean multi) throws Problem {
        int opened = line;
        StringBuilder b = new StringBuilder();
        if (multi) skipFirstLineEnd();
        while (true) {
            if (at >= s.length()) throw new Problem(opened, "this string is never closed");
            char c = s.charAt(at);
            if (c == '"') {
                if (!multi) {
                    at++;
                    return b.toString();
                }
                if (closes('"', b)) return b.toString();
            } else if (c == '\\') {
                escape(b, multi);
            } else {
                plain(c, b, multi, opened);
            }
        }
    }

    /** A 'literal' string, after its opening quotes: every character stands for itself. */
    private String literal(boolean multi) throws Problem {
        int opened = line;
        StringBuilder b = new StringBuilder();
        if (multi) skipFirstLineEnd();
        while (true) {
            if (at >= s.length()) throw new Problem(opened, "this string is never closed");
            char c = s.charAt(at);
            if (c == '\'') {
                if (!multi) {
                    at++;
                    return b.toString();
                }
                if (closes('\'', b)) return b.toString();
            } else {
                plain(c, b, multi, opened);
            }
        }
    }

    /**
     * At a run of quotes inside a multi-line string. Three or more close it: the last three are the
     * closing mark, and up to two before them are part of the text. Fewer are text.
     */
    private boolean closes(char quote, StringBuilder b) {
        int run = 0;
        while (at + run < s.length() && s.charAt(at + run) == quote) run++;
        if (run < 3) {
            b.append(String.valueOf(quote).repeat(run));
            at += run;
            return false;
        }
        int kept = Math.min(run - 3, 2);
        b.append(String.valueOf(quote).repeat(kept));
        at += kept + 3;
        return true;
    }

    /** One ordinary character of a string. A line end is text only in a multi-line string, as `\n`. */
    private void plain(char c, StringBuilder b, boolean multi, int opened) throws Problem {
        if (c == '\n') {
            if (!multi) throw new Problem(opened, "this string is never closed");
            line++;
        }
        if (c != '\r' || at + 1 >= s.length() || s.charAt(at + 1) != '\n') b.append(c);
        at++;
    }

    /** A line end right after the opening quotes of a multi-line string isn't part of it. */
    private void skipFirstLineEnd() {
        if (s.startsWith("\r\n", at)) at++;
        if (at < s.length() && s.charAt(at) == '\n') {
            at++;
            line++;
        }
    }

    private void escape(StringBuilder b, boolean multi) throws Problem {
        at++;
        if (at >= s.length()) throw problem("a `\\` ends the file");
        if (multi && joinsLines()) return;
        char c = s.charAt(at++);
        switch (c) {
            case 'b' -> b.append('\b');
            case 't' -> b.append('\t');
            case 'n' -> b.append('\n');
            case 'f' -> b.append('\f');
            case 'r' -> b.append('\r');
            case 'e' -> b.append((char) 0x1B);
            case '"' -> b.append('"');
            case '\\' -> b.append('\\');
            case 'x' -> b.appendCodePoint(hex(2));
            case 'u' -> b.appendCodePoint(hex(4));
            case 'U' -> b.appendCodePoint(hex(8));
            default -> throw problem("`\\" + c + "` isn't an escape TOML has");
        }
    }

    /** In a multi-line string, a `\` that ends its line joins the next: the blank space up to the next character goes. */
    private boolean joinsLines() {
        int p = at;
        while (p < s.length() && (s.charAt(p) == ' ' || s.charAt(p) == '\t' || s.charAt(p) == '\r')) p++;
        if (p >= s.length() || s.charAt(p) != '\n') return false;
        while (p < s.length() && Character.isWhitespace(s.charAt(p))) {
            if (s.charAt(p) == '\n') line++;
            p++;
        }
        at = p;
        return true;
    }

    private int hex(int digits) throws Problem {
        if (at + digits > s.length()) throw problem("an escape is cut short");
        long code = 0;
        for (int i = 0; i < digits; i++) {
            int d = Character.digit(s.charAt(at + i), 16);
            if (d < 0) throw problem("`" + s.substring(at, at + digits) + "` isn't a hexadecimal number");
            code = code * 16 + d;
        }
        if (code > Character.MAX_CODE_POINT || (code >= 0xD800 && code <= 0xDFFF)) throw problem("that escape isn't a character");
        at += digits;
        return (int) code;
    }

    private List<Object> array() throws Problem {
        List<Object> out = new ArrayList<>();
        while (true) {
            skipBlank();
            if (at >= s.length()) throw problem("this array is never closed");
            if (s.charAt(at) == ']') break;
            out.add(value());
            skipBlank();
            if (at >= s.length()) throw problem("this array is never closed");
            if (s.charAt(at) == ',') at++;
            else if (s.charAt(at) != ']') throw problem("expected `,` or `]` in the array");
        }
        at++;
        return out;
    }

    private Map<String, Object> inlineTable() throws Problem {
        Map<String, Object> out = new LinkedHashMap<>();
        while (true) {
            skipBlank();
            if (at >= s.length()) throw problem("this inline table is never closed");
            if (s.charAt(at) == '}') break;
            pair(out);
            skipBlank();
            if (at >= s.length()) throw problem("this inline table is never closed");
            if (s.charAt(at) == ',') at++;
            else if (s.charAt(at) != '}') throw problem("expected `,` or `}` in the inline table");
        }
        at++;
        return out;
    }

    /** A value without quotes or brackets: true, false, a number, or a date (kept as text). */
    private Object scalar() throws Problem {
        int from = at;
        while (at < s.length() && ",]}#\n\r".indexOf(s.charAt(at)) < 0) at++;
        String word = s.substring(from, at).strip();
        if (word.isEmpty()) throw problem("a value is missing");
        switch (word) {
            case "true" -> {
                return Boolean.TRUE;
            }
            case "false" -> {
                return Boolean.FALSE;
            }
            case "inf", "+inf" -> {
                return Double.POSITIVE_INFINITY;
            }
            case "-inf" -> {
                return Double.NEGATIVE_INFINITY;
            }
            case "nan", "+nan", "-nan" -> {
                return Double.NaN;
            }
            default -> {
            }
        }
        String digits = word.replace("_", "");
        try {
            if (RADIX.matcher(digits).matches()) {
                int radix = switch (digits.charAt(1)) {
                    case 'x' -> 16;
                    case 'o' -> 8;
                    default -> 2;
                };
                return Long.parseLong(digits.substring(2), radix);
            }
            if (INTEGER.matcher(digits).matches()) return Long.parseLong(digits);
            if (FLOAT.matcher(digits).matches()) return Double.parseDouble(digits);
        } catch (NumberFormatException e) {
            throw problem("can't read the number `" + word + "`");
        }
        if (DATE.matcher(word).matches()) return word;
        throw problem("can't read the value `" + (word.length() > 40 ? word.substring(0, 37) + "..." : word) + "` (text needs quotes)");
    }

    // ---- between things ----

    /** Spaces, tabs, line ends and comments. */
    private void skipBlank() {
        while (at < s.length()) {
            char c = s.charAt(at);
            if (c == '#') {
                skipComment();
                continue;
            }
            if (c == '\n') line++;
            else if (c != ' ' && c != '\t' && c != '\r') return;
            at++;
        }
    }

    private void skipInline() {
        while (at < s.length() && (s.charAt(at) == ' ' || s.charAt(at) == '\t')) at++;
    }

    private void skipComment() {
        while (at < s.length() && s.charAt(at) != '\n') at++;
    }

    /** After a header or a pair, only a comment may follow on its line. */
    private void endOfStatement() throws Problem {
        skipInline();
        if (at < s.length() && s.charAt(at) == '#') skipComment();
        if (at < s.length() && s.charAt(at) == '\r') at++;
        if (at >= s.length()) return;
        if (s.charAt(at) != '\n') throw problem("unexpected `" + s.charAt(at) + "` after the value");
        at++;
        line++;
    }

    private Problem problem(String message) {
        return new Problem(line, message);
    }
}
