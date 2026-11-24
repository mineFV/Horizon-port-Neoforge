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
    public static final Item TOTEM_OF_UNDYING = HorizonRegistries.item("minecraft:totem_of_undying");
    public static final Item BUCKET = HorizonRegistries.item("minecraft:bucket");
    public static final Item FURNACE = HorizonRegistries.item("minecraft:furnace");
    public static final Item FIRE_CHARGE = HorizonRegistries.item("minecraft:fire_charge");
    public static final Item CRAFTING_TABLE = HorizonRegistries.item("minecraft:crafting_table");
    public static final Item COBBLESTONE = HorizonRegistries.item("minecraft:cobblestone");

    private Items() {}
}
