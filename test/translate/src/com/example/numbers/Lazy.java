package com.example.numbers;

import java.util.function.Supplier;

/**
 * A lambda whose method isn't translated (a split by a regular expression): made all the same, as Java makes it,
 * so that its class initializer is translated; it refuses when run, saying so.
 */
public final class Lazy {
    private Lazy() {}

    static final Supplier<String> SPLIT = () -> String.valueOf("a,b;c".split("[,;]").length);

    public static String made() {
        return SPLIT != null ? "made" : "none";
    }

    /** Run by the translation alone (Java runs the split): what the lambda says. */
    public static String run() {
        try {
            return SPLIT.get();
        } catch (UnsupportedOperationException e) {
            return "refused: " + e.getMessage();
        }
    }
}
