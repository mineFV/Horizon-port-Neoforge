package net.minecraft.core.component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The stand-in for the game's PatchedDataComponentMap: a prototype (an item's components) and what was changed of it.
 * Setting a type back to its prototype's value takes the change out (observed, `standins --horizon-src`).
 */
public final class PatchedDataComponentMap implements DataComponentMap {
    private final DataComponentMap prototype;
    private Map<DataComponentType<?>, Optional<?>> patch;

    public PatchedDataComponentMap(DataComponentMap prototype) {
        this(prototype, new LinkedHashMap<>());
    }

    private PatchedDataComponentMap(DataComponentMap prototype, Map<DataComponentType<?>, Optional<?>> patch) {
        this.prototype = prototype;
        this.patch = patch;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(DataComponentType<? extends T> type) {
        Optional<?> changed = patch.get(type);
        return changed != null ? (T) changed.orElse(null) : prototype.get(type);
    }

    /**
     * Observed: what it gives back is the change it replaces (the value it set, or the prototype's when it had taken the
     * type out), else the prototype's value.
     */
    @SuppressWarnings("unchecked")
    public <T> T set(DataComponentType<? super T> type, T value) {
        Object original = prototype.get(type);
        Optional<?> replaced = Objects.equals(original, value) ? patch.remove(type) : patch.put(type, Optional.ofNullable(value));
        return (T) (replaced != null ? replaced.map(v -> (Object) v).orElse(original) : original);
    }

    /** As set: the change it replaces, else the prototype's value (observed). */
    @SuppressWarnings("unchecked")
    public <T> T remove(DataComponentType<? extends T> type) {
        Object original = prototype.get(type);
        Optional<?> replaced = original != null ? patch.put(type, Optional.empty()) : patch.remove(type);
        return (T) (replaced != null ? replaced.map(v -> (Object) v).orElse(original) : original);
    }

    public void applyPatch(DataComponentPatch changes) {
        for (Map.Entry<DataComponentType<?>, Optional<?>> e : changes.map.entrySet()) {
            DataComponentType<?> type = e.getKey();
            Object value = e.getValue().orElse(null);
            if (value == null) remove(type);
            else set(cast(type), value);
        }
    }

    @SuppressWarnings("unchecked")
    private static DataComponentType<Object> cast(DataComponentType<?> type) {
        return (DataComponentType<Object>) type;
    }

    @Override
    public Set<DataComponentType<?>> keySet() {
        Set<DataComponentType<?>> keys = new HorizonKeys(prototype.keySet());
        for (Map.Entry<DataComponentType<?>, Optional<?>> e : patch.entrySet()) {
            if (e.getValue().isPresent()) keys.add(e.getKey());
            else keys.remove(e.getKey());
        }
        return keys;
    }

    public DataComponentPatch asPatch() {
        return patch.isEmpty() ? DataComponentPatch.EMPTY : new DataComponentPatch(new LinkedHashMap<>(patch));
    }

    public PatchedDataComponentMap copy() {
        return new PatchedDataComponentMap(prototype, new LinkedHashMap<>(patch));
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof PatchedDataComponentMap m && prototype.equals(m.prototype) && patch.equals(m.patch);
    }

    @Override
    public int hashCode() {
        return prototype.hashCode() + patch.hashCode() * 31;
    }

    /** Observed: {type=>value, ...}, the prototype's types in their order (those changed in place), then those added. */
    @Override
    public String toString() {
        return HorizonKeys.text(this);
    }
}
