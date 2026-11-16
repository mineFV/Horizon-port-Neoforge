package net.minecraft.world.level.biome;

/** The stand-in for the game's BiomeManager: only its NoiseBiomeSource interface, for LevelReader's supertypes. */
public class BiomeManager {
    private BiomeManager() {}

    /** Its supertypes alone. */
    public interface NoiseBiomeSource {
    }
}
