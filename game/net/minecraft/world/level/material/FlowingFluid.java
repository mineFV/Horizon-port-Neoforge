package net.minecraft.world.level.material;

/** The stand-in for the game's FlowingFluid: the type of the game's water and lava (their flowing isn't modelled). */
public abstract class FlowingFluid extends Fluid {
    protected FlowingFluid() {}

    public FluidState getSource(boolean falling) {
        throw new UnsupportedOperationException("fluid states aren't modelled");
    }
}
