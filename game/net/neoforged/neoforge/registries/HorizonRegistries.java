package net.neoforged.neoforge.registries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;

/**
 * Not the game's: horizon-port-neoforge's registry of what a mod registers. NeoForge makes each value when its
 * registry fills, before the world; here it is made the first time it is asked for (DeferredHolder.get), then kept:
 * the same object each time, made at another moment (a supplier with effects of its own could tell).
 */
public final class HorizonRegistries {
    private static final Map<ResourceKey<?>, Supplier<?>> SUPPLIERS = new HashMap<>();
    private static final Map<ResourceKey<?>, Object> VALUES = new HashMap<>();

    private HorizonRegistries() {}

    static synchronized void add(ResourceKey<?> key, Supplier<?> supplier) {
        SUPPLIERS.put(key, supplier);
    }

    /** The value registered at key, made the first time; null when nothing was registered there. */
    public static Object get(ResourceKey<?> key) {
        Supplier<?> supplier;
        synchronized (HorizonRegistries.class) {
            Object value = VALUES.get(key);
            if (value != null) return value;
            supplier = SUPPLIERS.get(key);
        }
        if (supplier == null) return null;
        Object made = supplier.get();
        synchronized (HorizonRegistries.class) {
            Object value = VALUES.get(key);
            if (value != null) return value;
            VALUES.put(key, made);
        }
        if (made instanceof net.minecraft.world.level.block.Block block) block.horizonRegistered(key.location());
        return made;
    }
}
