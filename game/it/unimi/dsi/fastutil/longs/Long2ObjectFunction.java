package it.unimi.dsi.fastutil.longs;

/** The stand-in for fastutil's Long2ObjectFunction: a function of longs with a default return value, boxed methods unboxing. */
@FunctionalInterface
public interface Long2ObjectFunction<V> extends it.unimi.dsi.fastutil.Function<Long, V>, java.util.function.LongFunction<V> {
    @Override
    default V apply(long key) {
        return get(key);
    }

    default V put(long key, V value) {
        throw new UnsupportedOperationException();
    }

    V get(long key);

    default V getOrDefault(long key, V defaultValue) {
        V v;
        return (v = get(key)) != defaultReturnValue() || containsKey(key) ? v : defaultValue;
    }

    default V remove(long key) {
        throw new UnsupportedOperationException();
    }

    @Override
    default V put(Long key, V value) {
        long k = key.longValue();
        boolean containsKey = containsKey(k);
        V v = put(k, value);
        return containsKey ? v : null;
    }

    @Override
    default V get(Object key) {
        if (key == null) return null;
        long k = ((Long) key).longValue();
        V v;
        return (v = get(k)) != defaultReturnValue() || containsKey(k) ? v : null;
    }

    @Override
    default V getOrDefault(Object key, V defaultValue) {
        if (key == null) return defaultValue;
        long k = ((Long) key).longValue();
        V v = get(k);
        return v != defaultReturnValue() || containsKey(k) ? v : defaultValue;
    }

    @Override
    default V remove(Object key) {
        if (key == null) return null;
        long k = ((Long) key).longValue();
        return containsKey(k) ? remove(k) : null;
    }

    default boolean containsKey(long key) {
        return true;
    }

    @Override
    default boolean containsKey(Object key) {
        if (key == null) return false;
        return containsKey(((Long) key).longValue());
    }

    default void defaultReturnValue(V rv) {
        throw new UnsupportedOperationException();
    }

    default V defaultReturnValue() {
        return null;
    }
}
