package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * The stand-in for NeoForge's IItemExtension: an item's say over its stacks' size and damage, which ItemStack asks it
 * (observed with an item overriding them, `standins --horizon-src`).
 */
public interface IItemExtension {
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
}
