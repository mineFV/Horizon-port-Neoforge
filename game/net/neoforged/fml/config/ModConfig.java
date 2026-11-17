package net.neoforged.fml.config;

/** The stand-in for NeoForge's ModConfig: its kinds of config. */
public final class ModConfig {
    private ModConfig() {}

    /** The stand-in for ModConfig.Type, in its order. */
    public enum Type {
        COMMON,
        CLIENT,
        SERVER,
        STARTUP
    }
}
