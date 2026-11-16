package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The stand-in for the game's BlockEntityTicker. */
public interface BlockEntityTicker<T extends BlockEntity> {
    void tick(Level level, BlockPos pos, BlockState state, T entity);
}
