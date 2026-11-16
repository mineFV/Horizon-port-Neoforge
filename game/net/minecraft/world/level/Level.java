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

    /** Not modelled: what closing a level does is the game's. */
    @Override
    public native void close();
}
