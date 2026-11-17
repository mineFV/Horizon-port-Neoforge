package net.minecraft.core.component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The stand-in for the game's DataComponentPatch: what a stack changed of its item's components, a value set or a
 * type taken out (an empty Optional), in the order they were changed. Its text is observed.
 */
public final class DataComponentPatch {
    public static final DataComponentPatch EMPTY = new DataComponentPatch(new LinkedHashMap<>());

    final Map<DataComponentType<?>, Optional<?>> map;

    DataComponentPatch(Map<DataComponentType<?>, Optional<?>> map) {
        this.map = map;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<? extends T> get(DataComponentType<? extends T> type) {
        return (Optional<? extends T>) map.get(type);
    }

    public Set<Map.Entry<DataComponentType<?>, Optional<?>>> entrySet() {
        return java.util.Collections.unmodifiableSet(map.entrySet());
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof DataComponentPatch p && map.equals(p.map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    /** Observed: {type=>value, !type, ...}. */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<DataComponentType<?>, Optional<?>> e : map.entrySet()) {
            if (!first) b.append(", ");
            first = false;
            if (e.getValue().isPresent()) b.append(e.getKey()).append("=>").append(e.getValue().get());
            else b.append('!').append(e.getKey());
        }
        return b.append('}').toString();
    }
}
