package java.text;

import java.util.Locale;

/**
 * The model of java.text.NumberFormat: a locale's integer and number instances (DecimalFormat, getInstance being the
 * number one), of a given locale or of the default one, which the JDK makes from the environment (Locale.Category
 * .FORMAT; on Linux, as lib/README.md says). Their texts of a long and of a double, and their getters. Not modelled:
 * parsing, the setters, the currency and percent instances, format(Object) and the field positions, a NumberFormat
 * of one's own. An instance whose locale the model has no data for, or whose default locale can't be told, is made,
 * and refuses when used.
 */
public abstract class NumberFormat extends Format {
    public static final int INTEGER_FIELD = 0;
    public static final int FRACTION_FIELD = 1;

    protected NumberFormat() {}

    public final String format(double number) {
        return jText(number);
    }

    public final String format(long number) {
        return jText(number);
    }

    abstract String jText(double number);

    abstract String jText(long number);

    public static NumberFormat getInstance() {
        return getNumberInstance();
    }

    public static NumberFormat getInstance(Locale locale) {
        return getNumberInstance(locale);
    }

    public static NumberFormat getNumberInstance() {
        return new DecimalFormat(jDefaultKey(), false);
    }

    public static NumberFormat getNumberInstance(Locale locale) {
        return new DecimalFormat(key(locale), false);
    }

    public static NumberFormat getIntegerInstance() {
        return new DecimalFormat(jDefaultKey(), true);
    }

    public static NumberFormat getIntegerInstance(Locale locale) {
        return new DecimalFormat(key(locale), true);
    }

    /** A locale's key in the model's data: "language_script_country" (the model's Locale has no script). */
    static String key(Locale locale) {
        return locale.getLanguage() + "__" + locale.getCountry();
    }

    public abstract int getMaximumIntegerDigits();

    public abstract int getMinimumIntegerDigits();

    public abstract int getMaximumFractionDigits();

    public abstract int getMinimumFractionDigits();

    public abstract boolean isGroupingUsed();

    public abstract boolean isParseIntegerOnly();

    /** The default format locale's key, or "!" and why it can't be told (Library, jnative_number_default_key). */
    static native String jDefaultKey();

    /** A key's record (numbers.rs): the integer instance's fields, U+0001, the number instance's; null where none. */
    static native String jData(String key);
}
