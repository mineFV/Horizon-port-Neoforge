package net.minecraft.world.level.material;

/** The stand-in for the game's FluidState: a state of a fluid, its answers its fluid's (observed, `standins --horizon-src`). */
public final class FluidState extends net.minecraft.world.level.block.state.StateHolder<Fluid, FluidState> implements net.neoforged.neoforge.common.extensions.IFluidStateExtension {
    /** Fluid's: the game's own constructor takes the state's properties as its maps, which aren't modelled. */
    public FluidState(Fluid fluid, java.util.Map<net.minecraft.world.level.block.state.properties.Property<?>, Comparable<?>> values) {
        super(fluid, values);
    }

    public Fluid getType() {
        return owner;
    }

    public boolean isEmpty() {
        return owner.isEmpty();
    }

    public boolean isSource() {
        return owner.isSource(this);
    }

    public int getAmount() {
        return owner.getAmount(this);
    }

    public boolean is(Fluid fluid) {
        return owner == fluid;
    }

    public java.util.stream.Stream<net.minecraft.tags.TagKey<Fluid>> getTags() {
        return owner.builtInRegistryHolder().tags();
    }
}
