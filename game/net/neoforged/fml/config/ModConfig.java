package net.neoforged.fml.config;

/** The stand-in for NeoForge's ModConfig: a config a mod registered, its kind, spec and file (observed, `standins --horizon-src`). */
public final class ModConfig {
    private final Type type;
    private final IConfigSpec spec;
    private final String modId;
    private final String fileName;

    private ModConfig(Type type, IConfigSpec spec, String modId, String fileName) {
        this.type = type;
        this.spec = spec;
        this.modId = modId;
        this.fileName = fileName;
    }

    /** ModContainer.registerConfig's. Not the game's API. */
    public static ModConfig horizonMake(Type type, IConfigSpec spec, String modId, String fileName) {
        return new ModConfig(type, spec, modId, fileName);
    }

    public Type getType() {
        return type;
    }

    public String getFileName() {
        return fileName;
    }

    public IConfigSpec getSpec() {
        return spec;
    }

    public String getModId() {
        return modId;
    }

    /** The stand-in for ModConfig.Type, in its order, with the name each one's file takes. */
    public enum Type {
        COMMON,
        CLIENT,
        SERVER,
        STARTUP;

        public String extension() {
            return switch (this) {
                case COMMON -> "common";
                case CLIENT -> "client";
                case SERVER -> "server";
                case STARTUP -> "startup";
            };
        }
    }
}
