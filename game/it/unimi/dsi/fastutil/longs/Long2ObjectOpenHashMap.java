package it.unimi.dsi.fastutil.longs;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import it.unimi.dsi.fastutil.objects.AbstractObjectCollection;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;

/**
 * The stand-in for fastutil's Long2ObjectOpenHashMap: the open hash table of LongOpenHashSet with a value beside
 * each key (the 0 key's last, as null is the object map's), gone through in the same order. Observed against
 * fastutil's own (`standins`).
 */
public class Long2ObjectOpenHashMap<V> extends AbstractLong2ObjectMap<V> implements java.io.Serializable, Cloneable, Hash {
    protected transient long[] key;
    protected transient V[] value;
    protected transient int mask;
    protected transient boolean containsNullKey;
    protected transient int n;
    protected transient int maxFill;
    protected final transient int minN;
    protected int size;
    protected final float f;
    protected transient FastEntrySet<V> entries;
    protected transient LongSet keys;
    protected transient ObjectCollection<V> values;

    @SuppressWarnings("unchecked")
    public Long2ObjectOpenHashMap(int expected, float f) {
        if (f <= 0 || f >= 1) throw new IllegalArgumentException("Load factor must be greater than 0 and smaller than 1");
        if (expected < 0) throw new IllegalArgumentException("The expected number of elements must be nonnegative");
        this.f = f;
        minN = n = HashCommon.arraySize(expected, f);
        mask = n - 1;
        maxFill = HashCommon.maxFill(n, f);
        key = new long[n + 1];
        value = (V[]) new Object[n + 1];
    }

    public Long2ObjectOpenHashMap(int expected) {
        this(expected, DEFAULT_LOAD_FACTOR);
    }

    public Long2ObjectOpenHashMap() {
        this(DEFAULT_INITIAL_SIZE, DEFAULT_LOAD_FACTOR);
    }

    public Long2ObjectOpenHashMap(Map<? extends Long, ? extends V> m, float f) {
        this(m.size(), f);
        putAll(m);
    }

    public Long2ObjectOpenHashMap(Map<? extends Long, ? extends V> m) {
        this(m, DEFAULT_LOAD_FACTOR);
    }

    public Long2ObjectOpenHashMap(Long2ObjectMap<V> m, float f) {
        this(m.size(), f);
        putAll(m);
    }

    public Long2ObjectOpenHashMap(Long2ObjectMap<V> m) {
        this(m, DEFAULT_LOAD_FACTOR);
    }

    public Long2ObjectOpenHashMap(long[] k, V[] v, float f) {
        this(k.length, f);
        if (k.length != v.length) throw new IllegalArgumentException("The key array and the value array have different lengths (" + k.length + " and " + v.length + ")");
        for (int i = 0; i < k.length; i++) put(k[i], v[i]);
    }

    public Long2ObjectOpenHashMap(long[] k, V[] v) {
        this(k, v, DEFAULT_LOAD_FACTOR);
    }

    private int realSize() {
        return containsNullKey ? size - 1 : size;
    }

    public void ensureCapacity(int capacity) {
        int needed = HashCommon.arraySize(capacity, f);
        if (needed > n) rehash(needed);
    }

    private void tryCapacity(long capacity) {
        int needed = (int) Math.min(1 << 30, Math.max(2, HashCommon.nextPowerOfTwo((long) Math.ceil(capacity / f))));
        if (needed > n) rehash(needed);
    }

    private V removeEntry(int pos) {
        V oldValue = value[pos];
        value[pos] = null;
        size--;
        shiftKeys(pos);
        if (n > minN && size < maxFill / 4 && n > DEFAULT_INITIAL_SIZE) rehash(n / 2);
        return oldValue;
    }

    private V removeNullEntry() {
        containsNullKey = false;
        key[n] = 0;
        V oldValue = value[n];
        value[n] = null;
        size--;
        if (n > minN && size < maxFill / 4 && n > DEFAULT_INITIAL_SIZE) rehash(n / 2);
        return oldValue;
    }

    @Override
    public void putAll(Map<? extends Long, ? extends V> m) {
        if (f <= .5) ensureCapacity(m.size());
        else tryCapacity(size() + m.size());
        super.putAll(m);
    }

    /** The key's slot, else -(the free slot it would go in) - 1. */
    private int find(long k) {
        if (k == 0) return containsNullKey ? n : -(n + 1);
        long curr;
        int pos;
        if ((curr = key[pos = (int) HashCommon.mix(k) & mask]) == 0) return -(pos + 1);
        if (k == curr) return pos;
        while (true) {
            if ((curr = key[pos = (pos + 1) & mask]) == 0) return -(pos + 1);
            if (k == curr) return pos;
        }
    }

    private void insert(int pos, long k, V v) {
        if (pos == n) containsNullKey = true;
        key[pos] = k;
        value[pos] = v;
        if (size++ >= maxFill) rehash(HashCommon.arraySize(size + 1, f));
    }

    @Override
    public V put(long k, V v) {
        int pos = find(k);
        if (pos < 0) {
            insert(-pos - 1, k, v);
            return defRetValue;
        }
        V oldValue = value[pos];
        value[pos] = v;
        return oldValue;
    }

    protected final void shiftKeys(int pos) {
        int last, slot;
        long curr;
        for (;;) {
            pos = ((last = pos) + 1) & mask;
            for (;;) {
                if ((curr = key[pos]) == 0) {
                    key[last] = 0;
                    value[last] = null;
                    return;
                }
                slot = (int) HashCommon.mix(curr) & mask;
                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                pos = (pos + 1) & mask;
            }
            key[last] = curr;
            value[last] = value[pos];
        }
    }

    @Override
    public V remove(long k) {
        if (k == 0) return containsNullKey ? removeNullEntry() : defRetValue;
        int pos = find(k);
        return pos < 0 ? defRetValue : removeEntry(pos);
    }

    @Override
    public V get(long k) {
        int pos = find(k);
        return pos < 0 ? defRetValue : value[pos];
    }

    @Override
    public boolean containsKey(long k) {
        return find(k) >= 0;
    }

    @Override
    public boolean containsValue(Object v) {
        if (containsNullKey && Objects.equals(value[n], v)) return true;
        for (int i = n; i-- != 0;) if (key[i] != 0 && Objects.equals(value[i], v)) return true;
        return false;
    }

    @Override
    public V getOrDefault(long k, V defaultValue) {
        int pos = find(k);
        return pos < 0 ? defaultValue : value[pos];
    }

    @Override
    public V putIfAbsent(long k, V v) {
        int pos = find(k);
        if (pos >= 0) return value[pos];
        insert(-pos - 1, k, v);
        return defRetValue;
    }

    @Override
    public boolean remove(long k, Object v) {
        int pos = find(k);
        if (pos < 0 || !Objects.equals(v, value[pos])) return false;
        if (k == 0) removeNullEntry();
        else removeEntry(pos);
        return true;
    }

    @Override
    public boolean replace(long k, V oldValue, V v) {
        int pos = find(k);
        if (pos < 0 || !Objects.equals(oldValue, value[pos])) return false;
        value[pos] = v;
        return true;
    }

    @Override
    public V replace(long k, V v) {
        int pos = find(k);
        if (pos < 0) return defRetValue;
        V oldValue = value[pos];
        value[pos] = v;
        return oldValue;
    }

    @Override
    public V computeIfAbsent(long k, java.util.function.LongFunction<? extends V> mapping) {
        Objects.requireNonNull(mapping);
        int pos = find(k);
        if (pos >= 0) return value[pos];
        V newValue = mapping.apply(k);
        insert(-pos - 1, k, newValue);
        return newValue;
    }

    @Override
    public V computeIfAbsent(long k, Long2ObjectFunction<? extends V> mapping) {
        Objects.requireNonNull(mapping);
        int pos = find(k);
        if (pos >= 0) return value[pos];
        if (!mapping.containsKey(k)) return defRetValue;
        V newValue = mapping.get(k);
        insert(-pos - 1, k, newValue);
        return newValue;
    }

    @Override
    public V computeIfPresent(long k, BiFunction<? super Long, ? super V, ? extends V> remapping) {
        Objects.requireNonNull(remapping);
        int pos = find(k);
        if (pos < 0) return defRetValue;
        if (value[pos] == null) return defRetValue;
        V newValue = remapping.apply(k, value[pos]);
        if (newValue == null) {
            if (k == 0) removeNullEntry();
            else removeEntry(pos);
            return defRetValue;
        }
        return value[pos] = newValue;
    }

    @Override
    public V compute(long k, BiFunction<? super Long, ? super V, ? extends V> remapping) {
        Objects.requireNonNull(remapping);
        int pos = find(k);
        V newValue = remapping.apply(k, pos >= 0 ? value[pos] : null);
        if (newValue == null) {
            if (pos >= 0) {
                if (k == 0) removeNullEntry();
                else removeEntry(pos);
            }
            return defRetValue;
        }
        if (pos < 0) {
            insert(-pos - 1, k, newValue);
            return newValue;
        }
        return value[pos] = newValue;
    }

    @Override
    public V merge(long k, V v, BiFunction<? super V, ? super V, ? extends V> remapping) {
        Objects.requireNonNull(remapping);
        Objects.requireNonNull(v);
        int pos = find(k);
        if (pos < 0 || value[pos] == null) {
            if (pos < 0) insert(-pos - 1, k, v);
            else value[pos] = v;
            return v;
        }
        V newValue = remapping.apply(value[pos], v);
        if (newValue == null) {
            if (k == 0) removeNullEntry();
            else removeEntry(pos);
            return defRetValue;
        }
        return value[pos] = newValue;
    }

    @Override
    public void clear() {
        if (size == 0) return;
        size = 0;
        containsNullKey = false;
        Arrays.fill(key, 0L);
        Arrays.fill(value, null);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /** An entry: the map's slot it reads and writes. */
    final class MapEntry implements Long2ObjectMap.Entry<V>, Map.Entry<Long, V>, LongObjectPair<V> {
        int index;

        MapEntry(int index) {
            this.index = index;
        }

        MapEntry() {}

        @Override
        public long getLongKey() {
            return key[index];
        }

        @Override
        public long leftLong() {
            return key[index];
        }

        @Override
        public V getValue() {
            return value[index];
        }

        @Override
        public V right() {
            return value[index];
        }

        @Override
        public LongObjectPair<V> right(V v) {
            value[index] = v;
            return this;
        }

        @Override
        public V setValue(V v) {
            V oldValue = value[index];
            value[index] = v;
            return oldValue;
        }

        @Override
        public Long getKey() {
            return key[index];
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Map.Entry<?, ?> e && e.getKey() instanceof Long l && key[index] == l.longValue() && Objects.equals(value[index], e.getValue());
        }

        @Override
        public int hashCode() {
            return HashCommon.long2int(key[index]) ^ (value[index] == null ? 0 : value[index].hashCode());
        }

        @Override
        public String toString() {
            return key[index] + "=>" + value[index];
        }
    }

    /** The slots from the last to the first, the null key's first; a removal behind it is gone through as fastutil's. */
    private abstract class MapIterator {
        int pos = n;
        int last = -1;
        int c = size;
        boolean mustReturnNullKey = Long2ObjectOpenHashMap.this.containsNullKey;
        HorizonLongList wrapped;

        public boolean hasNext() {
            return c != 0;
        }

        int nextEntry() {
            if (!hasNext()) throw new NoSuchElementException();
            c--;
            if (mustReturnNullKey) {
                mustReturnNullKey = false;
                return last = n;
            }
            for (;;) {
                if (--pos < 0) {
                    last = Integer.MIN_VALUE;
                    long k = wrapped.get(-pos - 1);
                    int p = (int) HashCommon.mix(k) & mask;
                    while (k != key[p]) p = (p + 1) & mask;
                    return p;
                }
                if (key[pos] != 0) return last = pos;
            }
        }

        private void shiftKeys(int pos) {
            int last, slot;
            long curr;
            for (;;) {
                pos = ((last = pos) + 1) & mask;
                for (;;) {
                    if ((curr = key[pos]) == 0) {
                        key[last] = 0;
                        value[last] = null;
                        return;
                    }
                    slot = (int) HashCommon.mix(curr) & mask;
                    if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                    pos = (pos + 1) & mask;
                }
                if (pos < last) {
                    if (wrapped == null) wrapped = new HorizonLongList();
                    wrapped.add(key[pos]);
                }
                key[last] = curr;
                value[last] = value[pos];
            }
        }

        public void remove() {
            if (last == -1) throw new IllegalStateException();
            if (last == n) {
                containsNullKey = false;
                key[n] = 0;
                value[n] = null;
            } else if (pos >= 0) {
                shiftKeys(last);
            } else {
                Long2ObjectOpenHashMap.this.remove(wrapped.get(-pos - 1));
                last = -1;
                return;
            }
            size--;
            last = -1;
        }

        public int skip(int skip) {
            int i = skip;
            while (i-- != 0 && hasNext()) nextEntry();
            return skip - i - 1;
        }
    }

    private final class EntryIterator extends MapIterator implements ObjectIterator<Entry<V>> {
        private MapEntry entry;

        @Override
        public MapEntry next() {
            return entry = new MapEntry(nextEntry());
        }

        @Override
        public void remove() {
            super.remove();
            entry.index = -1;
        }

        @Override
        public void forEachRemaining(Consumer<? super Entry<V>> action) {
            while (hasNext()) action.accept(next());
        }
    }

    private final class FastEntryIterator extends MapIterator implements ObjectIterator<Entry<V>> {
        private final MapEntry entry = new MapEntry();

        @Override
        public MapEntry next() {
            entry.index = nextEntry();
            return entry;
        }

        @Override
        public void forEachRemaining(Consumer<? super Entry<V>> action) {
            while (hasNext()) action.accept(next());
        }
    }

    private final class KeyIterator extends MapIterator implements LongIterator {
        @Override
        public long nextLong() {
            return key[nextEntry()];
        }
    }

    private final class ValueIterator extends MapIterator implements ObjectIterator<V> {
        @Override
        public V next() {
            return value[nextEntry()];
        }

        @Override
        public void forEachRemaining(Consumer<? super V> action) {
            while (hasNext()) action.accept(next());
        }
    }

    private final class MapEntrySet extends AbstractObjectSet<Entry<V>> implements FastEntrySet<V> {
        @Override
        public ObjectIterator<Entry<V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public ObjectIterator<Entry<V>> fastIterator() {
            return new FastEntryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e) || !(e.getKey() instanceof Long l)) return false;
            int pos = find(l.longValue());
            return pos >= 0 && Objects.equals(value[pos], e.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e) || !(e.getKey() instanceof Long l)) return false;
            int pos = find(l.longValue());
            if (pos < 0 || !Objects.equals(value[pos], e.getValue())) return false;
            if (l.longValue() == 0) removeNullEntry();
            else removeEntry(pos);
            return true;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            Long2ObjectOpenHashMap.this.clear();
        }

        @Override
        public void forEach(Consumer<? super Entry<V>> consumer) {
            if (containsNullKey) consumer.accept(new MapEntry(n));
            for (int pos = n; pos-- != 0;) if (key[pos] != 0) consumer.accept(new MapEntry(pos));
        }

        @Override
        public void fastForEach(Consumer<? super Entry<V>> consumer) {
            MapEntry entry = new MapEntry();
            if (containsNullKey) {
                entry.index = n;
                consumer.accept(entry);
            }
            for (int pos = n; pos-- != 0;) {
                if (key[pos] != 0) {
                    entry.index = pos;
                    consumer.accept(entry);
                }
            }
        }
    }

    @Override
    public FastEntrySet<V> long2ObjectEntrySet() {
        if (entries == null) entries = new MapEntrySet();
        return entries;
    }

    private final class KeySet extends AbstractLongSet {
        @Override
        public LongIterator iterator() {
            return new KeyIterator();
        }

        @Override
        public void forEach(java.util.function.LongConsumer consumer) {
            if (containsNullKey) consumer.accept(key[n]);
            for (int pos = n; pos-- != 0;) if (key[pos] != 0) consumer.accept(key[pos]);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean contains(long k) {
            return containsKey(k);
        }

        @Override
        public boolean remove(long k) {
            int oldSize = size;
            Long2ObjectOpenHashMap.this.remove(k);
            return size != oldSize;
        }

        @Override
        public void clear() {
            Long2ObjectOpenHashMap.this.clear();
        }
    }

    @Override
    public LongSet keySet() {
        if (keys == null) keys = new KeySet();
        return keys;
    }

    @Override
    public ObjectCollection<V> values() {
        if (values == null) values = new AbstractObjectCollection<V>() {
            @Override
            public ObjectIterator<V> iterator() {
                return new ValueIterator();
            }

            @Override
            public void forEach(Consumer<? super V> consumer) {
                if (containsNullKey) consumer.accept(value[n]);
                for (int pos = n; pos-- != 0;) if (key[pos] != 0) consumer.accept(value[pos]);
            }

            @Override
            public int size() {
                return size;
            }

            @Override
            public boolean contains(Object v) {
                return containsValue(v);
            }

            @Override
            public void clear() {
                Long2ObjectOpenHashMap.this.clear();
            }
        };
        return values;
    }

    public boolean trim() {
        return trim(size);
    }

    public boolean trim(int wanted) {
        int l = HashCommon.nextPowerOfTwo((int) Math.ceil(wanted / f));
        if (l >= n || size > HashCommon.maxFill(l, f)) return true;
        rehash(l);
        return true;
    }

    @SuppressWarnings("unchecked")
    protected void rehash(int newN) {
        long[] key = this.key;
        V[] value = this.value;
        int mask = newN - 1;
        long[] newKey = new long[newN + 1];
        V[] newValue = (V[]) new Object[newN + 1];
        int i = n, pos;
        for (int j = realSize(); j-- != 0;) {
            while (key[--i] == 0) {
            }
            if (newKey[pos = (int) HashCommon.mix(key[i]) & mask] != 0) while (newKey[pos = (pos + 1) & mask] != 0) {
            }
            newKey[pos] = key[i];
            newValue[pos] = value[i];
        }
        newValue[newN] = value[n];
        n = newN;
        this.mask = mask;
        maxFill = HashCommon.maxFill(n, f);
        this.key = newKey;
        this.value = newValue;
    }

    @Override
    public Long2ObjectOpenHashMap<V> clone() {
        Long2ObjectOpenHashMap<V> c = new Long2ObjectOpenHashMap<>(0, f);
        c.key = key.clone();
        c.value = value.clone();
        c.mask = mask;
        c.n = n;
        c.maxFill = maxFill;
        c.size = size;
        c.containsNullKey = containsNullKey;
        c.defRetValue = defRetValue;
        return c;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int j = realSize(), i = 0, t = 0; j-- != 0;) {
            while (key[i] == 0) i++;
            t = HashCommon.long2int(key[i]);
            if (this != value[i]) t ^= value[i] == null ? 0 : value[i].hashCode();
            h += t;
            i++;
        }
        if (containsNullKey) h += value[n] == null ? 0 : value[n].hashCode();
        return h;
    }
}
