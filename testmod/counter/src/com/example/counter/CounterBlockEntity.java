package com.example.counter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Counts its server ticks; every second (Counter.TICKS_PER_SECOND, 20 by default), adds a second to its total, notes the block below it, and marks itself changed.
 * All are saved, so the count goes on after the world is reloaded: what Horizon's save must carry over. The block
 * below is read from the level, and the block is lit or put out each second: what Horizon's world must answer and do.
 */
public class CounterBlockEntity extends BlockEntity {
    private int ticks;
    private long seconds;
    private String below = "";

    public CounterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COUNTER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CounterBlockEntity counter) {
        counter.ticks++;
        if (counter.ticks % Counter.TICKS_PER_SECOND.getAsInt() == 0) {
            counter.seconds++;
            counter.below = level.getBlockState(pos.below()).getBlock().toString();
            level.setBlock(pos, state.setValue(CounterBlock.LIT, !state.getValue(CounterBlock.LIT)), 3);
            counter.setChanged();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ticks = tag.getInt("ticks");
        seconds = tag.getLong("seconds");
        below = tag.getString("below");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ticks", ticks);
        tag.putLong("seconds", seconds);
        tag.putString("below", below);
        tag.putInt("capabilities", Counter.capabilities);
    }
}
