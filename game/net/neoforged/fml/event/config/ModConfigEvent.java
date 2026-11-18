package net.neoforged.fml.event.config;

import net.neoforged.fml.config.ModConfig;

/** The stand-in for FML's ModConfigEvent: a config loaded (the bridge fires Loading once the mod is loaded) or reloaded. */
public class ModConfigEvent extends net.neoforged.bus.api.Event implements net.neoforged.fml.event.IModBusEvent {
    private final ModConfig config;

    ModConfigEvent(ModConfig config) {
        this.config = config;
    }

    public ModConfig getConfig() {
        return config;
    }

    public static class Loading extends ModConfigEvent {
        public Loading(ModConfig config) {
            super(config);
        }
    }

    public static class Reloading extends ModConfigEvent {
        public Reloading(ModConfig config) {
            super(config);
        }
    }
}
