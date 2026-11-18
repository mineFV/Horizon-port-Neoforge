package net.minecraft.world.level.material;

/**
 * The stand-in for the game's FlowingFluid: the type of the game's water and lava, their states' falling (observed);
 * their flowing isn't modelled.
 */
public abstract class FlowingFluid extends Fluid {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty FALLING = net.minecraft.world.level.block.state.properties.BlockStateProperties.FALLING;

    protected FlowingFluid() {}

    @Override
    protected void createFluidStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Fluid, FluidState> builder) {
        builder.add(FALLING);
    }

    @Override
    public abstract int getAmount(FluidState state);

    public abstract Fluid getSource();

    public abstract Fluid getFlowing();

    public FluidState getSource(boolean falling) {
        return getSource().defaultFluidState().setValue(FALLING, falling);
    }
}
