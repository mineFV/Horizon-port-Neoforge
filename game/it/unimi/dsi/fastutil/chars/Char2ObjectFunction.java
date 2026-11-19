package it.unimi.dsi.fastutil.chars;

/** The stand-in for fastutil's Char2ObjectFunction: a function of chars with a default return value, boxed methods unboxing (observed). */
@FunctionalInterface
public interface Char2ObjectFunction<V> extends it.unimi.dsi.fastutil.Function<Character, V>, java.util.function.IntFunction<V> {
    V get(char key);

    @Override
    default V apply(int key) {
        return get(HorizonChars.safeIntToChar(key));
    }

    default V put(char key, V value) {
        throw new UnsupportedOperationException();
    }

    default V getOrDefault(char key, V defaultValue) {
        V v;
        return (v = get(key)) != defaultReturnValue() || containsKey(key) ? v : defaultValue;
    }

    default V remove(char key) {
        throw new UnsupportedOperationException();
    }

    default boolean containsKey(char key) {
        return true;
    }

    default void defaultReturnValue(V rv) {
        throw new UnsupportedOperationException();
    }

    default V defaultReturnValue() {
        return null;
    }

    @Override
    default V put(Character key, V value) {
        char k = key.charValue();
        boolean containsKey = containsKey(k);
        V v = put(k, value);
        return containsKey ? v : null;
    }

    @Override
    default V get(Object key) {
        if (key == null) return null;
        char k = ((Character) key).charValue();
        V v;
        return (v = get(k)) != defaultReturnValue() || containsKey(k) ? v : null;
    }

    @Override
    default V getOrDefault(Object key, V defaultValue) {
        if (key == null) return defaultValue;
        char k = ((Character) key).charValue();
        V v = get(k);
        return v != defaultReturnValue() || containsKey(k) ? v : defaultValue;
    }

    @Override
    default V remove(Object key) {
        if (key == null) return null;
        char k = ((Character) key).charValue();
        return containsKey(k) ? remove(k) : null;
    }

    @Override
    default boolean containsKey(Object key) {
        if (key == null) return false;
        return containsKey(((Character) key).charValue());
    }
}
