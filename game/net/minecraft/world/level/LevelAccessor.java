package net.minecraft.world.level;

/** The stand-in for the game's LevelAccessor: its supertypes, and whether a chunk is loaded (game/README.md, "What isn't here"). */
public interface LevelAccessor extends net.minecraft.world.level.CommonLevelAccessor, net.minecraft.world.level.LevelTimeAccess {
    /**
     * Whether the chunk at chunk coordinates is loaded, asked of Horizon's world (HorizonWorld): Horizon answers the
     * game's hasChunk with the chunks its world holds (crates/horizon-client/src/game/raids.rs, `has_chunk`).
     */
    @Override
    default boolean hasChunk(int x, int z) {
        return HorizonWorld.hasChunk(x, z);
    }
}
