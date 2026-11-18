package net.neoforged.neoforge.common.extensions;

import net.neoforged.neoforge.capabilities.ItemCapability;

/**
 * The stand-in for NeoForge's IItemStackExtension: a stack's capabilities, its item's providers' (ItemCapability), and
 * its burn time.
 */
public interface IItemStackExtension {
    default <T, C> T getCapability(ItemCapability<T, C> capability, C context) {
        return capability.getCapability((net.minecraft.world.item.ItemStack) this, context);
    }

    default <T> T getCapability(ItemCapability<T, Void> capability) {
        return capability.getCapability((net.minecraft.world.item.ItemStack) this, null);
    }

    /** Observed (`standins --horizon-src`): its item's say. */
    default boolean hasCraftingRemainingItem() {
        net.minecraft.world.item.ItemStack stack = (net.minecraft.world.item.ItemStack) this;
        return stack.getItem().hasCraftingRemainingItem(stack);
    }

    default net.minecraft.world.item.ItemStack getCraftingRemainingItem() {
        net.minecraft.world.item.ItemStack stack = (net.minecraft.world.item.ItemStack) this;
        return stack.getItem().getCraftingRemainingItem(stack);
    }

    /**
     * Observed (`standins --horizon-src`): 0 for an empty stack; else its item's, which FurnaceFuelBurnTimeEvent's
     * listeners on NeoForge.EVENT_BUS may change.
     */
    default int getBurnTime(net.minecraft.world.item.crafting.RecipeType<?> recipeType) {
        net.minecraft.world.item.ItemStack stack = (net.minecraft.world.item.ItemStack) this;
        if (stack.isEmpty()) return 0;
        net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent event =
                new net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent(stack, stack.getItem().getBurnTime(stack, recipeType), recipeType);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        return event.getBurnTime();
    }
}
