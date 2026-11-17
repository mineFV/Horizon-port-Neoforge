package net.neoforged.fml;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforgespi.language.IModInfo;

/** The stand-in for NeoForge's ModContainer: a mod's info, and what it registers with it (kept, not read yet). */
public abstract class ModContainer {
    protected final IModInfo modInfo;
    private final List<Object> registered = new ArrayList<>();

    public ModContainer(IModInfo info) {
        this.modInfo = info;
    }

    public final String getModId() {
        return modInfo.getModId();
    }

    public final String getNamespace() {
        return modInfo.getNamespace();
    }

    public IModInfo getModInfo() {
        return modInfo;
    }

    public <T extends IExtensionPoint> void registerExtensionPoint(Class<T> point, T extension) {
        registered.add(extension);
    }

    public <T extends IExtensionPoint> void registerExtensionPoint(Class<T> point, Supplier<T> extension) {
        registered.add(extension);
    }

    public void registerConfig(ModConfig.Type type, IConfigSpec spec) {
        registered.add(spec);
    }

    public void registerConfig(ModConfig.Type type, IConfigSpec spec, String fileName) {
        registered.add(spec);
    }

    public abstract IEventBus getEventBus();
}
