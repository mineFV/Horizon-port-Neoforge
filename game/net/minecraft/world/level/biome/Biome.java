package net.minecraft.world.level.biome;

/**
 * The stand-in for the game's Biome: a type alone. A biome at a position is Horizon's world's, which isn't asked yet:
 * LevelReader.getBiome refuses, so none is made here. getTemperature is public as Mekanism's access transformer makes
 * it (private in the game).
 */
public final class Biome {
    private Biome() {}

    /** A biome's temperature at a position (height-adjusted): Horizon's world's, not asked yet. */
    public float getTemperature(net.minecraft.core.BlockPos pos) {
        throw new UnsupportedOperationException("a biome's temperature isn't modelled (horizon-port-neoforge)");
    }
}
