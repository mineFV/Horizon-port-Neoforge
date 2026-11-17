package net.neoforged.neoforge.common;

/**
 * The stand-in for NeoForge's NeoForgeMod: what a mod asks of NeoForge's own content. Milk as a fluid is NeoForge's
 * content, not the mod's: here the request is kept, for Horizon's side to honour (it isn't yet).
 */
public class NeoForgeMod {
    private static boolean milkFluid;

    private NeoForgeMod() {}

    public static void enableMilkFluid() {
        milkFluid = true;
    }

    /** Not the game's: whether a mod asked for milk as a fluid. */
    public static boolean horizonMilkFluid() {
        return milkFluid;
    }
}
