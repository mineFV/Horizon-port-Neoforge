package net.neoforged.neoforge.fluids.capability;

/** The stand-in for NeoForge's IFluidHandlerItem: its method (a mod's implement it). */
public interface IFluidHandlerItem extends IFluidHandler {
    net.minecraft.world.item.ItemStack getContainer();
}
