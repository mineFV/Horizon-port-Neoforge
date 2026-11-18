package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * The stand-in for NeoForge's IItemExtension: an item's say over its stacks' size and damage, which ItemStack asks it
 * (observed with an item overriding them, `standins --horizon-src`).
 */
public interface IItemExtension {
    boolean isRepairable(ItemStack stack);

    default int getMaxStackSize(ItemStack stack) {
        return stack.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
    }

    default int getMaxDamage(ItemStack stack) {
        return stack.getOrDefault(DataComponents.MAX_DAMAGE, 0);
    }

    default int getDamage(ItemStack stack) {
        return net.minecraft.util.Mth.clamp(stack.getOrDefault(DataComponents.DAMAGE, 0), 0, stack.getMaxDamage());
    }

    default boolean isDamaged(ItemStack stack) {
        return stack.getDamageValue() > 0;
    }

    default void setDamage(ItemStack stack, int damage) {
        stack.set(DataComponents.DAMAGE, net.minecraft.util.Mth.clamp(damage, 0, stack.getMaxDamage()));
    }

    /** Observed (`standins --horizon-src`): the item's own (Item.hasCraftingRemainingItem). */
    default boolean hasCraftingRemainingItem(ItemStack stack) {
        return ((net.minecraft.world.item.Item) this).hasCraftingRemainingItem();
    }

    /** Observed: a stack of one of the item's remainder, else the empty stack. */
    default ItemStack getCraftingRemainingItem(ItemStack stack) {
        if (!hasCraftingRemainingItem(stack)) return ItemStack.EMPTY;
        return new ItemStack(((net.minecraft.world.item.Item) this).getCraftingRemainingItem());
    }

    /** Its furnace fuels data map's value for the stack's item, else 0 (observed, `standins --horizon-src`). */
    default int getBurnTime(ItemStack stack, net.minecraft.world.item.crafting.RecipeType<?> recipeType) {
        net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel fuel =
                stack.getItemHolder().getData(net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps.FURNACE_FUELS);
        return fuel == null ? 0 : fuel.burnTime();
    }

    /**
     * Observed: its id's namespace. A stack of one of the game's items whose components were changed isn't modelled
     * (what it holds, an enchantment or a potion, may be another mod's): refused.
     */
    default String getCreatorModId(ItemStack stack) {
        String namespace = stack.getItem().horizonId().getNamespace();
        if (namespace.equals("minecraft") && !stack.isComponentsPatchEmpty())
            throw new UnsupportedOperationException("the mod that made a changed stack of " + stack.getItem() + " isn't modelled (horizon-port-neoforge)");
        return namespace;
    }
}
