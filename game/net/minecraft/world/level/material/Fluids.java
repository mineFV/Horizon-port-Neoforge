package net.minecraft.world.level.material;

import net.neoforged.neoforge.registries.HorizonRegistries;

/** The stand-in for the game's Fluids: the game's fluids a mod names, made by id (HorizonRegistries). */
public class Fluids {
    public static final Fluid EMPTY = HorizonRegistries.fluid("minecraft:empty");
    public static final FlowingFluid FLOWING_WATER = (FlowingFluid) HorizonRegistries.fluid("minecraft:flowing_water");
    public static final FlowingFluid WATER = (FlowingFluid) HorizonRegistries.fluid("minecraft:water");
    public static final FlowingFluid FLOWING_LAVA = (FlowingFluid) HorizonRegistries.fluid("minecraft:flowing_lava");
    public static final FlowingFluid LAVA = (FlowingFluid) HorizonRegistries.fluid("minecraft:lava");

    private Fluids() {}
}
