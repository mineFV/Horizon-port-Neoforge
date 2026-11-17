package net.neoforged.neoforge.fluids.capability;

import net.neoforged.neoforge.fluids.FluidStack;

/** The stand-in for NeoForge's IFluidHandler: its methods (a mod's implement them) and FluidAction. */
public interface IFluidHandler {
    int getTanks();

    FluidStack getFluidInTank(int tank);

    int getTankCapacity(int tank);

    boolean isFluidValid(int tank, FluidStack stack);

    int fill(FluidStack resource, FluidAction action);

    FluidStack drain(FluidStack resource, FluidAction action);

    FluidStack drain(int maxDrain, FluidAction action);

    /** Observed (`standins --horizon-src`): each says whether it is itself. */
    enum FluidAction {
        EXECUTE,
        SIMULATE;

        public boolean execute() {
            return this == EXECUTE;
        }

        public boolean simulate() {
            return this == SIMULATE;
        }
    }
}
