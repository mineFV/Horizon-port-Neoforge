import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Writes the Unicode tables the library model's Character and String read, as Rust statics, from the JDK that runs
 * it (build.sh runs it with the JDK that builds the tool): data of the JDK's Unicode version, no code of the JDK's.
 * Each table is sorted by code point, for a binary search.
 */
public final class UnicodeTables {
    private UnicodeTables() {}

    public static void main(String[] args) throws Exception {
        Method isCased = Class.forName("java.lang.ConditionalSpecialCasing").getDeclaredMethod("isCased", int.class);
        isCased.setAccessible(true);
        StringBuilder out = new StringBuilder();
        out.append("// Unicode's tables as the JDK ").append(System.getProperty("java.version")).append(" that built this tool has them.\n");
        // Simple case mappings: Character.toLowerCase(int), toUpperCase(int).
        StringBuilder lower = new StringBuilder(), upper = new StringBuilder(), lowerFull = new StringBuilder(), upperFull = new StringBuilder();
        for (int cp = 0; cp < 0x110000; cp++) {
            int l = Character.toLowerCase(cp), u = Character.toUpperCase(cp);
            if (l != cp) lower.append("(").append(cp).append(", ").append(l).append("), ");
            if (u != cp) upper.append("(").append(cp).append(", ").append(u).append("), ");
            if (Character.isSurrogate((char) cp) && cp < 0x10000) continue;
            String s = new String(Character.toChars(cp));
            String fl = s.toLowerCase(Locale.ROOT), fu = s.toUpperCase(Locale.ROOT);
            if (!fl.equals(new String(Character.toChars(l)))) lowerFull.append("(").append(cp).append(", &").append(units(fl)).append("), ");
            if (!fu.equals(new String(Character.toChars(u)))) upperFull.append("(").append(cp).append(", &").append(units(fu)).append("), ");
        }
        out.append("#[allow(dead_code)]\nstatic JUNI_LOWER: &[(u32, u32)] = &[").append(lower).append("];\n");
        out.append("#[allow(dead_code)]\nstatic JUNI_UPPER: &[(u32, u32)] = &[").append(upper).append("];\n");
        out.append("/// A code point's lower case as a text where it isn't its simple one (String.toLowerCase, out of context).\n");
        out.append("#[allow(dead_code)]\nstatic JUNI_LOWER_TEXT: &[(u32, &[u16])] = &[").append(lowerFull).append("];\n");
        out.append("/// A code point's upper case as a text where it isn't its simple one (ß is SS).\n");
        out.append("#[allow(dead_code)]\nstatic JUNI_UPPER_TEXT: &[(u32, &[u16])] = &[").append(upperFull).append("];\n");
        // General categories, by the first code point of each run.
        out.append("#[allow(dead_code)]\nstatic JUNI_TYPE: &[(u32, u8)] = &[").append(runs(cp -> Character.getType(cp))).append("];\n");
        // Properties, as runs of 0 and 1.
        out.append("#[allow(dead_code)]\nstatic JUNI_LOWERCASE: &[(u32, u8)] = &[").append(runs(cp -> Character.isLowerCase(cp) ? 1 : 0)).append("];\n");
        out.append("#[allow(dead_code)]\nstatic JUNI_UPPERCASE: &[(u32, u8)] = &[").append(runs(cp -> Character.isUpperCase(cp) ? 1 : 0)).append("];\n");
        out.append("#[allow(dead_code)]\nstatic JUNI_ALPHABETIC: &[(u32, u8)] = &[").append(runs(cp -> Character.isAlphabetic(cp) ? 1 : 0)).append("];\n");
        out.append("/// What String.toLowerCase counts as cased for a final sigma.\n");
        out.append("#[allow(dead_code)]\nstatic JUNI_CASED: &[(u32, u8)] = &[").append(runs(cp -> {
            try {
                return (boolean) isCased.invoke(null, cp) ? 1 : 0;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        })).append("];\n");
        // A digit's value (Character.digit's in radix 36, 255 for none), by runs where it goes up by one.
        StringBuilder digits = new StringBuilder();
        int start = -1, first = -1, previous = -2;
        for (int cp = 0; cp <= 0x110000; cp++) {
            int v = cp < 0x110000 ? Character.digit(cp, 36) : -1;
            boolean continues = v >= 0 && start >= 0 && cp == previous + 1 && v == first + (cp - start);
            if (!continues) {
                if (start >= 0) digits.append("(").append(start).append(", ").append(previous).append(", ").append(first).append("), ");
                start = v >= 0 ? cp : -1;
                first = v;
            }
            previous = cp;
        }
        out.append("/// Character.digit's value in radix 36: (first code point, last, the first's value).\n");
        out.append("#[allow(dead_code)]\nstatic JUNI_DIGIT: &[(u32, u32, u8)] = &[").append(digits).append("];\n");
        Files.writeString(Path.of(args[0]), out.toString(), StandardCharsets.UTF_8);
    }

    private static String units(String s) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < s.length(); i++) b.append(i == 0 ? "" : ", ").append((int) s.charAt(i));
        return b.append("]").toString();
    }

    interface Property {
        int of(int cp);
    }

    private static String runs(Property p) {
        StringBuilder b = new StringBuilder();
        int last = Integer.MIN_VALUE;
        for (int cp = 0; cp < 0x110000; cp++) {
            int v = p.of(cp);
            if (v != last) {
                b.append("(").append(cp).append(", ").append(v).append("), ");
                last = v;
            }
        }
        return b.toString();
    }
}
