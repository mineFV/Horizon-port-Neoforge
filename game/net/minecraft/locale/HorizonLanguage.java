package net.minecraft.locale;

import java.util.HashMap;
import java.util.Map;

/**
 * Not the game's: the language Language.getInstance() gives, its texts as given (given): those of some keys, and
 * whether no other key has one (all). Asked a key it can't answer, it says so.
 */
public final class HorizonLanguage extends Language {
    static final HorizonLanguage INSTANCE = new HorizonLanguage();

    private final Map<String, String> texts = new HashMap<>();
    private boolean all;

    private HorizonLanguage() {}

    /** The texts of some keys ("key=text" lines), and whether they are all there are. Not the game's API. */
    public static synchronized void given(String lines, boolean all) {
        for (String line : lines.split("\n")) {
            int eq = line.indexOf('=');
            if (eq > 0) INSTANCE.texts.put(line.substring(0, eq), line.substring(eq + 1));
        }
        INSTANCE.all = all;
    }

    @Override
    public synchronized String getOrDefault(String key, String fallback) {
        String text = texts.get(key);
        if (text != null) return text;
        if (all) return fallback;
        throw new IllegalStateException("the game's text of " + key + " isn't known here (horizon-port-neoforge)");
    }

    @Override
    public synchronized boolean has(String key) {
        if (texts.containsKey(key)) return true;
        if (all) return false;
        throw new IllegalStateException("the game's text of " + key + " isn't known here (horizon-port-neoforge)");
    }

    @Override
    public boolean isDefaultRightToLeft() {
        return false;
    }
}
