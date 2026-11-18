package it.unimi.dsi.fastutil.objects;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/** The stand-in for fastutil's Object2ObjectMap: a map with a default return value, its entries and views. */
public interface Object2ObjectMap<K, V> extends Object2ObjectFunction<K, V>, Map<K, V> {
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

    ObjectSet<Entry<K, V>> object2ObjectEntrySet();

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    default ObjectSet<Map.Entry<K, V>> entrySet() {
        return (ObjectSet) object2ObjectEntrySet();
    }

    @Override
    default V put(K key, V value) {
        return Object2ObjectFunction.super.put(key, value);
    }

    @Override
    default V remove(Object key) {
        return Object2ObjectFunction.super.remove(key);
    }

    @Override
    ObjectSet<K> keySet();

    @Override
    ObjectCollection<V> values();

    @Override
    boolean containsKey(Object key);

    @Override
    default void forEach(BiConsumer<? super K, ? super V> consumer) {
        ObjectSet<Entry<K, V>> entrySet = object2ObjectEntrySet();
        for (Entry<K, V> e : entrySet) consumer.accept(e.getKey(), e.getValue());
    }

    @Override
    default V getOrDefault(Object key, V defaultValue) {
        V v;
        return (v = get(key)) != defaultReturnValue() || containsKey(key) ? v : defaultValue;
    }

    @Override
    default V putIfAbsent(K key, V value) {
        V v = get(key), drv = defaultReturnValue();
        if (v != drv || containsKey(key)) return v;
        put(key, value);
        return drv;
    }

    @Override
    default boolean remove(Object key, Object value) {
        V curValue = get(key);
        if (!Objects.equals(curValue, value) || curValue == defaultReturnValue() && !containsKey(key)) return false;
        remove(key);
        return true;
    }

    @Override
    default boolean replace(K key, V oldValue, V newValue) {
        V curValue = get(key);
        if (!Objects.equals(curValue, oldValue) || curValue == defaultReturnValue() && !containsKey(key)) return false;
        put(key, newValue);
        return true;
    }

    @Override
    default V replace(K key, V value) {
        return containsKey(key) ? put(key, value) : defaultReturnValue();
    }

    default V computeIfAbsent(K key, Object2ObjectFunction<? super K, ? extends V> mapping) {
        Objects.requireNonNull(mapping);
        V v = get(key), drv = defaultReturnValue();
        if (v != drv || containsKey(key)) return v;
        if (!mapping.containsKey(key)) return drv;
        V newValue = mapping.get(key);
        put(key, newValue);
        return newValue;
    }

    @Override
    default V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remapping) {
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

    @Override
    default V compute(K key, BiFunction<? super K, ? super V, ? extends V> remapping) {
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

    @Override
    default V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remapping) {
        Objects.requireNonNull(remapping);
        Objects.requireNonNull(value);
        V oldValue = get(key), drv = defaultReturnValue(), newValue;
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

    /** The stand-in for Object2ObjectMap.Entry. */
    interface Entry<K, V> extends Map.Entry<K, V> {
    }

    /** The stand-in for Object2ObjectMap.FastEntrySet: entries gone through with one entry object. */
    interface FastEntrySet<K, V> extends ObjectSet<Entry<K, V>> {
        ObjectIterator<Entry<K, V>> fastIterator();

        default void fastForEach(java.util.function.Consumer<? super Entry<K, V>> consumer) {
            forEach(consumer);
        }
    }
}
