package net.neoforged.neoforge.capabilities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Not NeoForge's: the capabilities made, one for each name and kind, and the event that registers their providers,
 * which the bridge fires on the mod's bus once it is loaded (HorizonBridge.capabilities).
 */
public final class HorizonCapabilities {
    private HorizonCapabilities() {}

    private static final Map<String, BaseCapability<?, ?>> MADE = new LinkedHashMap<>();

    /** The capability of a kind ("block", "item", "entity") and name: the one made first, or the one maker makes now. */
    @SuppressWarnings("unchecked")
    static synchronized <K extends BaseCapability<?, ?>> K made(String kind, ResourceLocation name, Class<?> type, Class<?> context, java.util.function.Supplier<K> maker) {
        String key = kind + " " + name;
        BaseCapability<?, ?> old = MADE.get(key);
        if (old != null) {
            // Observed: the type class is checked first, then the context class.
            if (old.typeClass() != type) throw new IllegalStateException("Attempted to register capability " + name + " with existing type class " + old.typeClass() + " != " + type);
            if (old.contextClass() != context) throw new IllegalStateException("Attempted to register capability " + name + " with existing context class " + old.contextClass() + " != " + context);
            return (K) old;
        }
        K made = maker.get();
        MADE.put(key, made);
        return made;
    }

    /** Those of a kind, in the order they were made. */
    @SuppressWarnings("unchecked")
    static synchronized <K> List<K> all(Class<K> kind) {
        List<K> out = new ArrayList<>();
        for (BaseCapability<?, ?> c : MADE.values()) if (kind.isInstance(c)) out.add((K) c);
        return out;
    }

    /** A new event of registration, for the bridge to fire. */
    public static RegisterCapabilitiesEvent event() {
        return new RegisterCapabilitiesEvent();
    }
}
