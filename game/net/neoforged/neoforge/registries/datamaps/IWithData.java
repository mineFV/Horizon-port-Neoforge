package net.neoforged.neoforge.registries.datamaps;

/**
 * The stand-in for NeoForge's IWithData: a value's data maps. Data maps (data packs' values for registry entries, such
 * as furnace fuels) aren't modelled: asking one says so, where the game would answer from the data packs.
 */
public interface IWithData<R> {
    default <T> T getData(DataMapType<R, T> type) {
        throw new UnsupportedOperationException("the data map " + type.id() + " isn't modelled (horizon-port-neoforge)");
    }
}
