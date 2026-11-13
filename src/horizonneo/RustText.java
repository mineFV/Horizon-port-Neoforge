package horizonneo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What can be told of a Rust file by reading it, without a compiler: whether its brackets pair up,
 * and the functions it holds outside any block. Enough to see that a bundle's files are whole; it
 * says nothing of types or names.
 */
final class RustText {
    /** A function outside any block: its name and how many parameters it takes. */
    record Function(String name, int parameters, int line) {}

    private static final Pattern FN = Pattern.compile("\\bfn\\s+([A-Za-z_][A-Za-z0-9_]*)");

    private RustText() {}

    /**
     * The text with its comments and the insides of its string and character literals blanked: the
     * same length, the same lines, so a place in one is the same place in the other.
     */
    static String code(String text) {
        char[] out = text.toCharArray();
        int n = out.length, i = 0;
        while (i < n) {
            char c = out[i];
            if (c == '/' && i + 1 < n && out[i + 1] == '/') {
                while (i < n && out[i] != '\n') out[i++] = ' ';
            } else if (c == '/' && i + 1 < n && out[i + 1] == '*') {
                int depth = 0;
                while (i < n) {
                    if (out[i] == '/' && i + 1 < n && out[i + 1] == '*') {
                        depth++;
                        out[i] = out[i + 1] = ' ';
                        i += 2;
                    } else if (out[i] == '*' && i + 1 < n && out[i + 1] == '/') {
                        depth--;
                        out[i] = out[i + 1] = ' ';
                        i += 2;
                        if (depth == 0) break;
                    } else {
                        if (out[i] != '\n') out[i] = ' ';
                        i++;
                    }
                }
            } else if (c == '"') {
                i = string(out, i + 1);
            } else if (c == 'r' && !word(out, i - 1) && raw(out, i + 1) >= 0) {
                i = rawString(out, i + 1);
            } else if ((c == 'b' || c == 'c') && !word(out, i - 1) && i + 1 < n && out[i + 1] == '"') {
                i = string(out, i + 2);
            } else if (c == 'b' && !word(out, i - 1) && i + 1 < n && out[i + 1] == 'r' && raw(out, i + 2) >= 0) {
                i = rawString(out, i + 2);
            } else if (c == '\'') {
                i = quote(out, i);
            } else {
                i++;
            }
        }
        return new String(out);
    }

    /** Blanks a string from just after its opening quote; returns the place after its closing one. */
    private static int string(char[] s, int i) {
        while (i < s.length && s[i] != '"') {
            if (s[i] == '\\' && i + 1 < s.length) {
                if (s[i + 1] != '\n') s[i + 1] = ' ';
                s[i] = ' ';
                i += 2;
            } else {
                if (s[i] != '\n') s[i] = ' ';
                i++;
            }
        }
        return Math.min(i + 1, s.length);
    }

    /** How many `#` come before the quote of a raw string starting here; -1 when this isn't one. */
    private static int raw(char[] s, int i) {
        int hashes = 0;
        while (i < s.length && s[i] == '#') {
            hashes++;
            i++;
        }
        return i < s.length && s[i] == '"' ? hashes : -1;
    }

    private static int rawString(char[] s, int i) {
        int hashes = raw(s, i);
        i += hashes + 1;
        while (i < s.length) {
            if (s[i] == '"') {
                int k = 0;
                while (k < hashes && i + 1 + k < s.length && s[i + 1 + k] == '#') k++;
                if (k == hashes) return i + 1 + hashes;
            }
            if (s[i] != '\n') s[i] = ' ';
            i++;
        }
        return i;
    }

    /** A `'`: a character literal, whose inside is blanked, or the start of a lifetime, which is left. */
    private static int quote(char[] s, int i) {
        int n = s.length;
        if (i + 1 >= n) return i + 1;
        if (s[i + 1] == '\\') {
            int j = i + 2;
            while (j < n && s[j] != '\'' && s[j] != '\n') j++;
            if (j < n && s[j] == '\'' && j == i + 2) j++; // '\'' : the quote right after the backslash is the character
            while (j < n && s[j] != '\'' && s[j] != '\n') j++;
            for (int k = i + 1; k < j && k < n; k++) s[k] = ' ';
            return Math.min(j + 1, n);
        }
        int width = Character.isHighSurrogate(s[i + 1]) && i + 2 < n ? 2 : 1;
        if (i + 1 + width < n && s[i + 1 + width] == '\'' && s[i + 1] != '\n') {
            for (int k = i + 1; k < i + 1 + width; k++) s[k] = ' ';
            return i + 2 + width;
        }
        return i + 1;
    }

    private static boolean word(char[] s, int i) {
        return i >= 0 && (Character.isLetterOrDigit(s[i]) || s[i] == '_');
    }

    /** What is wrong with the brackets of that code (from `code`), in words; none when they pair up. */
    static List<String> brackets(String code) {
        List<String> wrong = new ArrayList<>();
        Deque<int[]> open = new ArrayDeque<>();
        int line = 1;
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '\n') line++;
            else if (c == '(' || c == '[' || c == '{') open.push(new int[] {c, line});
            else if (c == ')' || c == ']' || c == '}') {
                char want = c == ')' ? '(' : c == ']' ? '[' : '{';
                if (open.isEmpty()) {
                    wrong.add("line " + line + ": `" + c + "` closes nothing");
                    return wrong;
                }
                int[] top = open.pop();
                if (top[0] != want) {
                    wrong.add("line " + line + ": `" + c + "` closes the `" + (char) top[0] + "` of line " + top[1]);
                    return wrong;
                }
            }
        }
        if (!open.isEmpty()) wrong.add("line " + open.peek()[1] + ": `" + (char) open.peek()[0] + "` is never closed");
        return wrong;
    }

    /** The functions of that code (from `code`) that are outside any block. */
    static List<Function> functions(String code) {
        List<Function> found = new ArrayList<>();
        Matcher m = FN.matcher(code);
        int depth = 0, at = 0, line = 1;
        while (m.find()) {
            for (; at < m.start(); at++) {
                char c = code.charAt(at);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                else if (c == '\n') line++;
            }
            if (depth != 0) continue;
            found.add(new Function(m.group(1), parameters(code, m.end()), line));
        }
        return found;
    }

    /** How many parameters the function whose name ends at `i` takes; -1 when its list can't be read. */
    private static int parameters(String code, int i) {
        int n = code.length();
        while (i < n && Character.isWhitespace(code.charAt(i))) i++;
        if (i < n && code.charAt(i) == '<') {
            // Its generics: to the `>` that closes them (`->` closes nothing).
            int angle = 0;
            for (; i < n; i++) {
                char c = code.charAt(i);
                if (c == '<') angle++;
                else if (c == '>' && code.charAt(i - 1) != '-' && --angle == 0) break;
            }
            i++;
            while (i < n && Character.isWhitespace(code.charAt(i))) i++;
        }
        if (i >= n || code.charAt(i) != '(') return -1;
        int depth = 0, count = 0;
        boolean some = false;
        for (i++; i < n; i++) {
            char c = code.charAt(i);
            if (c == ')' && depth == 0) return some ? count + 1 : count;
            if (c == '(' || c == '[' || c == '{' || c == '<') depth++;
            else if (c == ')' || c == ']' || c == '}' || (c == '>' && code.charAt(i - 1) != '-')) depth--;
            else if (c == ',' && depth == 0) {
                if (some) count++;
                some = false;
            } else if (!Character.isWhitespace(c)) some = true;
        }
        return -1;
    }
}
