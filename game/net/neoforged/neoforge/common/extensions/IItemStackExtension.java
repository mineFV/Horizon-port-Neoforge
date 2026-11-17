package net.neoforged.neoforge.common.extensions;

import net.neoforged.neoforge.capabilities.ItemCapability;

/** The stand-in for NeoForge's IItemStackExtension: a stack's capabilities, its item's providers' (ItemCapability). */
public interface IItemStackExtension {
    default <T, C> T getCapability(ItemCapability<T, C> capability, C context) {
        return capability.getCapability((net.minecraft.world.item.ItemStack) this, context);
    }

    default <T> T getCapability(ItemCapability<T, Void> capability) {
        return capability.getCapability((net.minecraft.world.item.ItemStack) this, null);
    }
}
