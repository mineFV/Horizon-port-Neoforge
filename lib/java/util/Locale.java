package java.util;

/**
 * The model of java.util.Locale: its constants alone (the root locale, English, French, German...), so that a
 * locale whose case rules differ (Turkish, Azeri, Lithuanian) can't be made, and String's case mappings are those
 * of the root locale. Not modelled: getDefault, of, forLanguageTag, the display names.
 */
public final class Locale implements Cloneable, java.io.Serializable {
    public static final Locale ROOT = new Locale("", "");
    public static final Locale ENGLISH = new Locale("en", "");
    public static final Locale US = new Locale("en", "US");
    public static final Locale UK = new Locale("en", "GB");
    public static final Locale CANADA = new Locale("en", "CA");
    public static final Locale FRENCH = new Locale("fr", "");
    public static final Locale FRANCE = new Locale("fr", "FR");
    public static final Locale CANADA_FRENCH = new Locale("fr", "CA");
    public static final Locale GERMAN = new Locale("de", "");
    public static final Locale GERMANY = new Locale("de", "DE");
    public static final Locale ITALIAN = new Locale("it", "");
    public static final Locale ITALY = new Locale("it", "IT");

    private final String language;
    private final String country;

    private Locale(String language, String country) {
        this.language = language;
        this.country = country;
    }

    public String getLanguage() {
        return language;
    }

    public String getCountry() {
        return country;
    }

    public String getVariant() {
        return "";
    }

    public String toString() {
        return country.isEmpty() ? language : language.concat("_").concat(country);
    }

    public String toLanguageTag() {
        if (language.isEmpty()) return "und";
        return country.isEmpty() ? language : language.concat("-").concat(country);
    }

    public boolean equals(Object obj) {
        return this == obj || (obj instanceof Locale other && language.equals(other.language) && country.equals(other.country));
    }

    public int hashCode() {
        return language.hashCode() * 31 + country.hashCode();
    }
}
