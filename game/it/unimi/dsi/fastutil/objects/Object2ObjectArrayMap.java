package it.unimi.dsi.fastutil.objects;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The stand-in for fastutil's Object2ObjectArrayMap: its keys and values in two arrays (protected, as a subclass reads
 * them), in the order they were put, a key found from the last one back by equals; the arrays doubling when full; a
 * removal, from the map or through a view or an iterator, moving the following ones down. Its views are made once.
 * Observed against fastutil (`standins`). Their spliterators are Java's defaults (characteristics aren't fastutil's).
 */
public class Object2ObjectArrayMap<K, V> extends AbstractObject2ObjectMap<K, V> implements java.io.Serializable, Cloneable {
    protected transient Object[] key;
    protected transient Object[] value;
    protected int size;
    protected transient Object2ObjectMap.FastEntrySet<K, V> entries;
    protected transient ObjectSet<K> keys;
    protected transient ObjectCollection<V> values;

    public Object2ObjectArrayMap(Object[] key, Object[] value) {
        this.key = key;
        this.value = value;
        size = key.length;
        if (key.length != value.length) throw new IllegalArgumentException("Keys and values have different lengths (" + key.length + ", " + value.length + ")");
    }

    public Object2ObjectArrayMap() {
        this.key = ObjectArrays.EMPTY_ARRAY;
        this.value = ObjectArrays.EMPTY_ARRAY;
    }

    public Object2ObjectArrayMap(int capacity) {
        this.key = new Object[capacity];
        this.value = new Object[capacity];
    }

    public Object2ObjectArrayMap(Object2ObjectMap<K, V> m) {
        this(m.size());
        int i = 0;
        for (Object2ObjectMap.Entry<K, V> e : m.object2ObjectEntrySet()) {
            key[i] = e.getKey();
            value[i] = e.getValue();
            i++;
        }
        size = i;
    }

    public Object2ObjectArrayMap(Map<? extends K, ? extends V> m) {
        this(m.size());
        int i = 0;
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            key[i] = e.getKey();
            value[i] = e.getValue();
            i++;
        }
        size = i;
    }

    public Object2ObjectArrayMap(Object[] key, Object[] value, int size) {
        this.key = key;
        this.value = value;
        this.size = size;
        if (key.length != value.length) throw new IllegalArgumentException("Keys and values have different lengths (" + key.length + ", " + value.length + ")");
        if (size > key.length) throw new IllegalArgumentException("The provided size (" + size + ") is larger than or equal to the backing-arrays size (" + key.length + ")");
    }

    /** The view of its entries, made once: new entries, or one reused by fastIterator and fastForEach. */
    @Override
    public Object2ObjectMap.FastEntrySet<K, V> object2ObjectEntrySet() {
        if (entries == null) entries = new EntrySet();
        return entries;
    }

    private int findKey(Object k) {
        Object[] key = this.key;
        for (int i = size; i-- != 0;) if (Objects.equals(key[i], k)) return i;
        return -1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object k) {
        Object[] key = this.key;
        for (int i = size; i-- != 0;) if (Objects.equals(key[i], k)) return (V) value[i];
        return defRetValue;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        for (int i = size; i-- != 0;) {
            key[i] = null;
            value[i] = null;
        }
        size = 0;
    }

    @Override
    public boolean containsKey(Object k) {
        return findKey(k) != -1;
    }

    @Override
    public boolean containsValue(Object v) {
        for (int i = size; i-- != 0;) if (Objects.equals(value[i], v)) return true;
        return false;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V put(K k, V v) {
        int oldKey = findKey(k);
        if (oldKey != -1) {
            V oldValue = (V) value[oldKey];
            value[oldKey] = v;
            return oldValue;
        }
        if (size == key.length) {
            Object[] newKey = new Object[size == 0 ? 2 : size * 2];
            Object[] newValue = new Object[size == 0 ? 2 : size * 2];
            for (int i = size; i-- != 0;) {
                newKey[i] = key[i];
                newValue[i] = value[i];
            }
            key = newKey;
            value = newValue;
        }
        key[size] = k;
        value[size] = v;
        size++;
        return defRetValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V remove(Object k) {
        int oldPos = findKey(k);
        if (oldPos == -1) return defRetValue;
        V oldValue = (V) value[oldPos];
        int tail = size - oldPos - 1;
        System.arraycopy(key, oldPos + 1, key, oldPos, tail);
        System.arraycopy(value, oldPos + 1, value, oldPos, tail);
        size--;
        key[size] = null;
        value[size] = null;
        return oldValue;
    }

    /** The view of its keys, made once. */
    @Override
    public ObjectSet<K> keySet() {
        if (keys == null) keys = new KeySet();
        return keys;
    }

    /** The view of its values, made once. */
    @Override
    public ObjectCollection<V> values() {
        if (values == null) values = new ValuesCollection();
        return values;
    }

    /** A copy with arrays of its own, as long as its, and views of its own; a subclass's refuses. */
    @Override
    @SuppressWarnings("unchecked")
    public Object2ObjectArrayMap<K, V> clone() {
        if (getClass() != Object2ObjectArrayMap.class) throw new UnsupportedOperationException("cloning a subclass of fastutil's Object2ObjectArrayMap isn't modelled (horizon-port-neoforge)");
        Object2ObjectArrayMap<K, V> c = new Object2ObjectArrayMap<>(key.clone(), value.clone(), size);
        c.defRetValue = defRetValue;
        return c;
    }

    /** Removes the element at i, moving the following ones down. */
    private void removeAt(int i) {
        int tail = size - i - 1;
        System.arraycopy(key, i + 1, key, i, tail);
        System.arraycopy(value, i + 1, value, i, tail);
        size--;
        key[size] = null;
        value[size] = null;
    }

    private final class EntrySet extends AbstractObjectSet<Object2ObjectMap.Entry<K, V>> implements Object2ObjectMap.FastEntrySet<K, V> {
        @Override
        public ObjectIterator<Object2ObjectMap.Entry<K, V>> iterator() {
            return new ObjectIterator<Object2ObjectMap.Entry<K, V>>() {
                int curr = -1, next = 0;

                @Override
                public boolean hasNext() {
                    return next < size;
                }

                @Override
                @SuppressWarnings("unchecked")
                public Object2ObjectMap.Entry<K, V> next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return new AbstractObject2ObjectMap.BasicEntry<>((K) key[curr = next], (V) value[next++]);
                }

                @Override
                public void remove() {
                    if (curr == -1) throw new IllegalStateException();
                    curr = -1;
                    removeAt(--next);
                }

                @Override
                @SuppressWarnings("unchecked")
                public void forEachRemaining(Consumer<? super Object2ObjectMap.Entry<K, V>> action) {
                    int max = size;
                    while (next < max) action.accept(new AbstractObject2ObjectMap.BasicEntry<>((K) key[curr = next], (V) value[next++]));
                }
            };
        }

        @Override
        public ObjectIterator<Object2ObjectMap.Entry<K, V>> fastIterator() {
            return new ObjectIterator<Object2ObjectMap.Entry<K, V>>() {
                int next = 0, curr = -1;
                final AbstractObject2ObjectMap.BasicEntry<K, V> entry = new AbstractObject2ObjectMap.BasicEntry<>();

                @Override
                public boolean hasNext() {
                    return next < size;
                }

                @Override
                @SuppressWarnings("unchecked")
                public Object2ObjectMap.Entry<K, V> next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    entry.key = (K) key[curr = next];
                    entry.value = (V) value[next++];
                    return entry;
                }

                @Override
                public void remove() {
                    if (curr == -1) throw new IllegalStateException();
                    curr = -1;
                    removeAt(--next);
                }

                @Override
                @SuppressWarnings("unchecked")
                public void forEachRemaining(Consumer<? super Object2ObjectMap.Entry<K, V>> action) {
                    int max = size;
                    while (next < max) {
                        entry.key = (K) key[curr = next];
                        entry.value = (V) value[next++];
                        action.accept(entry);
                    }
                }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        public void forEach(Consumer<? super Object2ObjectMap.Entry<K, V>> action) {
            for (int i = 0, max = size; i < max; ++i) action.accept(new AbstractObject2ObjectMap.BasicEntry<>((K) key[i], (V) value[i]));
        }

        @Override
        @SuppressWarnings("unchecked")
        public void fastForEach(Consumer<? super Object2ObjectMap.Entry<K, V>> action) {
            AbstractObject2ObjectMap.BasicEntry<K, V> entry = new AbstractObject2ObjectMap.BasicEntry<>();
            for (int i = 0, max = size; i < max; ++i) {
                entry.key = (K) key[i];
                entry.value = (V) value[i];
                action.accept(entry);
            }
        }

        @Override
        public int size() {
            return size;
        }

        /** An entry of a key it holds, with the value it holds there. */
        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) return false;
            Object k = e.getKey();
            return Object2ObjectArrayMap.this.containsKey(k) && Objects.equals(Object2ObjectArrayMap.this.get(k), e.getValue());
        }

        /** The entry of that key, when it holds that value. */
        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) return false;
            int oldPos = findKey(e.getKey());
            if (oldPos == -1 || !Objects.equals(e.getValue(), value[oldPos])) return false;
            removeAt(oldPos);
            return true;
        }
    }

    private final class KeySet extends AbstractObjectSet<K> {
        @Override
        public boolean contains(Object k) {
            return findKey(k) != -1;
        }

        @Override
        public boolean remove(Object k) {
            int oldPos = findKey(k);
            if (oldPos == -1) return false;
            removeAt(oldPos);
            return true;
        }

        @Override
        public ObjectIterator<K> iterator() {
            return new ObjectIterator<K>() {
                int pos = 0;

                @Override
                public boolean hasNext() {
                    return pos < size;
                }

                @Override
                @SuppressWarnings("unchecked")
                public K next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return (K) key[pos++];
                }

                @Override
                public void remove() {
                    if (pos == 0) throw new IllegalStateException();
                    removeAt(--pos);
                }

                @Override
                @SuppressWarnings("unchecked")
                public void forEachRemaining(Consumer<? super K> action) {
                    int max = size;
                    while (pos < max) action.accept((K) key[pos++]);
                }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        public void forEach(Consumer<? super K> action) {
            for (int i = 0, max = size; i < max; ++i) action.accept((K) key[i]);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            Object2ObjectArrayMap.this.clear();
        }
    }

    private final class ValuesCollection extends AbstractObjectCollection<V> {
        @Override
        public boolean contains(Object v) {
            return containsValue(v);
        }

        @Override
        public ObjectIterator<V> iterator() {
            return new ObjectIterator<V>() {
                int pos = 0;

                @Override
                public boolean hasNext() {
                    return pos < size;
                }

                @Override
                @SuppressWarnings("unchecked")
                public V next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return (V) value[pos++];
                }

                @Override
                public void remove() {
                    if (pos == 0) throw new IllegalStateException();
                    removeAt(--pos);
                }

                @Override
                @SuppressWarnings("unchecked")
                public void forEachRemaining(Consumer<? super V> action) {
                    int max = size;
                    while (pos < max) action.accept((V) value[pos++]);
                }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        public void forEach(Consumer<? super V> action) {
            for (int i = 0, max = size; i < max; ++i) action.accept((V) value[i]);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            Object2ObjectArrayMap.this.clear();
        }
    }
}
