package com.example.gamey;

import java.util.List;
import net.minecraft.world.Light;
import net.minecraft.world.Named;

/** Made up for the translator's tests: a class of the mod that implements an interface of the game. */
public final class Tagged {
    private Tagged() {}

    static final class Tag implements Named {
        @Override
        public String name() {
            return "tag";
        }
    }

    /** Asks whether an object of Tag is a List: only the game's Named, read, can tell. */
    public static int size(int n) {
        Object o = n > 0 ? new Tag() : List.of(1, 2);
        return o instanceof List<?> l ? l.size() : -1;
    }

    /** Runs a default method of the game's interface. */
    public static int weight() {
        return new Tag().weight();
    }

    /** An enum whose initializer calls the game: its values() isn't translated, nor its valueOf. */
    enum Stuck {
        A;

        static final int LEVEL = Light.level();
    }

    enum Shade {
        DARK, BRIGHT
    }

    /**
     * Enum.valueOf reads the values() of every enum with a class object: Stuck's class constant, in its valueOf, drops
     * it at first, and it comes back once that valueOf drops in turn.
     */
    public static int pick(String name) {
        return Enum.valueOf(Shade.class, name).ordinal();
    }
}
