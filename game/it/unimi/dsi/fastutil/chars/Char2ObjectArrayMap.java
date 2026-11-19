package it.unimi.dsi.fastutil.chars;

import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;

/**
 * The stand-in for fastutil's Char2ObjectArrayMap: its keys and values in two arrays, in the order they were put, a
 * key found from the last one back; a removal moves the following ones down (observed against fastutil, `standins`).
 */
public class Char2ObjectArrayMap<V> extends AbstractChar2ObjectMap<V> implements java.io.Serializable, Cloneable {
    private transient char[] key;
    private transient Object[] value;
    private int size;

    public Char2ObjectArrayMap() {
        this(0);
    }

    public Char2ObjectArrayMap(int capacity) {
        key = new char[capacity];
        value = new Object[capacity];
    }

    private int findKey(char k) {
        for (int i = size; i-- != 0;) if (key[i] == k) return i;
        return -1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(char k) {
        for (int i = size; i-- != 0;) if (key[i] == k) return (V) value[i];
        return defRetValue;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        for (int i = size; i-- != 0;) value[i] = null;
        size = 0;
    }

    @Override
    public boolean containsKey(char k) {
        return findKey(k) != -1;
    }

    @Override
    public boolean containsValue(Object v) {
        for (int i = size; i-- != 0;) if (java.util.Objects.equals(value[i], v)) return true;
        return false;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V put(char k, V v) {
        int oldKey = findKey(k);
        if (oldKey != -1) {
            V oldValue = (V) value[oldKey];
            value[oldKey] = v;
            return oldValue;
        }
        if (size == key.length) {
            char[] newKey = new char[size == 0 ? 2 : size * 2];
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
    public V remove(char k) {
        int oldPos = findKey(k);
        if (oldPos == -1) return defRetValue;
        V oldValue = (V) value[oldPos];
        int tail = size - oldPos - 1;
        System.arraycopy(key, oldPos + 1, key, oldPos, tail);
        System.arraycopy(value, oldPos + 1, value, oldPos, tail);
        size--;
        value[size] = null;
        return oldValue;
    }

    /** A copy: its own arrays, of its length. */
    @Override
    @SuppressWarnings("unchecked")
    public Char2ObjectArrayMap<V> clone() {
        Char2ObjectArrayMap<V> c = new Char2ObjectArrayMap<>(0);
        c.key = key.clone();
        c.value = value.clone();
        c.size = size;
        c.defRetValue = defRetValue;
        return c;
    }

    /** Its entries in its order. */
    @Override
    public ObjectSet<Char2ObjectMap.Entry<V>> char2ObjectEntrySet() {
        return new EntrySet();
    }

    /** Its keys in its order. */
    @Override
    public CharSet keySet() {
        return new KeySet();
    }

    /** Its values in its order. */
    @Override
    public it.unimi.dsi.fastutil.objects.ObjectCollection<V> values() {
        return new ValuesCollection();
    }

    private final class EntrySet extends AbstractObjectSet<Char2ObjectMap.Entry<V>> implements Char2ObjectMap.FastEntrySet<V> {
        @Override
        public ObjectIterator<Char2ObjectMap.Entry<V>> iterator() {
            return new ObjectIterator<Char2ObjectMap.Entry<V>>() {
                int next = 0;

                @Override
                public boolean hasNext() {
                    return next < size;
                }

                @Override
                @SuppressWarnings("unchecked")
                public Char2ObjectMap.Entry<V> next() {
                    if (!hasNext()) throw new java.util.NoSuchElementException();
                    int i = next++;
                    return new AbstractChar2ObjectMap.BasicEntry<>(key[i], (V) value[i]);
                }
            };
        }

        /** An entry of a char key and its value. */
        @Override
        public boolean contains(Object o) {
            if (!(o instanceof java.util.Map.Entry<?, ?> e)) return false;
            if (!(e.getKey() instanceof Character c)) return false;
            char k = c.charValue();
            return Char2ObjectArrayMap.this.containsKey(k) && java.util.Objects.equals(Char2ObjectArrayMap.this.get(k), e.getValue());
        }

        /** The entry of that key, when it holds that value. */
        @Override
        public boolean remove(Object o) {
            if (!(o instanceof java.util.Map.Entry<?, ?> e)) return false;
            if (!(e.getKey() instanceof Character c)) return false;
            char k = c.charValue();
            int oldPos = findKey(k);
            if (oldPos == -1 || !java.util.Objects.equals(value[oldPos], e.getValue())) return false;
            Char2ObjectArrayMap.this.remove(k);
            return true;
        }

        @Override
        public ObjectIterator<Char2ObjectMap.Entry<V>> fastIterator() {
            return iterator();
        }

        @Override
        public int size() {
            return size;
        }
    }

    private final class KeySet extends AbstractCharSet {
        @Override
        public boolean contains(char k) {
            return findKey(k) != -1;
        }

        @Override
        public boolean remove(char k) {
            if (findKey(k) == -1) return false;
            Char2ObjectArrayMap.this.remove(k);
            return true;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            Char2ObjectArrayMap.this.clear();
        }

        @Override
        public CharIterator iterator() {
            return new CharIterator() {
                int pos = 0;

                @Override
                public boolean hasNext() {
                    return pos < size;
                }

                @Override
                public char nextChar() {
                    if (!hasNext()) throw new java.util.NoSuchElementException();
                    return key[pos++];
                }
            };
        }
    }

    private final class ValuesCollection extends it.unimi.dsi.fastutil.objects.AbstractObjectCollection<V> {
        @Override
        public boolean contains(Object v) {
            return containsValue(v);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            Char2ObjectArrayMap.this.clear();
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
                    if (!hasNext()) throw new java.util.NoSuchElementException();
                    return (V) value[pos++];
                }
            };
        }
    }
}
