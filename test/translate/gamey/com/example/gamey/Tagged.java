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

    static final class Badge implements Named {
        @Override
        public String name() {
            return "badge";
        }
    }

    /** Runs a default method of the game's interface: Badge's makers drop (this one), not Tag's. */
    public static int weight() {
        return new Badge().weight();
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

    /** A Consumer of the mod's that calls the game: Iterable.forEach may reach it. */
    static final class Lit implements java.util.function.Consumer<Object> {
        @Override
        public void accept(Object o) {
            Light.level();
        }
    }

    /** Makes a Lit: it drops out, so that the methods that iterate don't. */
    public static Object lit() {
        return new Lit();
    }

    public static int total(List<Integer> xs) {
        int[] t = {0};
        xs.forEach(x -> t[0] += x);
        return t[0];
    }

    /** A separator that is a regular expression: String.split isn't translated for it. */
    public static int words(String s) {
        return s.split("\\s+").length;
    }

    /** A class of the client, named behind a test: a dedicated server doesn't have it, so naming it throws there. */
    public static int screens(boolean client) {
        if (client) return net.minecraft.client.Screenish.open();
        return 0;
    }

    /** An array of a class of the client: making it resolves the class, which the server doesn't have. */
    public static int screenArray(boolean client) {
        if (client) return new net.minecraft.client.Screenish[2].length;
        return 0;
    }

    /** A class of the mod's that extends one of the client's. */
    static final class Sound extends net.minecraft.client.Soundish {
    }

    /** An array of it: what loading Sound does on a dedicated server isn't observed, so this isn't translated. */
    public static int sounds() {
        return new Sound[3].length;
    }
}
