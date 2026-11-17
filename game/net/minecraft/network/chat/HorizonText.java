package net.minecraft.network.chat;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Not the game's: a text's plain text, as Component.getString gives it (observed, `standins --horizon-src`): its
 * contents', then its siblings'. A translation is its language's text for the key (else the fallback, else the key),
 * with %s and %1$s replaced by the arguments and %% by %; a template it can't read stands as it is.
 */
public final class HorizonText {
    private HorizonText() {}

    static void append(Component c, StringBuilder out) {
        ComponentContents contents = c.getContents();
        if (contents instanceof PlainTextContents plain) out.append(plain.text());
        else if (contents instanceof TranslatableContents t) translation(t, out);
        else throw new UnsupportedOperationException("the text of " + contents + " isn't modelled (horizon-port-neoforge)");
        for (Component sibling : c.getSiblings()) append(sibling, out);
    }

    private static void translation(TranslatableContents t, StringBuilder out) {
        Language language = Language.getInstance();
        String template = t.getFallback() != null ? language.getOrDefault(t.getKey(), t.getFallback()) : language.getOrDefault(t.getKey());
        StringBuilder made = new StringBuilder();
        if (format(template, t.getArgs(), made)) out.append(made.toString());
        else out.append(template);
    }

    /** False when the template can't be read: a % not followed by s or %, an argument out of range. */
    private static boolean format(String template, Object[] args, StringBuilder out) {
        int next = 0;
        int i = 0;
        while (i < template.length()) {
            char ch = template.charAt(i);
            if (ch != '%') {
                out.append(ch);
                i++;
                continue;
            }
            int j = i + 1;
            int index = -1;
            int digits = j;
            while (digits < template.length() && template.charAt(digits) >= '0' && template.charAt(digits) <= '9') digits++;
            if (digits > j && digits < template.length() && template.charAt(digits) == '$') {
                int n = 0;
                for (int d = j; d < digits; d++) n = n * 10 + (template.charAt(d) - '0');
                if (n == 0) return false;
                index = n - 1;
                j = digits + 1;
            }
            if (j >= template.length()) return false;
            char kind = template.charAt(j);
            if (kind == '%' && index < 0) {
                out.append('%');
            } else if (kind == 's') {
                int at = index >= 0 ? index : next++;
                if (at < 0 || at >= args.length) return false;
                Object arg = args[at];
                if (arg instanceof Component component) append(component, out);
                else out.append(arg == null ? "null" : arg.toString());
            } else {
                return false;
            }
            i = j + 1;
        }
        return true;
    }
}
