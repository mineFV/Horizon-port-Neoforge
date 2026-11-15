package java.util;

/** The model of the entry Map.entry gives: neither key nor value is null, and it can't be changed. */
final class KeyValueHolder<K, V> implements Map.Entry<K, V> {
    final K key;
    final V value;

    KeyValueHolder(K k, V v) {
        key = Objects.requireNonNull(k);
        value = Objects.requireNonNull(v);
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public V setValue(V value) {
        throw new UnsupportedOperationException("not supported");
    }

    public boolean equals(Object o) {
        return o instanceof Map.Entry<?, ?> e && key.equals(e.getKey()) && value.equals(e.getValue());
    }

    public int hashCode() {
        return key.hashCode() ^ value.hashCode();
    }

    public String toString() {
        return String.valueOf(key).concat("=").concat(String.valueOf(value));
    }
}
