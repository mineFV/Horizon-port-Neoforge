package net.neoforged.neoforge.capabilities;

import net.minecraft.resources.ResourceLocation;

/**
 * The stand-in for NeoForge's Capabilities: its capabilities, their names observed (`standins --horizon-src`). Its
 * blocks' are proxyable once NeoForge has loaded (observed): here, from the start.
 */
public final class Capabilities {
    private Capabilities() {}

    public static final class EnergyStorage {
        private EnergyStorage() {}

        public static final BlockCapability<net.neoforged.neoforge.energy.IEnergyStorage, net.minecraft.core.Direction> BLOCK =
                BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("neoforge", "energy"), net.neoforged.neoforge.energy.IEnergyStorage.class);
        public static final EntityCapability<net.neoforged.neoforge.energy.IEnergyStorage, net.minecraft.core.Direction> ENTITY =
                EntityCapability.createSided(ResourceLocation.fromNamespaceAndPath("neoforge", "energy"), net.neoforged.neoforge.energy.IEnergyStorage.class);
        public static final ItemCapability<net.neoforged.neoforge.energy.IEnergyStorage, Void> ITEM =
                ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath("neoforge", "energy"), net.neoforged.neoforge.energy.IEnergyStorage.class);
        static {
            BLOCK.setProxyable(true);
        }
    }

    public static final class FluidHandler {
        private FluidHandler() {}

        public static final BlockCapability<net.neoforged.neoforge.fluids.capability.IFluidHandler, net.minecraft.core.Direction> BLOCK =
                BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("neoforge", "fluid_handler"), net.neoforged.neoforge.fluids.capability.IFluidHandler.class);
        public static final EntityCapability<net.neoforged.neoforge.fluids.capability.IFluidHandler, net.minecraft.core.Direction> ENTITY =
                EntityCapability.createSided(ResourceLocation.fromNamespaceAndPath("neoforge", "fluid_handler"), net.neoforged.neoforge.fluids.capability.IFluidHandler.class);
        public static final ItemCapability<net.neoforged.neoforge.fluids.capability.IFluidHandlerItem, Void> ITEM =
                ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath("neoforge", "fluid_handler"), net.neoforged.neoforge.fluids.capability.IFluidHandlerItem.class);
        static {
            BLOCK.setProxyable(true);
        }
    }

    public static final class ItemHandler {
        private ItemHandler() {}

        public static final BlockCapability<net.neoforged.neoforge.items.IItemHandler, net.minecraft.core.Direction> BLOCK =
                BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("neoforge", "item_handler"), net.neoforged.neoforge.items.IItemHandler.class);
        public static final EntityCapability<net.neoforged.neoforge.items.IItemHandler, Void> ENTITY =
                EntityCapability.createVoid(ResourceLocation.fromNamespaceAndPath("neoforge", "item_handler"), net.neoforged.neoforge.items.IItemHandler.class);
        public static final EntityCapability<net.neoforged.neoforge.items.IItemHandler, net.minecraft.core.Direction> ENTITY_AUTOMATION =
                EntityCapability.createSided(ResourceLocation.fromNamespaceAndPath("neoforge", "item_handler_automation"), net.neoforged.neoforge.items.IItemHandler.class);
        public static final ItemCapability<net.neoforged.neoforge.items.IItemHandler, Void> ITEM =
                ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath("neoforge", "item_handler"), net.neoforged.neoforge.items.IItemHandler.class);
        static {
            BLOCK.setProxyable(true);
        }
    }
}
