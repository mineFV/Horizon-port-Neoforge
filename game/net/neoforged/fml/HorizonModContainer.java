package net.neoforged.fml;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/** Not NeoForge's: the mod's container as the bridge makes it, from its neoforge.mods.toml, with its event bus. */
public final class HorizonModContainer extends ModContainer {
    private final IEventBus bus;

    public HorizonModContainer(String modId, String version, String displayName, IEventBus bus) {
        super(new Info(modId, displayName, new DefaultArtifactVersion(version)));
        this.bus = bus;
        ModList.get().horizonAdd(this);
    }

    @Override
    public IEventBus getEventBus() {
        return bus;
    }

    /**
     * The bridge's: each config the mod registered, loaded (its values their defaults: ModConfigSpec), then
     * ModConfigEvent.Loading fired for it on the mod's bus, in the order they were registered.
     */
    /** The text of a new file of the config registered under a file name (ModConfigSpec's), null when there is none. */
    public String horizonDefaultText(String fileName) {
        for (net.neoforged.fml.config.ModConfig config : horizonConfigs()) {
            if (config.getFileName().equals(fileName) && config.getSpec() instanceof net.neoforged.neoforge.common.ModConfigSpec spec) return spec.horizonDefaultText();
        }
        return null;
    }

    /**
     * The bridge's: the config registered under a file name read from that file's text (ModConfigSpec), then
     * ModConfigEvent.Loading fired for it on the mod's bus, or Reloading when it was read before. False when there is no
     * such config.
     */
    public boolean horizonLoadConfig(String fileName, String text, boolean reloading) {
        for (net.neoforged.fml.config.ModConfig config : horizonConfigs()) {
            if (!config.getFileName().equals(fileName)) continue;
            if (config.getSpec() instanceof net.neoforged.neoforge.common.ModConfigSpec spec) spec.horizonLoad(text);
            bus.post(reloading ? new net.neoforged.fml.event.config.ModConfigEvent.Reloading(config) : new net.neoforged.fml.event.config.ModConfigEvent.Loading(config));
            return true;
        }
        return false;
    }

    public void horizonLoadConfigs() {
        for (net.neoforged.fml.config.ModConfig config : horizonConfigs()) {
            if (config.getSpec() instanceof net.neoforged.neoforge.common.ModConfigSpec spec) spec.horizonLoad();
            bus.post(new net.neoforged.fml.event.config.ModConfigEvent.Loading(config));
        }
    }

    /** The mod's info: its namespace is its id. */
    private record Info(String getModId, String getDisplayName, ArtifactVersion getVersion) implements IModInfo {
        @Override
        public String getNamespace() {
            return getModId;
        }
    }
}
