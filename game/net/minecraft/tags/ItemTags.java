package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

/** The stand-in for the game's ItemTags: making an item tag's key (its constants aren't here). */
public final class ItemTags {
    private ItemTags() {}

    public static TagKey<net.minecraft.world.item.Item> create(ResourceLocation id) {
        return TagKey.create(Registries.ITEM, id);
    }
}
