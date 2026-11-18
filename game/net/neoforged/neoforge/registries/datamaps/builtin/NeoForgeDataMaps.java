package net.neoforged.neoforge.registries.datamaps.builtin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/** The stand-in for NeoForge's NeoForgeDataMaps: the furnace fuels (observed); the other data maps aren't here yet. */
public class NeoForgeDataMaps {
    public static final DataMapType<Item, FurnaceFuel> FURNACE_FUELS = DataMapType.<FurnaceFuel, Item>builder(
            ResourceLocation.fromNamespaceAndPath("neoforge", "furnace_fuels"), Registries.ITEM, null).build();

    public NeoForgeDataMaps() {}
}
