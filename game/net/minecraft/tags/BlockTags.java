package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

/** The stand-in for the game's BlockTags: making a block tag's key, and the constants Mekanism uses. */
public final class BlockTags {
    private BlockTags() {}

    public static final TagKey<net.minecraft.world.level.block.Block> REPLACEABLE_BY_TREES = create(ResourceLocation.withDefaultNamespace("replaceable_by_trees"));

    public static TagKey<net.minecraft.world.level.block.Block> create(ResourceLocation id) {
        return TagKey.create(Registries.BLOCK, id);
    }
}
