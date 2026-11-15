package java.util;

/** The model of java.util.AbstractMap: everything from entrySet(), as the API specifies. */
public abstract class AbstractMap<K, V> implements Map<K, V> {
    transient Set<K> keySet;
    transient Collection<V> values;

    protected AbstractMap() {}

    public int size() {
        return entrySet().size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean containsValue(Object value) {
        Iterator<Map.Entry<K, V>> i = entrySet().iterator();
        if (value == null) {
            while (i.hasNext()) if (i.next().getValue() == null) return true;
        } else {
            while (i.hasNext()) if (value.equals(i.next().getValue())) return true;
        }
        return false;
    }

    public boolean containsKey(Object key) {
        Iterator<Map.Entry<K, V>> i = entrySet().iterator();
        if (key == null) {
            while (i.hasNext()) if (i.next().getKey() == null) return true;
        } else {
            while (i.hasNext()) if (key.equals(i.next().getKey())) return true;
        }
        return false;
    }

    public V get(Object key) {
        Iterator<Map.Entry<K, V>> i = entrySet().iterator();
        if (key == null) {
            while (i.hasNext()) {
                Map.Entry<K, V> e = i.next();
                if (e.getKey() == null) return e.getValue();
            }
        } else {
            while (i.hasNext()) {
                Map.Entry<K, V> e = i.next();
                if (key.equals(e.getKey())) return e.getValue();
            }
        }
        return null;
    }

    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    public V remove(Object key) {
        Iterator<Map.Entry<K, V>> i = entrySet().iterator();
        Map.Entry<K, V> correctEntry = null;
        if (key == null) {
            while (correctEntry == null && i.hasNext()) {
                Map.Entry<K, V> e = i.next();
                if (e.getKey() == null) correctEntry = e;
            }
        } else {
            while (correctEntry == null && i.hasNext()) {
                Map.Entry<K, V> e = i.next();
                if (key.equals(e.getKey())) correctEntry = e;
            }
        }
        V oldValue = null;
        if (correctEntry != null) {
            oldValue = correctEntry.getValue();
            i.remove();
        }
        return oldValue;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) put(e.getKey(), e.getValue());
    }

    public void clear() {
        entrySet().clear();
    }

    public Set<K> keySet() {
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new AbstractSet<K>() {
                public Iterator<K> iterator() {
                    return new Iterator<K>() {
                        private final Iterator<Map.Entry<K, V>> i = entrySet().iterator();

                        public boolean hasNext() {
                            return i.hasNext();
                        }

                        public K next() {
                            return i.next().getKey();
                        }

                        public void remove() {
                            i.remove();
                        }
                    };
                }

                public int size() {
                    return AbstractMap.this.size();
                }

                public boolean isEmpty() {
                    return AbstractMap.this.isEmpty();
                }

                public void clear() {
                    AbstractMap.this.clear();
                }

                public boolean contains(Object k) {
                    return AbstractMap.this.containsKey(k);
                }
            };
            keySet = ks;
        }
        return ks;
    }

    public Collection<V> values() {
        Collection<V> vals = values;
        if (vals == null) {
            vals = new AbstractCollection<V>() {
                public Iterator<V> iterator() {
                    return new Iterator<V>() {
                        private final Iterator<Map.Entry<K, V>> i = entrySet().iterator();

                        public boolean hasNext() {
                            return i.hasNext();
                        }

                        public V next() {
                            return i.next().getValue();
                        }

                        public void remove() {
                            i.remove();
                        }
                    };
                }

                public int size() {
                    return AbstractMap.this.size();
                }

                public boolean isEmpty() {
                    return AbstractMap.this.isEmpty();
                }

                public void clear() {
                    AbstractMap.this.clear();
                }

                public boolean contains(Object v) {
                    return AbstractMap.this.containsValue(v);
                }
            };
            values = vals;
        }
        return vals;
    }

    public abstract Set<Map.Entry<K, V>> entrySet();

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Map<?, ?> m)) return false;
        if (m.size() != size()) return false;
        try {
            for (Map.Entry<K, V> e : entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                if (value == null) {
                    if (!(m.get(key) == null && m.containsKey(key))) return false;
                } else {
                    if (!value.equals(m.get(key))) return false;
                }
            }
        } catch (ClassCastException | NullPointerException unused) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int h = 0;
        for (Map.Entry<K, V> entry : entrySet()) h += entry.hashCode();
        return h;
    }

    public String toString() {
        Iterator<Map.Entry<K, V>> i = entrySet().iterator();
        if (!i.hasNext()) return "{}";
        String s = "{";
        for (;;) {
            Map.Entry<K, V> e = i.next();
            K key = e.getKey();
            V value = e.getValue();
            s = s.concat(key == this ? "(this Map)" : String.valueOf(key)).concat("=").concat(value == this ? "(this Map)" : String.valueOf(value));
            if (!i.hasNext()) return s.concat("}");
            s = s.concat(", ");
        }
    }

    /** The model of AbstractMap.SimpleEntry: a key and a value that can be changed. */
    public static class SimpleEntry<K, V> implements Map.Entry<K, V>, java.io.Serializable {
        private final K key;
        private V value;

        public SimpleEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public SimpleEntry(Map.Entry<? extends K, ? extends V> entry) {
            this.key = entry.getKey();
            this.value = entry.getValue();
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public boolean equals(Object o) {
            return o instanceof Map.Entry<?, ?> e && Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue());
        }

        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        public String toString() {
            return String.valueOf(key).concat("=").concat(String.valueOf(value));
        }
    }

    /** The model of AbstractMap.SimpleImmutableEntry: setValue throws. */
    public static class SimpleImmutableEntry<K, V> implements Map.Entry<K, V>, java.io.Serializable {
        private final K key;
        private final V value;

        public SimpleImmutableEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public SimpleImmutableEntry(Map.Entry<? extends K, ? extends V> entry) {
            this.key = entry.getKey();
            this.value = entry.getValue();
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        public boolean equals(Object o) {
            return o instanceof Map.Entry<?, ?> e && Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue());
        }

        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        public String toString() {
            return String.valueOf(key).concat("=").concat(String.valueOf(value));
        }
    }
}
