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

    /** Observed in a server (`standins --horizon-src`): see ItemProbe. */
    @Override
    public String toString() {
        return String.valueOf(horizonId);
    }
}
