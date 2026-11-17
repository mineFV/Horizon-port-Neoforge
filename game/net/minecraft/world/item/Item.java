package net.minecraft.world.item;

import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.extensions.IItemExtension;

/** The stand-in for the game's Item: its id, given when it is registered (HorizonRegistries). */
public class Item implements FeatureElement, ItemLike, IItemExtension {
    private net.minecraft.resources.ResourceLocation horizonId;

    private net.minecraft.core.component.DataComponentMap components;

    public Item(Properties properties) {
        this.components = properties.buildAndValidateComponents();
    }

    public net.minecraft.core.component.DataComponentMap components() {
        return components;
    }

    /** Observed: itself. */
    public Item asItem() {
        return this;
    }

    public int getDefaultMaxStackSize() {
        return components.getOrDefault(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 1);
    }

    /** HorizonRegistries': the components of an item of the game's, as given. Not the game's API. */
    public void horizonGiven(net.minecraft.core.component.DataComponentMap given) {
        this.components = given;
    }

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

    /**
     * The stand-in for Item.Properties: the item's components, from those every item has (observed, `standins
     * --horizon-src`); its other settings aren't modelled.
     */
    public static class Properties implements net.neoforged.neoforge.common.extensions.IItemPropertiesExtensions {
        private final net.minecraft.core.component.DataComponentMap.Builder components =
                net.minecraft.core.component.DataComponentMap.builder().addAll(net.minecraft.core.component.DataComponents.COMMON_ITEM_COMPONENTS);

        public Properties() {}

        public Properties stacksTo(int n) {
            return component(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, n);
        }

        public Properties rarity(net.minecraft.world.item.Rarity rarity) {
            return component(net.minecraft.core.component.DataComponents.RARITY, rarity);
        }

        public Properties durability(int n) {
            component(net.minecraft.core.component.DataComponents.MAX_DAMAGE, n);
            component(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 1);
            component(net.minecraft.core.component.DataComponents.DAMAGE, 0);
            return this;
        }

        public <T> Properties component(net.minecraft.core.component.DataComponentType<T> type, T value) {
            components.set(type, value);
            return this;
        }

        net.minecraft.core.component.DataComponentMap buildAndValidateComponents() {
            net.minecraft.core.component.DataComponentMap m = components.build();
            if (m.has(net.minecraft.core.component.DataComponents.DAMAGE) && m.getOrDefault(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 1) > 1) {
                throw new IllegalStateException("Item cannot have both durability and be stackable");
            }
            return m;
        }
    }
}
