package net.minecraft.world.item;

import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.extensions.IItemExtension;

/** The stand-in for the game's Item: its id, given when it is registered (HorizonRegistries). */
public class Item implements FeatureElement, ItemLike, IItemExtension {
    private net.minecraft.resources.ResourceLocation horizonId;

    public Item(Properties properties) {}

    private net.minecraft.core.Holder.Reference<Item> holder;

    /** Its holder: one for each item, its key the item registry's and its id (observed). */
    public net.minecraft.core.Holder.Reference<Item> builtInRegistryHolder() {
        if (holder == null) holder = new net.minecraft.core.Holder.Reference<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, horizonId), this);
        return holder;
    }

    /** HorizonRegistries' and ItemStack's: its id. Not the game's API. */
    public net.minecraft.resources.ResourceLocation horizonId() {
        return horizonId;
    }

    /** Observed in a server (`standins --horizon-src`): its id. */
    @Override
    public String toString() {
        return String.valueOf(horizonId);
    }

    /** HorizonRegistries': the id this item was registered under. Not the game's API. */
    public void horizonRegistered(net.minecraft.resources.ResourceLocation id) {
        this.horizonId = id;
    }

    /** The stand-in for Item.Properties: a builder (what it sets isn't read yet). */
    public static class Properties implements net.neoforged.neoforge.common.extensions.IItemPropertiesExtensions {
        public Properties() {}
    }
}
