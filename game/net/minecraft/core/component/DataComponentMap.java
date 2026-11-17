package net.minecraft.core.component;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The stand-in for the game's DataComponentMap: types and their values, in the order they were set (the game's array
 * map, for the few an item has). Its texts are observed (`standins --horizon-src`).
 */
public interface DataComponentMap extends Iterable<TypedDataComponent<?>> {
    DataComponentMap EMPTY = new DataComponentMap() {
        @Override
        public <T> T get(DataComponentType<? extends T> type) {
            return null;
        }

        @Override
        public Set<DataComponentType<?>> keySet() {
            return Set.of();
        }

        @Override
        public Iterator<TypedDataComponent<?>> iterator() {
            return java.util.Collections.emptyIterator();
        }
    };

    static Builder builder() {
        return new Builder();
    }

    <T> T get(DataComponentType<? extends T> type);

    Set<DataComponentType<?>> keySet();

    default boolean has(DataComponentType<?> type) {
        return get(type) != null;
    }

    default <T> T getOrDefault(DataComponentType<? extends T> type, T fallback) {
        T value = get(type);
        return value != null ? value : fallback;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    default Iterator<TypedDataComponent<?>> iterator() {
        java.util.List<TypedDataComponent<?>> all = new java.util.ArrayList<>();
        for (DataComponentType<?> type : keySet()) all.add(new TypedDataComponent(type, get(type)));
        return all.iterator();
    }

    default int size() {
        return keySet().size();
    }

    default boolean isEmpty() {
        return size() == 0;
    }

    class Builder implements net.neoforged.neoforge.common.extensions.IDataComponentMapBuilderExtensions {
        private final Map<DataComponentType<?>, Object> map = new LinkedHashMap<>();

        Builder() {}

        /** A null value takes the type out. */
        public <T> Builder set(DataComponentType<T> type, T value) {
            if (value != null) map.put(type, value);
            else map.remove(type);
            return this;
        }

        public Builder addAll(DataComponentMap other) {
            for (DataComponentType<?> type : other.keySet()) map.put(type, other.get(type));
            return this;
        }

        public DataComponentMap build() {
            return map.isEmpty() ? EMPTY : new SimpleMap(new LinkedHashMap<>(map));
        }

        record SimpleMap(Map<DataComponentType<?>, Object> map) implements DataComponentMap {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(DataComponentType<? extends T> type) {
                return (T) map.get(type);
            }

            @Override
            public Set<DataComponentType<?>> keySet() {
                return new HorizonKeys(map.keySet());
            }

            /** Observed: {type=>value, ...}. */
            @Override
            public String toString() {
                return HorizonKeys.text(this);
            }
        }
    }
}
