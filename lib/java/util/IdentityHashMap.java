package java.util;

/**
 * The model of java.util.IdentityHashMap: keys (and values, for containsValue and equality) compared by ==, hashed
 * by System.identityHashCode. It iterates in the order of a HashMap of those hashes: the JDK's order comes from the
 * identity hashes too, which change from run to run, so no program can count on it. Its iterators are fail-fast.
 */
public class IdentityHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, java.io.Serializable, Cloneable {
    /** A key compared by identity. */
    private static final class IdKey {
        final Object key;

        IdKey(Object key) {
            this.key = key;
        }

        public boolean equals(Object o) {
            return o instanceof IdKey k && k.key == key;
        }

        public int hashCode() {
            return System.identityHashCode(key);
        }
    }

    private final HashMap<IdKey, V> map;

    public IdentityHashMap() {
        map = new HashMap<>();
    }

    public IdentityHashMap(int expectedMaxSize) {
        if (expectedMaxSize < 0) throw new IllegalArgumentException("expectedMaxSize is negative: ".concat(Integer.toString(expectedMaxSize)));
        map = new HashMap<>();
    }

    public IdentityHashMap(Map<? extends K, ? extends V> m) {
        this();
        putAll(m);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public V get(Object key) {
        return map.get(new IdKey(key));
    }

    public boolean containsKey(Object key) {
        return map.containsKey(new IdKey(key));
    }

    public boolean containsValue(Object value) {
        for (V v : map.values()) if (v == value) return true;
        return false;
    }

    public V put(K key, V value) {
        return map.put(new IdKey(key), value);
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) put(e.getKey(), e.getValue());
    }

    public V remove(Object key) {
        return map.remove(new IdKey(key));
    }

    public void clear() {
        map.clear();
    }

    /** By identity: the same keys mapped to the same values. */
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof IdentityHashMap<?, ?> m) {
            if (m.size() != size()) return false;
            for (Map.Entry<IdKey, V> e : map.entrySet()) {
                IdKey k = e.getKey();
                if (!m.map.containsKey(k) || m.map.get(k) != e.getValue()) return false;
            }
            return true;
        }
        return super.equals(o);
    }

    public int hashCode() {
        int result = 0;
        for (Map.Entry<IdKey, V> e : map.entrySet()) result += System.identityHashCode(e.getKey().key) ^ System.identityHashCode(e.getValue());
        return result;
    }

    /** An entry: equal to another with the same key and value by identity. */
    private final class IdEntry implements Map.Entry<K, V> {
        private final Map.Entry<IdKey, V> inner;

        IdEntry(Map.Entry<IdKey, V> inner) {
            this.inner = inner;
        }

        @SuppressWarnings("unchecked")
        public K getKey() {
            return (K) inner.getKey().key;
        }

        public V getValue() {
            return inner.getValue();
        }

        public V setValue(V value) {
            return inner.setValue(value);
        }

        public boolean equals(Object o) {
            return o instanceof Map.Entry<?, ?> e && e.getKey() == getKey() && e.getValue() == getValue();
        }

        public int hashCode() {
            return System.identityHashCode(getKey()) ^ System.identityHashCode(getValue());
        }

        public String toString() {
            return String.valueOf(getKey()).concat("=").concat(String.valueOf(getValue()));
        }
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>() {
            public Iterator<Map.Entry<K, V>> iterator() {
                Iterator<Map.Entry<IdKey, V>> it = map.entrySet().iterator();
                return new Iterator<Map.Entry<K, V>>() {
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    public Map.Entry<K, V> next() {
                        return new IdEntry(it.next());
                    }

                    public void remove() {
                        it.remove();
                    }
                };
            }

            public int size() {
                return map.size();
            }

            public boolean contains(Object o) {
                return o instanceof Map.Entry<?, ?> e && containsKey(e.getKey()) && get(e.getKey()) == e.getValue();
            }

            public void clear() {
                map.clear();
            }
        };
    }

    public Set<K> keySet() {
        return new AbstractSet<K>() {
            public Iterator<K> iterator() {
                Iterator<IdKey> it = map.keySet().iterator();
                return new Iterator<K>() {
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @SuppressWarnings("unchecked")
                    public K next() {
                        return (K) it.next().key;
                    }

                    public void remove() {
                        it.remove();
                    }
                };
            }

            public int size() {
                return map.size();
            }

            public boolean contains(Object o) {
                return containsKey(o);
            }

            public boolean remove(Object o) {
                if (!containsKey(o)) return false;
                IdentityHashMap.this.remove(o);
                return true;
            }

            public void clear() {
                map.clear();
            }
        };
    }

    public Collection<V> values() {
        return new AbstractCollection<V>() {
            public Iterator<V> iterator() {
                return map.values().iterator();
            }

            public int size() {
                return map.size();
            }

            public boolean contains(Object o) {
                return containsValue(o);
            }

            public void clear() {
                map.clear();
            }
        };
    }
}
