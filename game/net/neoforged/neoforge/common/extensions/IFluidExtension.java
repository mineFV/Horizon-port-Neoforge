package net.neoforged.neoforge.common.extensions;

/** The stand-in for the game's IFluidExtension: a fluid's type. */
public interface IFluidExtension {
    net.neoforged.neoforge.fluids.FluidType getFluidType();
}
