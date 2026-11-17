package net.minecraft.world.level.material;

import net.neoforged.neoforge.registries.HorizonRegistries;

/** The stand-in for the game's Fluids: the game's fluids a mod names, made by id (HorizonRegistries). */
public class Fluids {
    public static final Fluid EMPTY = HorizonRegistries.fluid("minecraft:empty");
    public static final Fluid WATER = HorizonRegistries.fluid("minecraft:water");
    public static final Fluid LAVA = HorizonRegistries.fluid("minecraft:lava");

    private Fluids() {}
}
