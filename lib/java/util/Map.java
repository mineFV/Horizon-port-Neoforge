package java.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The model of java.util.Map. Its defaults are those the API specification gives. The maps Map.of, Map.ofEntries and
 * Map.copyOf give iterate in the order of their entries as given; the JDK's order is salted at random, run to run.
 */
public interface Map<K, V> {
    int size();

    boolean isEmpty();

    boolean containsKey(Object key);

    boolean containsValue(Object value);

    V get(Object key);

    V put(K key, V value);

    V remove(Object key);

    void putAll(Map<? extends K, ? extends V> m);

    void clear();

    Set<K> keySet();

    Collection<V> values();

    Set<Map.Entry<K, V>> entrySet();

    interface Entry<K, V> {
        K getKey();

        V getValue();

        V setValue(V value);

        boolean equals(Object o);

        int hashCode();

        static <K extends Comparable<? super K>, V> Comparator<Map.Entry<K, V>> comparingByKey() {
            return (c1, c2) -> c1.getKey().compareTo(c2.getKey());
        }

        static <K, V extends Comparable<? super V>> Comparator<Map.Entry<K, V>> comparingByValue() {
            return (c1, c2) -> c1.getValue().compareTo(c2.getValue());
        }

        static <K, V> Comparator<Map.Entry<K, V>> comparingByKey(Comparator<? super K> cmp) {
            Objects.requireNonNull(cmp);
            return (c1, c2) -> cmp.compare(c1.getKey(), c2.getKey());
        }

        static <K, V> Comparator<Map.Entry<K, V>> comparingByValue(Comparator<? super V> cmp) {
            Objects.requireNonNull(cmp);
            return (c1, c2) -> cmp.compare(c1.getValue(), c2.getValue());
        }

        @SuppressWarnings("unchecked")
        static <K, V> Map.Entry<K, V> copyOf(Map.Entry<? extends K, ? extends V> e) {
            Objects.requireNonNull(e);
            if (e instanceof KeyValueHolder) return (Map.Entry<K, V>) e;
            return Map.entry(e.getKey(), e.getValue());
        }
    }

    boolean equals(Object o);

    int hashCode();

    default V getOrDefault(Object key, V defaultValue) {
        V v;
        return (((v = get(key)) != null) || containsKey(key)) ? v : defaultValue;
    }

    default void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        for (Map.Entry<K, V> entry : entrySet()) action.accept(entry.getKey(), entry.getValue());
    }

    default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        for (Map.Entry<K, V> entry : entrySet()) entry.setValue(function.apply(entry.getKey(), entry.getValue()));
    }

    default V putIfAbsent(K key, V value) {
        V v = get(key);
        if (v == null) v = put(key, value);
        return v;
    }

    default boolean remove(Object key, Object value) {
        Object curValue = get(key);
        if (!Objects.equals(curValue, value) || (curValue == null && !containsKey(key))) return false;
        remove(key);
        return true;
    }

    default boolean replace(K key, V oldValue, V newValue) {
        Object curValue = get(key);
        if (!Objects.equals(curValue, oldValue) || (curValue == null && !containsKey(key))) return false;
        put(key, newValue);
        return true;
    }

    default V replace(K key, V value) {
        V curValue;
        if (((curValue = get(key)) != null) || containsKey(key)) curValue = put(key, value);
        return curValue;
    }

    default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V v;
        if ((v = get(key)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                put(key, newValue);
                return newValue;
            }
        }
        return v;
    }

    default V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue;
        if ((oldValue = get(key)) != null) {
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue != null) {
                put(key, newValue);
                return newValue;
            }
            remove(key);
        }
        return null;
    }

    default V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue = get(key);
        V newValue = remappingFunction.apply(key, oldValue);
        if (newValue == null) {
            if (oldValue != null || containsKey(key)) remove(key);
            return null;
        }
        put(key, newValue);
        return newValue;
    }

    default V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        Objects.requireNonNull(value);
        V oldValue = get(key);
        V newValue = (oldValue == null) ? value : remappingFunction.apply(oldValue, value);
        if (newValue == null) remove(key);
        else put(key, newValue);
        return newValue;
    }

    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> of() {
        return (Map<K, V>) ImmutableCollections.EMPTY_MAP;
    }

    static <K, V> Map<K, V> of(K k1, V v1) {
        return new ImmutableCollections.Map1<>(k1, v1);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        return ImmutableCollections.mapOf(new Object[] {k1, v1, k2, v2});
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        return ImmutableCollections.mapOf(new Object[] {k1, v1, k2, v2, k3, v3});
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return ImmutableCollections.mapOf(new Object[] {k1, v1, k2, v2, k3, v3, k4, v4});
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return ImmutableCollections.mapOf(new Object[] {k1, v1, k2, v2, k3, v3, k4, v4, k5, v5});
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
        return ImmutableCollections.mapOf(new Object[] {k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6});
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
        return ImmutableCollections.mapOf(new Object[] {k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7});
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
        return ImmutableCollections.mapOf(new Object[] {k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8});
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
        return ImmutableCollections.mapOf(new Object[] {k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9});
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
        return ImmutableCollections.mapOf(new Object[] {k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10});
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> ofEntries(Map.Entry<? extends K, ? extends V>... entries) {
        if (entries.length == 0) return (Map<K, V>) ImmutableCollections.EMPTY_MAP;
        if (entries.length == 1) return new ImmutableCollections.Map1<>(entries[0].getKey(), entries[0].getValue());
        Object[] kv = new Object[entries.length << 1];
        for (int i = 0; i < entries.length; i++) {
            kv[i << 1] = entries[i].getKey();
            kv[(i << 1) + 1] = entries[i].getValue();
        }
        return ImmutableCollections.mapOf(kv);
    }

    static <K, V> Map.Entry<K, V> entry(K k, V v) {
        return new KeyValueHolder<>(k, v);
    }

    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> copyOf(Map<? extends K, ? extends V> map) {
        if (map instanceof ImmutableCollections.AbstractImmutableMap) return (Map<K, V>) map;
        int n = map.size();
        if (n == 0) return (Map<K, V>) ImmutableCollections.EMPTY_MAP;
        Object[] kv = new Object[n << 1];
        int i = 0;
        for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
            kv[i++] = e.getKey();
            kv[i++] = e.getValue();
        }
        if (n == 1) return new ImmutableCollections.Map1<>((K) kv[0], (V) kv[1]);
        return ImmutableCollections.mapOf(kv);
    }
}
