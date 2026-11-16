package net.minecraft.world.level;

import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.common.extensions.ILevelExtension;

/**
 * The stand-in for the game's Level: whether it is a client's. The world itself is Horizon's, reached through the
 * hook points; none of the Level's methods is modelled yet, so a call to one isn't translated.
 */
public abstract class Level extends AttachmentHolder implements LevelAccessor, AutoCloseable, ILevelExtension {
    public final boolean isClientSide;

    protected Level(boolean isClientSide) {
        this.isClientSide = isClientSide;
    }

    /** The state at a position, from Horizon's world (HorizonWorld): its block, in its default state. */
    public net.minecraft.world.level.block.state.BlockState getBlockState(net.minecraft.core.BlockPos pos) {
        return HorizonWorld.blockState(pos);
    }

    /**
     * Sets the state at a position, through Horizon's world (HorizonWorld): a change of the properties of the block
     * there; another block isn't set yet (false), as Horizon's hook points don't write whole blocks.
     */
    public boolean setBlock(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, int flags) {
        return HorizonWorld.setBlock(pos, state, flags);
    }

    /** Not modelled: what closing a level does is the game's. */
    @Override
    public native void close();
}
