package net.neoforged.fml;

import java.util.Optional;
import java.util.Set;

/**
 * The stand-in for NeoForge's ModList: the mods loaded beside this one. In its translation, the mod itself, the game and
 * NeoForge: another mod ported to Horizon runs in a translation of its own, which this one doesn't see.
 */
public class ModList {
    private static final ModList LIST = new ModList();
    private ModContainer own;

    private ModList() {}

    public static ModList get() {
        return LIST;
    }

    void horizonAdd(ModContainer container) {
        own = container;
    }

    public boolean isLoaded(String modId) {
        return Set.of("minecraft", "neoforge").contains(modId) || own != null && own.getModId().equals(modId);
    }

    public Optional<? extends ModContainer> getModContainerById(String modId) {
        return own != null && own.getModId().equals(modId) ? Optional.of(own) : Optional.empty();
    }
}
