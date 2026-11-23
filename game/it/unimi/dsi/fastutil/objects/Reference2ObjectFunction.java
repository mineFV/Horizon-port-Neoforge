package it.unimi.dsi.fastutil.objects;

/** The stand-in for fastutil's Reference2ObjectFunction: a function with a default return value. */
@FunctionalInterface
public interface Reference2ObjectFunction<K, V> extends it.unimi.dsi.fastutil.Function<K, V> {
    @Override
    default V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    V get(Object key);

    @Override
    default V getOrDefault(Object key, V defaultValue) {
        V v;
        return (v = get(key)) != defaultReturnValue() || containsKey(key) ? v : defaultValue;
    }

    @Override
    default V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    default void defaultReturnValue(V rv) {
        throw new UnsupportedOperationException();
    }

    default V defaultReturnValue() {
        return null;
    }
}
