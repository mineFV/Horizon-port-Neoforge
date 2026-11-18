package net.minecraft.world.level.material;

import net.neoforged.neoforge.common.extensions.IFluidExtension;

/** The stand-in for the game's Fluid: its id, given when it is made (HorizonRegistries). */
public abstract class Fluid implements IFluidExtension {
    private net.minecraft.resources.ResourceLocation horizonId;

    protected final net.minecraft.world.level.block.state.StateDefinition<Fluid, FluidState> stateDefinition;
    private FluidState defaultFluidState;

    protected Fluid() {
        net.minecraft.world.level.block.state.StateDefinition.Builder<Fluid, FluidState> builder = new net.minecraft.world.level.block.state.StateDefinition.Builder<>(this);
        createFluidStateDefinition(builder);
        this.stateDefinition = builder.horizonCreate(FluidState::new);
        registerDefaultState(stateDefinition.any());
    }

    protected void createFluidStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Fluid, FluidState> builder) {}

    protected final void registerDefaultState(FluidState state) {
        this.defaultFluidState = state;
    }

    /** Its first state; refused for the game's flowing water and lava, whose level isn't modelled. */
    public final FluidState defaultFluidState() {
        if (horizonLevelled()) throw new UnsupportedOperationException("the states of " + this + " aren't modelled: their level (horizon-port-neoforge)");
        return defaultFluidState;
    }

    public net.minecraft.world.level.block.state.StateDefinition<Fluid, FluidState> getStateDefinition() {
        return stateDefinition;
    }

    /** HorizonRegistries' game fluids: whether its states have a level, which isn't modelled. Not the game's API. */
    protected boolean horizonLevelled() {
        return false;
    }

    protected boolean isEmpty() {
        return false;
    }

    public abstract boolean isSource(FluidState state);

    public abstract int getAmount(FluidState state);

    public boolean isSame(Fluid fluid) {
        return fluid == this;
    }

    /** HorizonRegistries': the id this fluid was registered under. Not the game's API. */
    public void horizonRegistered(net.minecraft.resources.ResourceLocation id) {
        this.horizonId = id;
    }

    public net.minecraft.resources.ResourceLocation horizonId() {
        return horizonId;
    }

    private net.minecraft.core.Holder.Reference<Fluid> holder;

    /** Its holder: one for each fluid, its key the fluid registry's and its id (observed). */
    public net.minecraft.core.Holder.Reference<Fluid> builtInRegistryHolder() {
        if (holder == null) holder = new net.minecraft.core.Holder.Reference<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.FLUID, horizonId), this);
        return holder;
    }

    /** Observed in a server (`standins --horizon-src`): see ItemProbe. */
    @Override
    public String toString() {
        return String.valueOf(horizonId);
    }
}
