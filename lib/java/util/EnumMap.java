package java.util;

/**
 * The model of java.util.EnumMap: a value for each of an enum's constants, iterated in their order (ordinal), null
 * values allowed, the JDK's messages. clone isn't modelled.
 */
public class EnumMap<K extends Enum<K>, V> extends AbstractMap<K, V> implements java.io.Serializable, Cloneable {
    private final Class<K> keyType;
    private transient K[] keyUniverse;
    /** Each constant's value, NULL standing for a null one, null for none. */
    private transient Object[] vals;
    private transient int size = 0;

    private static final Object NULL = new Object();

    private Object maskNull(Object value) {
        return (value == null ? NULL : value);
    }

    @SuppressWarnings("unchecked")
    private V unmaskNull(Object value) {
        return (V) (value == NULL ? null : value);
    }

    @SuppressWarnings("unchecked")
    public EnumMap(Class<K> keyType) {
        this.keyType = keyType;
        keyUniverse = (K[]) keyType.jEnumUniverse();
        vals = new Object[keyUniverse.length];
    }

    public EnumMap(EnumMap<K, ? extends V> m) {
        keyType = m.keyType;
        keyUniverse = m.keyUniverse;
        vals = Arrays.copyOfObjects(m.vals, m.vals.length);
        size = m.size;
    }

    @SuppressWarnings("unchecked")
    public EnumMap(Map<K, ? extends V> m) {
        if (m instanceof EnumMap) {
            EnumMap<K, ? extends V> em = (EnumMap<K, ? extends V>) m;
            keyType = em.keyType;
            keyUniverse = em.keyUniverse;
            vals = Arrays.copyOfObjects(em.vals, em.vals.length);
            size = em.size;
        } else {
            if (m.isEmpty()) throw new IllegalArgumentException("Specified map is empty");
            keyType = m.keySet().iterator().next().getDeclaringClass();
            keyUniverse = (K[]) keyType.jEnumUniverse();
            vals = new Object[keyUniverse.length];
            putAll(m);
        }
    }

    public int size() {
        return size;
    }

    public boolean containsValue(Object value) {
        value = maskNull(value);
        for (Object val : vals) if (value.equals(val)) return true;
        return false;
    }

    public boolean containsKey(Object key) {
        return isValidKey(key) && vals[((Enum<?>) key).ordinal()] != null;
    }

    private boolean containsMapping(Object key, Object value) {
        return isValidKey(key) && maskNull(value).equals(vals[((Enum<?>) key).ordinal()]);
    }

    public V get(Object key) {
        return (isValidKey(key) ? unmaskNull(vals[((Enum<?>) key).ordinal()]) : null);
    }

    public V put(K key, V value) {
        typeCheck(key);
        int index = key.ordinal();
        Object oldValue = vals[index];
        vals[index] = maskNull(value);
        if (oldValue == null) size++;
        return unmaskNull(oldValue);
    }

    public V remove(Object key) {
        if (!isValidKey(key)) return null;
        int index = ((Enum<?>) key).ordinal();
        Object oldValue = vals[index];
        vals[index] = null;
        if (oldValue != null) size--;
        return unmaskNull(oldValue);
    }

    private boolean removeMapping(Object key, Object value) {
        if (!isValidKey(key)) return false;
        int index = ((Enum<?>) key).ordinal();
        if (maskNull(value).equals(vals[index])) {
            vals[index] = null;
            size--;
            return true;
        }
        return false;
    }

    private boolean isValidKey(Object key) {
        if (key == null) return false;
        Class<?> keyClass = key.getClass();
        return keyClass == keyType || keyClass.getSuperclass() == keyType;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) put(e.getKey(), e.getValue());
    }

    public void clear() {
        Arrays.fill(vals, null);
        size = 0;
    }

    private void typeCheck(K key) {
        Class<?> keyClass = key.getClass();
        if (keyClass != keyType && keyClass.getSuperclass() != keyType) {
            throw new ClassCastException(String.valueOf(keyClass).concat(" != ").concat(String.valueOf(keyType)));
        }
    }

    public Set<K> keySet() {
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new KeySet();
            keySet = ks;
        }
        return ks;
    }

    private class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        public int size() {
            return size;
        }

        public boolean contains(Object o) {
            return containsKey(o);
        }

        public boolean remove(Object o) {
            int oldSize = size;
            EnumMap.this.remove(o);
            return size != oldSize;
        }

        public void clear() {
            EnumMap.this.clear();
        }
    }

    public Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            vs = new Values();
            values = vs;
        }
        return vs;
    }

    private class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        public int size() {
            return size;
        }

        public boolean contains(Object o) {
            return containsValue(o);
        }

        public boolean remove(Object o) {
            o = maskNull(o);
            for (int i = 0; i < vals.length; i++) {
                if (o.equals(vals[i])) {
                    vals[i] = null;
                    size--;
                    return true;
                }
            }
            return false;
        }

        public void clear() {
            EnumMap.this.clear();
        }
    }

    private transient Set<Map.Entry<K, V>> entrySet;

    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> es = entrySet;
        if (es != null) return es;
        return entrySet = new EntrySet();
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        public boolean contains(Object o) {
            return o instanceof Map.Entry<?, ?> entry && containsMapping(entry.getKey(), entry.getValue());
        }

        public boolean remove(Object o) {
            return o instanceof Map.Entry<?, ?> entry && removeMapping(entry.getKey(), entry.getValue());
        }

        public int size() {
            return size;
        }

        public void clear() {
            EnumMap.this.clear();
        }
    }

    private abstract class EnumMapIterator<T> implements Iterator<T> {
        int index = 0;
        int lastReturnedIndex = -1;

        public boolean hasNext() {
            while (index < vals.length && vals[index] == null) index++;
            return index != vals.length;
        }

        public void remove() {
            checkLastReturnedIndex();
            if (vals[lastReturnedIndex] != null) {
                vals[lastReturnedIndex] = null;
                size--;
            }
            lastReturnedIndex = -1;
        }

        private void checkLastReturnedIndex() {
            if (lastReturnedIndex < 0) throw new IllegalStateException();
        }
    }

    private class KeyIterator extends EnumMapIterator<K> {
        public K next() {
            if (!hasNext()) throw new NoSuchElementException();
            lastReturnedIndex = index++;
            return keyUniverse[lastReturnedIndex];
        }
    }

    private class ValueIterator extends EnumMapIterator<V> {
        public V next() {
            if (!hasNext()) throw new NoSuchElementException();
            lastReturnedIndex = index++;
            return unmaskNull(vals[lastReturnedIndex]);
        }
    }

    private class EntryIterator extends EnumMapIterator<Map.Entry<K, V>> {
        public Map.Entry<K, V> next() {
            if (!hasNext()) throw new NoSuchElementException();
            lastReturnedIndex = index++;
            return new EntryAt(lastReturnedIndex);
        }
    }

    /** An entry of the map: what it changes, it changes in the map. */
    private class EntryAt implements Map.Entry<K, V> {
        private final int at;

        EntryAt(int at) {
            this.at = at;
        }

        public K getKey() {
            return keyUniverse[at];
        }

        public V getValue() {
            return unmaskNull(vals[at]);
        }

        public V setValue(V value) {
            V oldValue = unmaskNull(vals[at]);
            vals[at] = maskNull(value);
            return oldValue;
        }

        public boolean equals(Object o) {
            return o instanceof Map.Entry<?, ?> e && keyUniverse[at] == e.getKey() && Objects.equals(unmaskNull(vals[at]), e.getValue());
        }

        public int hashCode() {
            return keyUniverse[at].hashCode() ^ Objects.hashCode(unmaskNull(vals[at]));
        }

        public String toString() {
            return String.valueOf(keyUniverse[at]).concat("=").concat(String.valueOf(unmaskNull(vals[at])));
        }
    }
}
