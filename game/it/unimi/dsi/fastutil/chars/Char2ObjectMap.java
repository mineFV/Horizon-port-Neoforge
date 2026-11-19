package it.unimi.dsi.fastutil.chars;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectSet;

/** The stand-in for fastutil's Char2ObjectMap: a map of chars with a default return value, its entries and views. */
public interface Char2ObjectMap<V> extends Char2ObjectFunction<V>, java.util.Map<Character, V> {
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

    ObjectSet<Entry<V>> char2ObjectEntrySet();

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    default ObjectSet<java.util.Map.Entry<Character, V>> entrySet() {
        return (ObjectSet) char2ObjectEntrySet();
    }

    @Override
    default V put(Character key, V value) {
        return Char2ObjectFunction.super.put(key, value);
    }

    @Override
    default V get(Object key) {
        return Char2ObjectFunction.super.get(key);
    }

    @Override
    default V remove(Object key) {
        return Char2ObjectFunction.super.remove(key);
    }

    @Override
    CharSet keySet();

    @Override
    ObjectCollection<V> values();

    @Override
    boolean containsKey(char key);

    @Override
    default boolean containsKey(Object key) {
        return Char2ObjectFunction.super.containsKey(key);
    }

    @Override
    default V getOrDefault(Object key, V defaultValue) {
        return Char2ObjectFunction.super.getOrDefault(key, defaultValue);
    }

    @Override
    default V getOrDefault(char key, V defaultValue) {
        V v;
        return (v = get(key)) != defaultReturnValue() || containsKey(key) ? v : defaultValue;
    }

    /** The stand-in for Char2ObjectMap.FastEntrySet: entries, whose fast iterator here gives new ones as the other does. */
    interface FastEntrySet<V> extends ObjectSet<Entry<V>> {
        it.unimi.dsi.fastutil.objects.ObjectIterator<Entry<V>> fastIterator();
    }

    /** The stand-in for Char2ObjectMap.Entry: an entry of a char key. */
    interface Entry<V> extends java.util.Map.Entry<Character, V> {
        char getCharKey();

        @Override
        default Character getKey() {
            return getCharKey();
        }
    }
}
