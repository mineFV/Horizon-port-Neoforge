package net.minecraft.locale;

/**
 * The stand-in for the game's Language: the game's texts by key. What it holds is given (HorizonLanguage): a key not
 * given can't be answered, and says so, unless all were given.
 */
public abstract class Language {
    public static final String DEFAULT = "en_us";

    public Language() {}

    public static Language getInstance() {
        return HorizonLanguage.INSTANCE;
    }

    public String getOrDefault(String key) {
        return getOrDefault(key, key);
    }

    public abstract String getOrDefault(String key, String fallback);

    public abstract boolean has(String key);

    public abstract boolean isDefaultRightToLeft();
}
