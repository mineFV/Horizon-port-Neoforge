package net.neoforged.api.distmarker;

/** The stand-in for NeoForge's Dist. */
public enum Dist {
    CLIENT,
    DEDICATED_SERVER;

    public boolean isDedicatedServer() {
        return this == DEDICATED_SERVER;
    }

    public boolean isClient() {
        return this == CLIENT;
    }
}
