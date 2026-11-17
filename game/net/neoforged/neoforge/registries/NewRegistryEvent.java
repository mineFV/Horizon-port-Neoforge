package net.neoforged.neoforge.registries;

import net.minecraft.core.Registry;

/** The stand-in for NeoForge's NewRegistryEvent: the bridge fires it once the mod is loaded; registering changes nothing here. */
public class NewRegistryEvent extends net.neoforged.bus.api.Event implements net.neoforged.fml.event.IModBusEvent {
    NewRegistryEvent() {}

    public <T> Registry<T> create(RegistryBuilder<T> builder) {
        return builder.create();
    }

    public <T> void register(Registry<T> registry) {}
}
