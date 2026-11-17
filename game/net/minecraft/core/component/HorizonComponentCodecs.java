package net.minecraft.core.component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

/**
 * Not the game's: a stack's changed components to and from NBT, as a server writes and reads them (observed, `standins
 * --horizon-src`), for the types whose codec is a number's: max stack size, max damage, damage and repair cost. Any
 * other type's codec isn't modelled: writing it says so, and reading it can't be done.
 */
public final class HorizonComponentCodecs {
    private HorizonComponentCodecs() {}

    /** The range each number's type takes (its codec's), or null for a type whose codec isn't modelled. */
    private static int[] range(DataComponentType<?> type) {
        if (type == DataComponents.MAX_STACK_SIZE) return new int[] {1, 99};
        if (type == DataComponents.MAX_DAMAGE) return new int[] {1, Integer.MAX_VALUE};
        if (type == DataComponents.DAMAGE || type == DataComponents.REPAIR_COST) return new int[] {0, Integer.MAX_VALUE};
        return null;
    }

    private static String rangeMessage(int[] r, int value) {
        if (r[1] == Integer.MAX_VALUE) return (r[0] == 1 ? "Value must be positive: " : "Value must be non-negative: ") + value;
        return "Value must be within range [" + r[0] + ";" + r[1] + "]: " + value;
    }

    public static Tag encode(DataComponentPatch patch) {
        CompoundTag out = new CompoundTag();
        for (Map.Entry<DataComponentType<?>, Optional<?>> e : patch.entrySet()) {
            DataComponentType<?> type = e.getKey();
            if (type.isTransient()) continue;
            net.minecraft.resources.ResourceLocation id = DataComponentType.horizonId(type);
            if (id == null) throw new IllegalStateException("Unregistered component: " + type);
            if (e.getValue().isEmpty()) {
                out.put("!" + id, new CompoundTag());
                continue;
            }
            int[] r = range(type);
            if (r == null) throw new IllegalStateException("the codec of " + id + " isn't modelled (horizon-port-neoforge)");
            int value = (Integer) e.getValue().get();
            if (value < r[0] || value > r[1]) throw new IllegalStateException(rangeMessage(r, value));
            out.put(id.toString(), IntTag.valueOf(value));
        }
        return out;
    }

    /**
     * The patch a compound says. Observed: what the game can't read of it is left out (an unknown type, a value out of
     * its range or of another kind, a compound that isn't one), the rest is taken, unchecked.
     */
    public static DataComponentPatch decode(Tag tag) {
        if (!(tag instanceof CompoundTag c)) return DataComponentPatch.EMPTY;
        Map<DataComponentType<?>, Optional<?>> map = new LinkedHashMap<>();
        for (String key : c.getAllKeys()) {
            boolean removed = key.startsWith("!");
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(removed ? key.substring(1) : key);
            if (id == null) continue;
            DataComponentType<?> type = byId(id);
            if (type == null) continue;
            if (removed) {
                map.put(type, Optional.empty());
                continue;
            }
            int[] r = range(type);
            if (r == null) throw new IllegalStateException("the codec of " + id + " isn't modelled (horizon-port-neoforge)");
            if (!(c.get(key) instanceof NumericTag n)) continue;
            int value = n.getAsInt();
            if (value < r[0] || value > r[1]) continue;
            map.put(type, Optional.of(value));
        }
        return map.isEmpty() ? DataComponentPatch.EMPTY : new DataComponentPatch(map);
    }

    /** The component type of an id: the game's modelled one, the mod's own; null when the game has none; the game's others say they aren't modelled. */
    public static DataComponentType<?> type(net.minecraft.resources.ResourceLocation id) {
        return byId(id);
    }

    private static DataComponentType<?> byId(net.minecraft.resources.ResourceLocation id) {
        DataComponentType<?>[] game = {DataComponents.MAX_STACK_SIZE, DataComponents.MAX_DAMAGE, DataComponents.DAMAGE, DataComponents.LORE,
            DataComponents.ENCHANTMENTS, DataComponents.REPAIR_COST, DataComponents.ATTRIBUTE_MODIFIERS, DataComponents.RARITY};
        for (DataComponentType<?> t : game) if (id.equals(DataComponentType.horizonId(t))) return t;
        Object own = net.neoforged.neoforge.registries.HorizonRegistries.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, id));
        if (own instanceof DataComponentType<?> t) return t;
        if (net.neoforged.neoforge.registries.HorizonRegistries.knownComponent(id.toString())) throw new IllegalStateException("the game's component " + id + " isn't modelled (horizon-port-neoforge)");
        return null;
    }
}
