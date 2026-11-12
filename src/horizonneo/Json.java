package horizonneo;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/** Writes JSON: maps, collections, text, numbers, booleans and null, indented by two spaces. */
final class Json {
    private Json() {}

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
