package net.minecraft.world.level.material;

import net.neoforged.neoforge.common.extensions.IFluidExtension;

/** The stand-in for the game's Fluid: its id, given when it is made (HorizonRegistries). */
public abstract class Fluid implements IFluidExtension {
    private net.minecraft.resources.ResourceLocation horizonId;

    protected Fluid() {}

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
