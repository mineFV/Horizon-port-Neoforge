package net.neoforged.neoforge.registries.datamaps;

/**
 * The stand-in for NeoForge's IWithData: a value's data maps (data packs' values for registry entries). Those of a
 * holder that HorizonRegistries was given (the furnace fuels, which `dump` reads) are answered; asking another says so.
 */
public interface IWithData<R> {
    default <T> T getData(DataMapType<R, T> type) {
        return net.neoforged.neoforge.registries.HorizonRegistries.data(this, type);
    }
}
