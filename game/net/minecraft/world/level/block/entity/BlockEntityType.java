package net.minecraft.world.level.block.entity;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The stand-in for the game's BlockEntityType: what makes its block entities, and on which blocks. */
public class BlockEntityType<T extends BlockEntity> {
    private final BlockEntitySupplier<? extends T> factory;
    private final Set<Block> validBlocks;

    public BlockEntityType(BlockEntitySupplier<? extends T> factory, Set<Block> validBlocks, com.mojang.datafixers.types.Type<?> dataType) {
        this.factory = factory;
        this.validBlocks = validBlocks;
    }

    public T create(BlockPos pos, BlockState state) {
        return factory.create(pos, state);
    }

    public boolean isValid(BlockState state) {
        return validBlocks.contains(state.getBlock());
    }

    /** The stand-in for BlockEntityType.Builder. */
    public static final class Builder<T extends BlockEntity> {
        private final BlockEntitySupplier<? extends T> factory;
        private final Set<Block> validBlocks;

        private Builder(BlockEntitySupplier<? extends T> factory, Set<Block> validBlocks) {
            this.factory = factory;
            this.validBlocks = validBlocks;
        }

        public static <T extends BlockEntity> Builder<T> of(BlockEntitySupplier<? extends T> factory, Block... validBlocks) {
            return new Builder<>(factory, Set.of(validBlocks));
        }

        public BlockEntityType<T> build(com.mojang.datafixers.types.Type<?> dataType) {
            return new BlockEntityType<>(factory, validBlocks, dataType);
        }
    }

    /** The stand-in for BlockEntityType.BlockEntitySupplier. */
    @FunctionalInterface
    public interface BlockEntitySupplier<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }
}
