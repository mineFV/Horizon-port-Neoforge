package net.neoforged.neoforge.registries.datamaps.builtin;

/** The stand-in for NeoForge's FurnaceFuel: an item's burn time in ticks, the furnace_fuels data map's value (its codecs aren't modelled). */
public record FurnaceFuel(int burnTime) {
}
