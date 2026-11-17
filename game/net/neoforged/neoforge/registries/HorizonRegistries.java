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

    private static final Map<String, net.minecraft.world.item.Item> ITEMS = new HashMap<>();
    private static final Map<String, net.minecraft.world.level.material.Fluid> FLUIDS = new HashMap<>();

    /** The item registered under an id: the mod's own, else one made for that id (the game's), the same each time. */
    public static net.minecraft.world.item.Item item(String id) {
        net.minecraft.resources.ResourceLocation location = net.minecraft.resources.ResourceLocation.parse(id);
        Object own = get(ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, location));
        if (own instanceof net.minecraft.world.item.Item item) return item;
        synchronized (HorizonRegistries.class) {
            net.minecraft.world.item.Item other = ITEMS.get(id);
            if (other == null) {
                other = new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties());
                other.horizonRegistered(location);
                ITEMS.put(id, other);
            }
            return other;
        }
    }

    /** The fluid of an id, made once (the game's: a mod's own fluids aren't modelled yet). */
    public static synchronized net.minecraft.world.level.material.Fluid fluid(String id) {
        net.minecraft.world.level.material.Fluid f = FLUIDS.get(id);
        if (f == null) {
            f = new net.minecraft.world.level.material.Fluid() {};
            f.horizonRegistered(net.minecraft.resources.ResourceLocation.parse(id));
            FLUIDS.put(id, f);
        }
        return f;
    }

    private static java.util.Set<String> knownItems;
    private static java.util.Set<String> knownFluids;

    /**
     * The game's ids of a registry ("item" or "fluid"), comma-separated, as `dump` read them in NeoForge: the bridge
     * gives them before the mod loads (the facts' vanilla_items), the stand-ins' probe gives its own. Not the game's API.
     */
    public static synchronized void known(String registry, String ids) {
        java.util.Set<String> set = new java.util.HashSet<>();
        for (String id : ids.split(",")) if (!id.isEmpty()) set.add(id);
        if (registry.equals("item")) knownItems = set;
        else if (registry.equals("fluid")) knownFluids = set;
        else throw new IllegalArgumentException(registry);
    }

    /** The item registered under an id (the mod's own or the game's); null when there is none. */
    public static net.minecraft.world.item.Item knownItem(String id) {
        net.minecraft.resources.ResourceLocation location = net.minecraft.resources.ResourceLocation.parse(id);
        Object own = get(ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, location));
        if (own instanceof net.minecraft.world.item.Item item) return item;
        java.util.Set<String> known;
        synchronized (HorizonRegistries.class) {
            known = knownItems;
        }
        if (known == null) throw new IllegalStateException("the game's item ids weren't given (HorizonRegistries.known)");
        return known.contains(id) ? item(id) : null;
    }

    /** The fluid of an id, the game's (a mod's own fluids aren't modelled yet); null when there is none. */
    public static net.minecraft.world.level.material.Fluid knownFluid(String id) {
        java.util.Set<String> known;
        synchronized (HorizonRegistries.class) {
            known = knownFluids;
        }
        if (known == null) throw new IllegalStateException("the game's fluid ids weren't given (HorizonRegistries.known)");
        return known.contains(id) ? fluid(id) : null;
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
        if (made instanceof net.minecraft.world.item.Item item) item.horizonRegistered(key.location());
        return made;
    }
}
