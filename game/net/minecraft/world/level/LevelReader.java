package net.minecraft.world.level;

/** The stand-in for the game's LevelReader: its supertypes alone (game/README.md, "What isn't here"). */
public interface LevelReader extends net.minecraft.world.level.BlockAndTintGetter, net.minecraft.world.level.CollisionGetter, net.minecraft.world.level.SignalGetter, net.minecraft.world.level.biome.BiomeManager.NoiseBiomeSource, net.neoforged.neoforge.common.extensions.ILevelReaderExtension {
    boolean isClientSide();
}
