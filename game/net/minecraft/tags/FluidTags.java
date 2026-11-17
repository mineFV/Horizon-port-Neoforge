package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

/** The stand-in for the game's FluidTags: water and lava, and making a fluid tag's key. */
public final class FluidTags {
    private FluidTags() {}

    public static final TagKey<net.minecraft.world.level.material.Fluid> WATER = create(ResourceLocation.withDefaultNamespace("water"));
    public static final TagKey<net.minecraft.world.level.material.Fluid> LAVA = create(ResourceLocation.withDefaultNamespace("lava"));

    public static TagKey<net.minecraft.world.level.material.Fluid> create(ResourceLocation id) {
        return TagKey.create(Registries.FLUID, id);
    }
}
