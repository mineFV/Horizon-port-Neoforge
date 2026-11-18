package it.unimi.dsi.fastutil.objects;

import java.util.Map;

/** The stand-in for fastutil's AbstractObject2ObjectMap: a map's text, equality and hash (observed, `standins`). */
public abstract class AbstractObject2ObjectMap<K, V> extends AbstractObject2ObjectFunction<K, V> implements Object2ObjectMap<K, V>, java.io.Serializable {
    protected AbstractObject2ObjectMap() {}

    @Override
    public boolean containsKey(Object k) {
        for (Entry<K, V> e : object2ObjectEntrySet()) if (java.util.Objects.equals(e.getKey(), k)) return true;
        return false;
    }

    @Override
    public boolean containsValue(Object v) {
        for (Entry<K, V> e : object2ObjectEntrySet()) if (java.util.Objects.equals(e.getValue(), v)) return true;
        return false;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) put(e.getKey(), e.getValue());
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (Entry<K, V> e : object2ObjectEntrySet()) h += e.hashCode();
        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Map<?, ?> m)) return false;
        if (m.size() != size()) return false;
        return object2ObjectEntrySet().containsAll(m.entrySet());
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("{");
        boolean first = true;
        for (Entry<K, V> e : object2ObjectEntrySet()) {
            if (first) first = false;
            else s.append(", ");
            s.append(this == e.getKey() ? "(this map)" : String.valueOf(e.getKey())).append("=>").append(this == e.getValue() ? "(this map)" : String.valueOf(e.getValue()));
        }
        return s.append('}').toString();
    }

    /** The stand-in for AbstractObject2ObjectMap.BasicEntry: a key and a value. */
    public static class BasicEntry<K, V> implements Entry<K, V> {
        protected K key;
        protected V value;

        public BasicEntry() {}

        public BasicEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Map.Entry<?, ?> e && java.util.Objects.equals(key, e.getKey()) && java.util.Objects.equals(value, e.getValue());
        }

        @Override
        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        @Override
        public String toString() {
            return key + "->" + value;
        }
    }
}
