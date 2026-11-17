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
                other.horizonGiven(given(id));
                String name = givenName(id);
                if (name != null) other.horizonGivenName(name);
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
        if (registry.equals("components")) {
            given = new HashMap<>();
            for (String entry : ids.split(";")) {
                int eq = entry.indexOf('=');
                if (eq > 0) given.put(entry.substring(0, eq), entry.substring(eq + 1));
            }
            for (Map.Entry<String, net.minecraft.world.item.Item> e : ITEMS.entrySet()) {
                e.getValue().horizonGiven(given(e.getKey()));
                String name = givenName(e.getKey());
                if (name != null) e.getValue().horizonGivenName(name);
            }
            return;
        }
        if (registry.equals("component")) knownComponents = set;
        else if (registry.equals("item")) knownItems = set;
        else if (registry.equals("fluid")) knownFluids = set;
        else throw new IllegalArgumentException(registry);
    }

    private static Map<String, String> given;
    private static java.util.Set<String> knownComponents;

    /** Whether the game has a component type of this id (given: "component"). */
    public static boolean knownComponent(String id) {
        java.util.Set<String> known;
        synchronized (HorizonRegistries.class) {
            known = knownComponents;
        }
        if (known == null) throw new IllegalStateException("the game's component ids weren't given (HorizonRegistries.known)");
        return known.contains(id);
    }

    /**
     * The components of the game's item of an id, as given ("components": "id=type:value,...;..." in their order, a
     * value an int, a rarity's name or "empty" for an empty lore, enchantments or modifiers, and "other" when it has
     * one more that isn't modelled).
     */
    private static net.minecraft.core.component.DataComponentMap given(String id) {
        String text = given == null ? null : given.get(id);
        if (text == null) return new net.minecraft.core.component.HorizonGivenComponents(id, null, false);
        Map<net.minecraft.core.component.DataComponentType<?>, Object> values = new java.util.LinkedHashMap<>();
        boolean complete = true;
        for (String part : text.split(",")) {
            if (part.isEmpty()) continue;
            int colon = part.indexOf(':');
            String key = colon < 0 ? part : part.substring(0, colon), value = colon < 0 ? "" : part.substring(colon + 1);
            switch (key) {
                case "max_stack_size" -> values.put(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, Integer.parseInt(value));
                case "max_damage" -> values.put(net.minecraft.core.component.DataComponents.MAX_DAMAGE, Integer.parseInt(value));
                case "damage" -> values.put(net.minecraft.core.component.DataComponents.DAMAGE, Integer.parseInt(value));
                case "repair_cost" -> values.put(net.minecraft.core.component.DataComponents.REPAIR_COST, Integer.parseInt(value));
                case "rarity" -> {
                    for (net.minecraft.world.item.Rarity r : net.minecraft.world.item.Rarity.values()) if (r.getSerializedName().equals(value)) values.put(net.minecraft.core.component.DataComponents.RARITY, r);
                }
                case "fire_resistant" -> values.put(net.minecraft.core.component.DataComponents.FIRE_RESISTANT, net.minecraft.util.Unit.INSTANCE);
                case "name" -> {
                    // Its description id: not a component (givenName).
                }
                case "lore" -> values.put(net.minecraft.core.component.DataComponents.LORE, net.minecraft.world.item.component.ItemLore.EMPTY);
                case "enchantments" -> values.put(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
                case "attribute_modifiers" -> values.put(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
                default -> complete = false;
            }
        }
        return new net.minecraft.core.component.HorizonGivenComponents(id, values, complete);
    }

    /** The description id given for the game's item of an id ("name:..." among its components), when it isn't its own. */
    private static String givenName(String id) {
        String text = given == null ? null : given.get(id);
        if (text == null) return null;
        for (String part : text.split(",")) if (part.startsWith("name:")) return part.substring(5);
        return null;
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

    /** The fluid registered under an id (the mod's own, else the game's); null when there is none. */
    public static net.minecraft.world.level.material.Fluid knownFluid(String id) {
        Object own = get(ResourceKey.create(net.minecraft.core.registries.Registries.FLUID, net.minecraft.resources.ResourceLocation.parse(id)));
        if (own instanceof net.minecraft.world.level.material.Fluid fluid) return fluid;
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
        if (made instanceof net.minecraft.core.component.DataComponentType<?> type) net.minecraft.core.component.DataComponentType.horizonRegistered(type, key.location());
        return made;
    }
}
