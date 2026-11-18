package net.minecraft.world.level.material;

/** The stand-in for the game's FluidState: its fluid (fluid states aren't made yet: their properties aren't modelled). */
public final class FluidState extends net.minecraft.world.level.block.state.StateHolder<Fluid, FluidState> implements net.neoforged.neoforge.common.extensions.IFluidStateExtension {
    private FluidState(Fluid fluid) {
        super(fluid, java.util.Map.of());
    }

    public Fluid getType() {
        return owner;
    }
}
