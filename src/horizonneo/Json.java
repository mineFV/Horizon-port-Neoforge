package horizonneo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON, read and written. Read: objects as Map (in the text's order), arrays as List, text as String,
 * numbers as Long or Double, true, false and null. Written: the same, indented by two spaces.
 */
final class Json {
    private Json() {}

    /** Text that isn't JSON, with the line where it stops making sense. */
    static final class Invalid extends Exception {
        private static final long serialVersionUID = 1L;

        Invalid(String message) {
            super(message);
        }
    }

    // ---- reading ----

    /**
     * Reads one JSON value. Comments and a comma before a closing bracket are let through: Mixin reads
     * its config files that way, so real ones have them.
     */
    static Object parse(String text) throws Invalid {
        Reader reader = new Reader(text.startsWith("﻿") ? text.substring(1) : text);
        reader.skip();
        Object value = reader.value(0);
        reader.skip();
        if (reader.at < reader.s.length()) throw reader.problem("unexpected text after the value");
        return value;
    }

    /** `value` as an object; an empty one when it isn't an object. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    /** `value` as an array; an empty one when it isn't an array. */
    @SuppressWarnings("unchecked")
    static List<Object> array(Object value) {
        return value instanceof List<?> l ? (List<Object>) l : List.of();
    }

    /** The text at `key`, or null when it is missing or isn't text. */
    static String text(Map<String, Object> object, String key) {
        return object.get(key) instanceof String text ? text : null;
    }

    private static final class Reader {
        final String s;
        int at;

        Reader(String s) {
            this.s = s;
        }

        Object value(int depth) throws Invalid {
            if (depth > 200) throw problem("nested too deep");
            if (at >= s.length()) throw problem("a value is missing");
            char c = s.charAt(at);
            if (c == '{') {
                at++;
                Map<String, Object> object = new LinkedHashMap<>();
                while (true) {
                    skip();
                    if (peek() == '}') break;
                    if (peek() != '"') throw problem("expected a name in quotes");
                    String name = string();
                    skip();
                    if (peek() != ':') throw problem("expected `:` after the name `" + name + "`");
                    at++;
                    skip();
                    object.put(name, value(depth + 1));
                    skip();
                    if (peek() == ',') at++;
                    else if (peek() != '}') throw problem("expected `,` or `}`");
                }
                at++;
                return object;
            }
            if (c == '[') {
                at++;
                List<Object> array = new ArrayList<>();
                while (true) {
                    skip();
                    if (peek() == ']') break;
                    array.add(value(depth + 1));
                    skip();
                    if (peek() == ',') at++;
                    else if (peek() != ']') throw problem("expected `,` or `]`");
                }
                at++;
                return array;
            }
            if (c == '"') return string();
            int from = at;
            while (at < s.length() && ",]}/ \t\r\n".indexOf(s.charAt(at)) < 0) at++;
            String word = s.substring(from, at);
            switch (word) {
                case "true" -> {
                    return Boolean.TRUE;
                }
                case "false" -> {
                    return Boolean.FALSE;
                }
                case "null" -> {
                    return null;
                }
                default -> {
                }
            }
            try {
                if (word.matches("-?[0-9]+")) return Long.parseLong(word);
                if (word.matches("-?[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?")) return Double.parseDouble(word);
            } catch (NumberFormatException e) {
                // Falls through to the message below.
            }
            at = from;
            throw problem(word.isEmpty() ? "a value is missing" : "can't read the value `" + (word.length() > 40 ? word.substring(0, 37) + "..." : word) + "`");
        }

        /** A string, from its opening quote. */
        String string() throws Invalid {
            at++;
            StringBuilder b = new StringBuilder();
            while (true) {
                if (at >= s.length() || s.charAt(at) == '\n') throw problem("this text is never closed");
                char c = s.charAt(at++);
                if (c == '"') return b.toString();
                if (c != '\\') {
                    b.append(c);
                    continue;
                }
                if (at >= s.length()) throw problem("a `\\` ends the file");
                char e = s.charAt(at++);
                switch (e) {
                    case '"', '\\', '/' -> b.append(e);
                    case 'b' -> b.append('\b');
                    case 'f' -> b.append('\f');
                    case 'n' -> b.append('\n');
                    case 'r' -> b.append('\r');
                    case 't' -> b.append('\t');
                    case 'u' -> {
                        if (at + 4 > s.length()) throw problem("a `\\u` escape is cut short");
                        try {
                            b.append((char) Integer.parseInt(s.substring(at, at + 4), 16));
                        } catch (NumberFormatException x) {
                            throw problem("`\\u" + s.substring(at, at + 4) + "` isn't a character");
                        }
                        at += 4;
                    }
                    default -> throw problem("`\\" + e + "` isn't an escape JSON has");
                }
            }
        }

        /** Blank space and comments. */
        void skip() throws Invalid {
            while (at < s.length()) {
                char c = s.charAt(at);
                if (c == '/' && s.startsWith("//", at)) {
                    while (at < s.length() && s.charAt(at) != '\n') at++;
                } else if (c == '/' && s.startsWith("/*", at)) {
                    int end = s.indexOf("*/", at + 2);
                    if (end < 0) throw problem("this comment is never closed");
                    at = end + 2;
                } else if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                    at++;
                } else {
                    return;
                }
            }
        }

        char peek() throws Invalid {
            if (at >= s.length()) throw problem("the text ends too soon");
            return s.charAt(at);
        }

        Invalid problem(String message) {
            int line = 1;
            for (int i = 0; i < at && i < s.length(); i++) if (s.charAt(i) == '\n') line++;
            return new Invalid("line " + line + ": " + message);
        }
    }

    // ---- writing ----

    static String write(Object value) {
        StringBuilder b = new StringBuilder();
        put(b, value, 0);
        return b.append('\n').toString();
    }

    private static void put(StringBuilder b, Object value, int depth) {
        if (value == null || value instanceof Boolean) {
            b.append(value);
        } else if (value instanceof Number n) {
            // JSON has no infinity or NaN.
            if (n instanceof Double d && !Double.isFinite(d)) quote(b, d.toString());
            else b.append(n);
        } else if (value instanceof CharSequence text) {
            quote(b, text.toString());
        } else if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                b.append("{}");
                return;
            }
            b.append("{\n");
            for (Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<?, ?> e = it.next();
                indent(b, depth + 1);
                quote(b, String.valueOf(e.getKey()));
                b.append(": ");
                put(b, e.getValue(), depth + 1);
                b.append(it.hasNext() ? ",\n" : "\n");
            }
            indent(b, depth);
            b.append('}');
        } else if (value instanceof Collection<?> items) {
            if (items.isEmpty()) {
                b.append("[]");
                return;
            }
            b.append("[\n");
            for (Iterator<?> it = items.iterator(); it.hasNext(); ) {
                indent(b, depth + 1);
                put(b, it.next(), depth + 1);
                b.append(it.hasNext() ? ",\n" : "\n");
            }
            indent(b, depth);
            b.append(']');
        } else {
            throw new IllegalArgumentException("can't write a " + value.getClass().getName() + " as JSON");
        }
    }

    private static void indent(StringBuilder b, int depth) {
        b.append("  ".repeat(depth));
    }

    private static void quote(StringBuilder b, String text) {
        b.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        b.append('"');
    }
}
