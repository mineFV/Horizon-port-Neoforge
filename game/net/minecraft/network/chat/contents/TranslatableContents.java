package net.minecraft.network.chat.contents;

import java.util.Arrays;
import java.util.Objects;
import net.minecraft.network.chat.ComponentContents;

/** The stand-in for the game's TranslatableContents: a key, a fallback and arguments (observed, `standins --horizon-src`). */
public class TranslatableContents implements ComponentContents {
    public static final Object[] NO_ARGS = new Object[0];

    private final String key;
    private final String fallback;
    private final Object[] args;

    public TranslatableContents(String key, String fallback, Object[] args) {
        this.key = key;
        this.fallback = fallback;
        this.args = args;
    }

    public static boolean isAllowedPrimitiveArgument(Object o) {
        return o instanceof Number || o instanceof Boolean || o instanceof String;
    }

    public String getKey() {
        return key;
    }

    public String getFallback() {
        return fallback;
    }

    public Object[] getArgs() {
        return args;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof TranslatableContents t && Objects.equals(key, t.key) && Objects.equals(fallback, t.fallback) && Arrays.equals(args, t.args);
    }

    @Override
    public int hashCode() {
        int h = Objects.hashCode(key);
        h = 31 * h + Objects.hashCode(fallback);
        return 31 * h + Arrays.hashCode(args);
    }

    @Override
    public String toString() {
        return "translation{key='" + key + "'" + (fallback != null ? ", fallback='" + fallback + "'" : "") + ", args=" + Arrays.toString(args) + "}";
    }
}
