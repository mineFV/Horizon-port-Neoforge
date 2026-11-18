package it.unimi.dsi.fastutil;

/** The stand-in for fastutil's Function: a map as a function (what it doesn't hold, its default return value). */
@FunctionalInterface
public interface Function<K, V> extends java.util.function.Function<K, V> {
    @Override
    default V apply(K key) {
        return get(key);
    }

    default V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    V get(Object key);

    default V getOrDefault(Object key, V defaultValue) {
        V v = get(key);
        return v != null || containsKey(key) ? v : defaultValue;
    }

    default boolean containsKey(Object key) {
        return true;
    }

    default V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    default int size() {
        return -1;
    }

    default void clear() {
        throw new UnsupportedOperationException();
    }
}
