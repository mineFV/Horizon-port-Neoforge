package net.neoforged.neoforge.registries;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/** The stand-in for NeoForge's DeferredBlock: a registered block. */
public class DeferredBlock<T extends Block> extends DeferredHolder<Block, T> implements ItemLike {
    protected DeferredBlock(ResourceKey<Block> key) {
        super(key);
    }

    public static <T extends Block> DeferredBlock<T> createBlock(ResourceKey<Block> key) {
        return new DeferredBlock<>(key);
    }

    /** Its block's item (Block.asItem). */
    @Override
    public net.minecraft.world.item.Item asItem() {
        return get().asItem();
    }
}
