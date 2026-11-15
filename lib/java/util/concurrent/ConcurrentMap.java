package java.util.concurrent;

import java.util.Map;

/** The model of java.util.concurrent.ConcurrentMap: Map's, whose defaults ConcurrentHashMap overrides. */
public interface ConcurrentMap<K, V> extends Map<K, V> {
    V putIfAbsent(K key, V value);

    boolean remove(Object key, Object value);

    boolean replace(K key, V oldValue, V newValue);

    V replace(K key, V value);
}
