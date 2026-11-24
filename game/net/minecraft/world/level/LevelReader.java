package net.minecraft.world.level;

/**
 * The stand-in for the game's LevelReader: its supertypes, which side it is on, and whether a chunk is loaded (game/README.md,
 * "What isn't here").
 */
public interface LevelReader extends net.minecraft.world.level.BlockAndTintGetter, net.minecraft.world.level.CollisionGetter, net.minecraft.world.level.SignalGetter, net.minecraft.world.level.biome.BiomeManager.NoiseBiomeSource, net.neoforged.neoforge.common.extensions.ILevelReaderExtension {
    boolean isClientSide();

    net.minecraft.world.level.chunk.ChunkAccess getChunk(int x, int z, net.minecraft.world.level.chunk.status.ChunkStatus status, boolean create);

    boolean hasChunk(int x, int z);

    /** The biome at a position: Horizon's world's, not asked yet, so it refuses. */
    default net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> getBiome(net.minecraft.core.BlockPos pos) {
        throw new UnsupportedOperationException("the biome at a position isn't modelled (horizon-port-neoforge)");
    }
}
