package net.minecraft.stats;

/**
 * The stand-in for the game's Stats: the ids of the custom stats a mod names (Mekanism's: opening a chest, a barrel),
 * observed in a server (`standins --horizon-src`, probe/ChunkProbe.java). Not here: the others, the stat types, the
 * registry they are in.
 */
public class Stats {
    public static final net.minecraft.resources.ResourceLocation OPEN_CHEST = net.minecraft.resources.ResourceLocation.withDefaultNamespace("open_chest");
    public static final net.minecraft.resources.ResourceLocation OPEN_BARREL = net.minecraft.resources.ResourceLocation.withDefaultNamespace("open_barrel");

    public Stats() {}
}
