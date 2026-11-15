package com.example.gamey;

import java.util.List;
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
}
