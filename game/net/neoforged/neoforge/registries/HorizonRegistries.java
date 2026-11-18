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
    private static final Map<ResourceKey<?>, Supplier<?>> SUPPLIERS = new java.util.LinkedHashMap<>();
    private static final Map<ResourceKey<?>, Object> VALUES = new HashMap<>();
    /** The key of each value made, by identity. */
    private static final Map<Object, ResourceKey<?>> KEYS = new java.util.IdentityHashMap<>();

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
                other.horizonOther(true);
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
                if (itemBlocks == null) throw new IllegalStateException("whether " + id + " is a block's item wasn't given (HorizonRegistries.known: item_blocks)");
                String blockId = itemBlocks.get(id);
                other = blockId == null ? new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties())
                        : new net.minecraft.world.item.BlockItem(block(blockId), new net.minecraft.world.item.Item.Properties());
                other.horizonRegistered(location);
                other.horizonGiven(given(id));
                String name = givenName(id);
                if (name != null) other.horizonGivenName(name);
                ITEMS.put(id, other);
                remainder(id, other);
            }
            return other;
        }
    }

    /** The fluid of an id, made once (the game's: a mod's own fluids aren't modelled yet). */
    public static synchronized net.minecraft.world.level.material.Fluid fluid(String id) {
        net.minecraft.world.level.material.Fluid f = FLUIDS.get(id);
        if (f == null) {
            // Observed (FluidTypeProbe): the game's water and lava, still and flowing, are FlowingFluids.
            f = switch (id) {
                case "minecraft:water", "minecraft:flowing_water", "minecraft:lava", "minecraft:flowing_lava" -> new net.minecraft.world.level.material.FlowingFluid() {
                    // Observed (FluidProbe): a source's states are sources of 8; the flowing ones' have a level, not modelled.
                    private final boolean flowing = id.contains("flowing_");
                    private final String kind = id.endsWith("water") ? "water" : "lava";

                    @Override
                    public net.neoforged.neoforge.fluids.FluidType getFluidType() {
                        return fluidType(horizonId());
                    }

                    @Override
                    protected boolean horizonLevelled() {
                        return flowing;
                    }

                    @Override
                    public net.minecraft.world.level.material.Fluid getSource() {
                        return fluid("minecraft:" + kind);
                    }

                    @Override
                    public net.minecraft.world.level.material.Fluid getFlowing() {
                        return fluid("minecraft:flowing_" + kind);
                    }

                    @Override
                    public boolean isSource(net.minecraft.world.level.material.FluidState state) {
                        return !flowing;
                    }

                    @Override
                    public int getAmount(net.minecraft.world.level.material.FluidState state) {
                        if (flowing) throw new UnsupportedOperationException("the level of " + this + " isn't modelled (horizon-port-neoforge)");
                        return 8;
                    }
                };
                case "minecraft:empty" -> new net.minecraft.world.level.material.Fluid() {
                    @Override
                    public net.neoforged.neoforge.fluids.FluidType getFluidType() {
                        return fluidType(horizonId());
                    }

                    @Override
                    protected boolean isEmpty() {
                        return true;
                    }

                    @Override
                    public boolean isSource(net.minecraft.world.level.material.FluidState state) {
                        return false;
                    }

                    @Override
                    public int getAmount(net.minecraft.world.level.material.FluidState state) {
                        return 0;
                    }
                };
                default -> new net.minecraft.world.level.material.Fluid() {
                    @Override
                    public net.neoforged.neoforge.fluids.FluidType getFluidType() {
                        return fluidType(horizonId());
                    }

                    @Override
                    public boolean isSource(net.minecraft.world.level.material.FluidState state) {
                        throw new UnsupportedOperationException("the states of " + this + " aren't modelled (horizon-port-neoforge)");
                    }

                    @Override
                    public int getAmount(net.minecraft.world.level.material.FluidState state) {
                        throw new UnsupportedOperationException("the states of " + this + " aren't modelled (horizon-port-neoforge)");
                    }
                };
            };
            f.horizonRegistered(net.minecraft.resources.ResourceLocation.parse(id));
            FLUIDS.put(id, f);
        }
        return f;
    }

    /** Observed: the game's fluids' types, NeoForgeMod's; another library's fluid's type isn't modelled. */
    private static net.neoforged.neoforge.fluids.FluidType fluidType(net.minecraft.resources.ResourceLocation id) {
        return switch (String.valueOf(id)) {
            case "minecraft:empty" -> net.neoforged.neoforge.common.NeoForgeMod.EMPTY_TYPE.value();
            case "minecraft:water", "minecraft:flowing_water" -> net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value();
            case "minecraft:lava", "minecraft:flowing_lava" -> net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value();
            default -> throw new UnsupportedOperationException("the fluid type of " + id + " isn't modelled");
        };
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
        if (registry.startsWith("tags_")) {
            net.minecraft.tags.HorizonTags.given(registry.substring(5), ids, registry.equals("tags_fluid"));
            return;
        }
        if (registry.equals("namespaces")) {
            net.minecraft.tags.HorizonTags.namespaces(ids);
            return;
        }
        if (registry.equals("item_blocks") || registry.equals("block_items")) {
            Map<String, String> pairs = new HashMap<>();
            for (String entry : ids.split(";")) {
                int eq = entry.indexOf('=');
                if (eq > 0) pairs.put(entry.substring(0, eq), entry.substring(eq + 1));
            }
            if (registry.equals("item_blocks")) itemBlocks = pairs;
            else blockItems = pairs;
            return;
        }
        if (registry.equals("remainders")) {
            remainders = new HashMap<>();
            for (String entry : ids.split(";")) {
                int eq = entry.indexOf('=');
                if (eq > 0) remainders.put(entry.substring(0, eq), entry.substring(eq + 1));
            }
            for (Map.Entry<String, net.minecraft.world.item.Item> e : new java.util.ArrayList<>(ITEMS.entrySet())) remainder(e.getKey(), e.getValue());
            return;
        }
        if (registry.equals("furnace_fuels")) {
            furnaceFuels = new HashMap<>();
            for (String entry : ids.split(";")) {
                int eq = entry.indexOf('=');
                if (eq > 0) furnaceFuels.put(entry.substring(0, eq), Integer.parseInt(entry.substring(eq + 1)));
            }
            return;
        }
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
        else if (registry.equals("block")) knownBlocks = set;
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

    private static java.util.Set<String> knownBlocks;
    private static final Map<ResourceKey<?>, net.minecraft.core.Holder.Reference<?>> HOLDERS = new HashMap<>();

    /** The one holder of a key's value, for values that don't keep theirs (all but items, blocks and fluids). */
    @SuppressWarnings("unchecked")
    public static synchronized <T> net.minecraft.core.Holder.Reference<T> holder(ResourceKey<?> key, T value) {
        return (net.minecraft.core.Holder.Reference<T>) HOLDERS.computeIfAbsent(key, k -> new net.minecraft.core.Holder.Reference<>((ResourceKey<T>) k, value));
    }

    /** The id a value was registered under: the mod's registrations', else an item's, a block's, a fluid's or a component type's own; null when none. */
    public static net.minecraft.resources.ResourceLocation idOf(Object value) {
        synchronized (HorizonRegistries.class) {
            ResourceKey<?> key = KEYS.get(value);
            if (key != null) return key.location();
        }
        if (value instanceof net.minecraft.world.item.Item item) return item.horizonId();
        if (value instanceof net.minecraft.world.level.block.Block block) return block.horizonId();
        if (value instanceof net.minecraft.world.level.material.Fluid fluid) return fluid.horizonId();
        if (value instanceof net.minecraft.core.component.DataComponentType<?> type) return net.minecraft.core.component.DataComponentType.horizonId(type);
        return null;
    }

    /** The block registered under an id (the mod's own or the game's, as given: "block"); null when there is none. */
    public static net.minecraft.world.level.block.Block knownBlock(String id) {
        Object own = get(ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.parse(id)));
        if (own instanceof net.minecraft.world.level.block.Block block) return block;
        java.util.Set<String> known;
        synchronized (HorizonRegistries.class) {
            known = knownBlocks;
        }
        if (known == null) throw new IllegalStateException("the game's block ids weren't given (HorizonRegistries.known)");
        return known.contains(id) ? block(id) : null;
    }

    /** A new NewRegistryEvent, for the bridge to fire. Not the game's API. */
    public static NewRegistryEvent newRegistryEvent() {
        return new NewRegistryEvent();
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

    /** The game's own value at key, bound now: its holder, and its id for idOf. */
    public static synchronized <T> net.minecraft.core.Holder.Reference<T> bind(ResourceKey<T> key, T value) {
        VALUES.put(key, value);
        KEYS.put(value, key);
        return holder(key, value);
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
            KEYS.put(made, key);
        }
        if (made instanceof net.minecraft.world.level.block.Block block) block.horizonRegistered(key.location());
        if (made instanceof net.minecraft.world.item.Item item) item.horizonRegistered(key.location());
        if (made instanceof net.minecraft.world.item.BlockItem blockItem) {
            synchronized (HorizonRegistries.class) {
                blockItem.registerBlocks(net.minecraft.world.item.Item.BY_BLOCK, blockItem);
            }
        }
        if (made instanceof net.minecraft.core.component.DataComponentType<?> type) net.minecraft.core.component.DataComponentType.horizonRegistered(type, key.location());
        return made;
    }

    private static Map<String, Integer> furnaceFuels;

    /**
     * IWithData's: a holder's value in a data map. The furnace fuels are given whole ("furnace_fuels": "id=ticks;...",
     * every item the game's data map holds, as `dump` read it); an item not in them has none. Other maps aren't modelled.
     * Not the game's API.
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T> T data(Object holder, net.neoforged.neoforge.registries.datamaps.DataMapType<?, T> type) {
        if (type == net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps.FURNACE_FUELS && holder instanceof net.minecraft.core.Holder<?> h) {
            if (furnaceFuels == null) throw new IllegalStateException("the furnace fuels weren't given (HorizonRegistries.known)");
            net.minecraft.resources.ResourceKey<?> key = h.unwrapKey().orElse(null);
            if (key == null) return null;
            Integer ticks = furnaceFuels.get(key.location().toString());
            return ticks == null ? null : (T) new net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel(ticks);
        }
        throw new UnsupportedOperationException("the data map " + type.id() + " isn't modelled (horizon-port-neoforge)");
    }

    private static Map<String, String> remainders;

    /** What an item of the game's leaves in a crafting grid, from "remainders" (whole: the game's items that leave one), when given. */
    private static void remainder(String id, net.minecraft.world.item.Item item) {
        if (remainders == null) {
            item.horizonGivenRemainder(null, false);
            return;
        }
        String r = remainders.get(id);
        item.horizonGivenRemainder(r == null ? null : item(r), true);
    }

    /** The game's items that stand for a block, with it ("item_blocks": "item=block;...", whole), as `dump` read them. */
    private static Map<String, String> itemBlocks;
    /** The game's blocks' items ("block_items": "block=item;...", whole: Item.BY_BLOCK's), as `dump` read them. */
    private static Map<String, String> blockItems;
    private static boolean makingItems;
    /** How many values were registered when the mod's items were last all made: none to make while it is the same. */
    private static int madeAt = -1;

    /**
     * Item.byBlock's: a block's item, else air. The mod's BlockItems fill Item.BY_BLOCK when they are made, and the
     * game makes all of them when it registers them: so the mod's items are all made first (once; not while they are
     * being made). The game's blocks' items are given; another mod's block's isn't known: refused. Not the game's API.
     */
    public static net.minecraft.world.item.Item itemOf(net.minecraft.world.level.block.Block block) {
        synchronized (HorizonRegistries.class) {
            net.minecraft.world.item.Item item = net.minecraft.world.item.Item.BY_BLOCK.get(block);
            if (item != null) return item;
        }
        if (KEYS.get(block) == null) {
            String id = block.horizonId().toString();
            if (!block.horizonId().getNamespace().equals("minecraft"))
                throw new UnsupportedOperationException("the item of another mod's block " + id + " isn't known (horizon-port-neoforge)");
            String itemId;
            synchronized (HorizonRegistries.class) {
                if (blockItems == null) throw new IllegalStateException("the game's blocks' items weren't given (HorizonRegistries.known: block_items)");
                itemId = blockItems.get(id);
            }
            return item(itemId == null ? "minecraft:air" : itemId);
        }
        boolean makeAll;
        synchronized (HorizonRegistries.class) {
            makeAll = !makingItems && madeAt != SUPPLIERS.size();
            if (makeAll) {
                makingItems = true;
                madeAt = SUPPLIERS.size();
            }
        }
        if (makeAll) {
            try {
                java.util.List<ResourceKey<?>> keys;
                synchronized (HorizonRegistries.class) {
                    keys = new java.util.ArrayList<>(SUPPLIERS.keySet());
                }
                for (ResourceKey<?> key : keys) if (key.registry().equals(net.minecraft.core.registries.Registries.ITEM.location())) get(key);
            } finally {
                synchronized (HorizonRegistries.class) {
                    makingItems = false;
                }
            }
        }
        synchronized (HorizonRegistries.class) {
            net.minecraft.world.item.Item item = net.minecraft.world.item.Item.BY_BLOCK.get(block);
            if (item != null) return item;
        }
        return item("minecraft:air");
    }
}
