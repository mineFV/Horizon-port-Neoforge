package net.neoforged.neoforge.common.extensions;

/** The stand-in for NeoForge's IFluidStateExtension: a fluid state's type, its fluid's. */
public interface IFluidStateExtension {
    default net.neoforged.neoforge.fluids.FluidType getFluidType() {
        return ((net.minecraft.world.level.material.FluidState) this).getType().getFluidType();
    }
}
