package net.minecraft.world.level;

import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.common.extensions.ILevelExtension;

/**
 * The stand-in for the game's Level. The translation runs on the server's side (FMLEnvironment.dist is
 * DEDICATED_SERVER): every level it sees is a server's, so isClientSide is false, always; the translator counts on it
 * (MethodRust.sides). The world itself is Horizon's, reached through the hook points.
 */
public abstract class Level extends AttachmentHolder implements LevelAccessor, AutoCloseable, ILevelExtension {
    public final boolean isClientSide;

    protected Level() {
        this.isClientSide = false;
    }

    /** False: a server's (see above). */
    @Override
    public boolean isClientSide() {
        return isClientSide;
    }

    /** The state at a position, from Horizon's world (HorizonWorld): its block, in its default state. */
    public net.minecraft.world.level.block.state.BlockState getBlockState(net.minecraft.core.BlockPos pos) {
        return HorizonWorld.blockState(pos);
    }

    /** Refused: the block entities of Horizon's world, but the one the bridge ticks, aren't reached yet. */
    @Override
    public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(net.minecraft.core.BlockPos pos) {
        throw new UnsupportedOperationException("a level's block entities aren't modelled (horizon-port-neoforge): getBlockEntity(" + pos + ")");
    }

    /** Refused: the fluids of Horizon's world aren't asked yet. */
    @Override
    public net.minecraft.world.level.material.FluidState getFluidState(net.minecraft.core.BlockPos pos) {
        throw new UnsupportedOperationException("a level's fluids aren't modelled (horizon-port-neoforge): getFluidState(" + pos + ")");
    }

    /**
     * Sets the state at a position, through Horizon's world (HorizonWorld): a change of the properties of the block
     * there; another block isn't set yet (false), as Horizon's hook points don't write whole blocks.
     */
    public boolean setBlock(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, int flags) {
        return HorizonWorld.setBlock(pos, state, flags);
    }

    /**
     * A chunk of the level. Null for a whole chunk (FULL) that isn't loaded, without making it (create false), as the
     * game gives null where hasChunk is false (observed in a server, 2026-11-22). A chunk as an object isn't modelled:
     * any other call refuses, saying so.
     */
    @Override
    public net.minecraft.world.level.chunk.ChunkAccess getChunk(int x, int z, net.minecraft.world.level.chunk.status.ChunkStatus status, boolean create) {
        if (status == net.minecraft.world.level.chunk.status.ChunkStatus.FULL && !create && !hasChunk(x, z)) return null;
        throw new UnsupportedOperationException("a level's chunks as objects aren't modelled (horizon-port-neoforge): getChunk(" + x + ", " + z + ", " + status + ", " + create + ")");
    }

    /** Not modelled: what closing a level does is the game's. */
    @Override
    public native void close();
}
