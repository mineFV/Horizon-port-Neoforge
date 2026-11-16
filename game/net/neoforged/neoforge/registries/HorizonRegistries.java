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

    private static final Map<String, net.minecraft.world.level.block.Block> OTHERS = new HashMap<>();

    /**
     * The block registered under an id: the mod's own, else one made for that id (a block of the game's or of another
     * mod's, which only says what it is: its behaviour isn't modelled), the same object each time.
     */
    public static net.minecraft.world.level.block.Block block(String id) {
        net.minecraft.resources.ResourceLocation location = net.minecraft.resources.ResourceLocation.parse(id);
        Object own = get(ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, location));
        if (own instanceof net.minecraft.world.level.block.Block block) return block;
        synchronized (HorizonRegistries.class) {
            net.minecraft.world.level.block.Block other = OTHERS.get(id);
            if (other == null) {
                other = new net.minecraft.world.level.block.Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
                other.horizonRegistered(location);
                OTHERS.put(id, other);
            }
            return other;
        }
    }

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
