package net.minecraft.world.level.chunk;

/**
 * The stand-in for the game's ChunkAccess: its supertypes alone (game/README.md, "What isn't here"). No chunk is
 * made: a level's getChunk gives null where the game does (Level), and refuses where it would give one.
 */
public abstract class ChunkAccess implements net.minecraft.world.level.BlockGetter, net.minecraft.world.level.biome.BiomeManager.NoiseBiomeSource, LightChunk, StructureAccess, net.neoforged.neoforge.attachment.IAttachmentHolder {
    private ChunkAccess() {}
}
