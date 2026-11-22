package it.unimi.dsi.fastutil.longs;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Objects;

/** The stand-in for fastutil's Long2ObjectMap: a map of longs with a default return value, its entries and views. */
public interface Long2ObjectMap<V> extends Long2ObjectFunction<V>, java.util.Map<Long, V> {
    @Override
    int size();

    @Override
    default void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    void defaultReturnValue(V rv);

    @Override
    V defaultReturnValue();

    ObjectSet<Entry<V>> long2ObjectEntrySet();

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    default ObjectSet<java.util.Map.Entry<Long, V>> entrySet() {
        return (ObjectSet) long2ObjectEntrySet();
    }

    @Override
    default V put(Long key, V value) {
        return Long2ObjectFunction.super.put(key, value);
    }

    @Override
    default V get(Object key) {
        return Long2ObjectFunction.super.get(key);
    }

    @Override
    default V remove(Object key) {
        return Long2ObjectFunction.super.remove(key);
    }

    @Override
    LongSet keySet();

    @Override
    ObjectCollection<V> values();

    @Override
    boolean containsKey(long key);

    @Override
    default boolean containsKey(Object key) {
        return Long2ObjectFunction.super.containsKey(key);
    }

    @Override
    default void forEach(java.util.function.BiConsumer<? super Long, ? super V> consumer) {
        for (Entry<V> e : long2ObjectEntrySet()) consumer.accept(e.getLongKey(), e.getValue());
    }

    @Override
    default V getOrDefault(long key, V defaultValue) {
        V v;
        return (v = get(key)) != defaultReturnValue() || containsKey(key) ? v : defaultValue;
    }

    @Override
    default V getOrDefault(Object key, V defaultValue) {
        return Long2ObjectFunction.super.getOrDefault(key, defaultValue);
    }

    default V putIfAbsent(long key, V value) {
        V v = get(key), drv = defaultReturnValue();
        if (v != drv || containsKey(key)) return v;
        put(key, value);
        return drv;
    }

    default boolean remove(long key, Object value) {
        V curValue = get(key);
        if (!Objects.equals(curValue, value) || curValue == defaultReturnValue() && !containsKey(key)) return false;
        remove(key);
        return true;
    }

    default boolean replace(long key, V oldValue, V newValue) {
        V curValue = get(key);
        if (!Objects.equals(curValue, oldValue) || curValue == defaultReturnValue() && !containsKey(key)) return false;
        put(key, newValue);
        return true;
    }

    default V replace(long key, V value) {
        return containsKey(key) ? put(key, value) : defaultReturnValue();
    }

    default V computeIfAbsent(long key, java.util.function.LongFunction<? extends V> mapping) {
        Objects.requireNonNull(mapping);
        V v = get(key), drv = defaultReturnValue();
        if (v != drv || containsKey(key)) return v;
        V newValue = mapping.apply(key);
        put(key, newValue);
        return newValue;
    }

    default V computeIfAbsent(long key, Long2ObjectFunction<? extends V> mapping) {
        Objects.requireNonNull(mapping);
        V v = get(key), drv = defaultReturnValue();
        if (v != drv || containsKey(key)) return v;
        if (!mapping.containsKey(key)) return drv;
        V newValue = mapping.get(key);
        put(key, newValue);
        return newValue;
    }

    default V computeIfPresent(long key, java.util.function.BiFunction<? super Long, ? super V, ? extends V> remapping) {
        Objects.requireNonNull(remapping);
        V oldValue = get(key), drv = defaultReturnValue();
        if (oldValue == drv && !containsKey(key)) return drv;
        V newValue = remapping.apply(key, oldValue);
        if (newValue == null) {
            remove(key);
            return drv;
        }
        put(key, newValue);
        return newValue;
    }

    default V compute(long key, java.util.function.BiFunction<? super Long, ? super V, ? extends V> remapping) {
        Objects.requireNonNull(remapping);
        V oldValue = get(key), drv = defaultReturnValue();
        boolean contained = oldValue != drv || containsKey(key);
        V newValue = remapping.apply(key, contained ? oldValue : null);
        if (newValue == null) {
            if (contained) remove(key);
            return drv;
        }
        put(key, newValue);
        return newValue;
    }

    default V merge(long key, V value, java.util.function.BiFunction<? super V, ? super V, ? extends V> remapping) {
        Objects.requireNonNull(remapping);
        Objects.requireNonNull(value);
        V oldValue = get(key), drv = defaultReturnValue();
        V newValue;
        if (oldValue != drv || containsKey(key)) {
            V merged = remapping.apply(oldValue, value);
            if (merged == null) {
                remove(key);
                return drv;
            }
            newValue = merged;
        } else {
            newValue = value;
        }
        put(key, newValue);
        return newValue;
    }

    /** The stand-in for Long2ObjectMap.Entry: an entry of a long key. */
    interface Entry<V> extends java.util.Map.Entry<Long, V> {
        long getLongKey();

        @Override
        default Long getKey() {
            return getLongKey();
        }
    }

    /** The stand-in for Long2ObjectMap.FastEntrySet: entries, and a fast iterator that may give one entry object again. */
    interface FastEntrySet<V> extends ObjectSet<Entry<V>> {
        it.unimi.dsi.fastutil.objects.ObjectIterator<Entry<V>> fastIterator();

        default void fastForEach(java.util.function.Consumer<? super Entry<V>> consumer) {
            forEach(consumer);
        }
    }
}
