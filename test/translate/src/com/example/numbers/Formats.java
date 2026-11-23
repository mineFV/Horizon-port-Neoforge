package com.example.numbers;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * NumberFormat's texts (lib/README.md): the integer and number instances of the locales the model's Locale has, of a
 * double (below 1e16: from there the model refuses) and of a long, and what they say of themselves; the same of the
 * default locale's, which the test runs in several environments.
 */
public final class Formats {
    private Formats() {}

    static final Locale[] LOCALES = {Locale.ROOT, Locale.ENGLISH, Locale.US, Locale.UK, Locale.CANADA, Locale.FRENCH,
        Locale.FRANCE, Locale.CANADA_FRENCH, Locale.GERMAN, Locale.GERMANY, Locale.ITALIAN, Locale.ITALY};

    public static String doubles(double d) {
        StringBuilder b = new StringBuilder();
        for (Locale l : LOCALES) {
            b.append(NumberFormat.getIntegerInstance(l).format(d)).append('|').append(NumberFormat.getNumberInstance(l).format(d)).append(' ');
        }
        return b.toString();
    }

    public static String longs(long v) {
        StringBuilder b = new StringBuilder();
        for (Locale l : LOCALES) {
            b.append(NumberFormat.getIntegerInstance(l).format(v)).append('|').append(NumberFormat.getInstance(l).format(v)).append(' ');
        }
        return b.toString();
    }

    public static String facts() {
        StringBuilder b = new StringBuilder();
        for (Locale l : LOCALES) {
            fact(b, NumberFormat.getIntegerInstance(l), NumberFormat.getIntegerInstance(l));
            fact(b, NumberFormat.getNumberInstance(l), NumberFormat.getInstance(l));
        }
        return b.toString();
    }

    public static String defaultDouble(double d) {
        return NumberFormat.getIntegerInstance().format(d) + "|" + NumberFormat.getNumberInstance().format(d) + "|" + NumberFormat.getInstance().format(d);
    }

    public static String defaultLong(long v) {
        return NumberFormat.getIntegerInstance().format(v) + "|" + NumberFormat.getNumberInstance().format(v);
    }

    public static String defaultFacts() {
        StringBuilder b = new StringBuilder();
        fact(b, NumberFormat.getIntegerInstance(), NumberFormat.getIntegerInstance());
        fact(b, NumberFormat.getNumberInstance(), NumberFormat.getInstance());
        return b.toString();
    }

    static void fact(StringBuilder b, NumberFormat f, NumberFormat same) {
        DecimalFormat d = (DecimalFormat) f;
        Object copy = d.clone();
        b.append(d.toPattern()).append(',').append(d.getPositivePrefix()).append(',').append(d.getNegativePrefix()).append(',')
                .append(d.getPositiveSuffix()).append(',').append(d.getNegativeSuffix()).append(',').append(d.getGroupingSize()).append(',')
                .append(d.isGroupingUsed()).append(',').append(d.getMaximumFractionDigits()).append(',').append(d.getMinimumFractionDigits()).append(',')
                .append(d.getMaximumIntegerDigits()).append(',').append(d.getMinimumIntegerDigits()).append(',').append(d.isParseIntegerOnly()).append(',')
                .append(d.getMultiplier()).append(',').append(d.isDecimalSeparatorAlwaysShown()).append(',').append(d.hashCode()).append(',')
                .append(d.equals(same)).append(',').append(d == same).append(',').append(d.equals(copy)).append(',').append(copy != d).append(',')
                .append(copy.getClass().getName()).append(',').append(d.equals(NumberFormat.getIntegerInstance(Locale.ROOT))).append(',')
                .append(d.equals(NumberFormat.getNumberInstance(Locale.GERMANY))).append(',').append(d.equals("x")).append(',')
                .append(f instanceof java.text.Format).append(';');
    }
}
