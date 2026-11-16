package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;

/**
 * The stand-in for the game's BlockEntity: its type, level, position, state and removal, and what a mod saves and
 * loads (saveAdditional, loadAdditional: nothing here, the mod's own say what). In Horizon the block entity is its
 * saved compound: HorizonBridge loads the mod's object from it and saves it back after each tick, so setChanged
 * has nothing more to do. Not modelled: NeoForge's persistent data and attachments, data components.
 */
public abstract class BlockEntity extends AttachmentHolder implements IBlockEntityExtension {
    private final BlockEntityType<?> type;
    protected Level level;
    protected final BlockPos worldPosition;
    protected boolean remove;
    private BlockState blockState;

    public BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this.type = type;
        this.worldPosition = pos.immutable();
        this.blockState = state;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public boolean hasLevel() {
        return level != null;
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {}

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {}

    public final void loadWithComponents(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    public final CompoundTag saveWithoutMetadata(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    /** Horizon saves the compound HorizonBridge writes after each tick: nothing more to mark. */
    public void setChanged() {}

    public BlockPos getBlockPos() {
        return worldPosition;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public void setBlockState(BlockState state) {
        this.blockState = state;
    }

    public boolean isRemoved() {
        return remove;
    }

    public void setRemoved() {
        remove = true;
    }

    public void clearRemoved() {
        remove = false;
    }

    public BlockEntityType<?> getType() {
        return type;
    }
}
