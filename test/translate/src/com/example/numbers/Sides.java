package com.example.numbers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * The server's side: what only a client runs (here, a split by a regular expression, which isn't translated) is never
 * reached, so the method is translated, and still answers as Java does (a null level still throws).
 */
public final class Sides {
    private Sides() {}

    static String client(String text) {
        return String.valueOf(text.split("[,;]").length);
    }

    public static String side(Level level) {
        StringBuilder b = new StringBuilder();
        if (level.isClientSide) b.append(client("a,b"));
        else b.append("field ");
        if (!level.isClientSide()) b.append("call ");
        else return client("a;b;c");
        if (FMLEnvironment.dist.isClient()) b.append(client("x"));
        if (FMLEnvironment.dist.isDedicatedServer()) b.append("dedicated");
        else b.append(client("y,z"));
        return b.toString();
    }

    public static String server() {
        return side(new ServerLevel());
    }

    public static String none() {
        return side(null);
    }

    /** Math.log10 where its specification fixes it: powers of ten, zeros, below zero, NaN, infinity. */
    public static String logs() {
        double[] in = {1, 10, 100, 1e22, 1e15, 0.0, -0.0, -1, Double.NaN, Double.POSITIVE_INFINITY};
        StringBuilder b = new StringBuilder();
        for (double d : in) b.append(Math.log10(d)).append(' ');
        return b.toString();
    }
}
