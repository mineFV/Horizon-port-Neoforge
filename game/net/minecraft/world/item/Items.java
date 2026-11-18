package net.minecraft.world.item;

import net.neoforged.neoforge.registries.HorizonRegistries;

/** The stand-in for the game's Items: the game's items a mod names, made by id (HorizonRegistries). Others aren't here yet. */
public class Items {
    public static final Item AIR = HorizonRegistries.item("minecraft:air");
    public static final Item STONE = HorizonRegistries.item("minecraft:stone");
    public static final Item DIAMOND = HorizonRegistries.item("minecraft:diamond");
    public static final Item EMERALD = HorizonRegistries.item("minecraft:emerald");
    public static final Item DIAMOND_SWORD = HorizonRegistries.item("minecraft:diamond_sword");
    public static final Item COAL = HorizonRegistries.item("minecraft:coal");
    public static final Item STICK = HorizonRegistries.item("minecraft:stick");
    public static final Item LAVA_BUCKET = HorizonRegistries.item("minecraft:lava_bucket");

    private Items() {}
}
