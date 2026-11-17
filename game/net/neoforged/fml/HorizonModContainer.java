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

    /** The mod's info: its namespace is its id. */
    private record Info(String getModId, String getDisplayName, ArtifactVersion getVersion) implements IModInfo {
        @Override
        public String getNamespace() {
            return getModId;
        }
    }
}
