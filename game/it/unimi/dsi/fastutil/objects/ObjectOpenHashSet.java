package it.unimi.dsi.fastutil.objects;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * The stand-in for fastutil's ObjectOpenHashSet: an open hash table as fastutil's (each element at its mixed hash,
 * or the next free slot after it; a removal shifts the following ones back; a table twice as large past its load
 * factor, half as large below a quarter of it), gone through from its last slot to its first, null first. Its
 * order, observed against fastutil's own (`standins`), is the game's.
 */
public class ObjectOpenHashSet<K> extends AbstractObjectSet<K> implements java.io.Serializable, Cloneable, Hash {
    protected transient K[] key;
    protected transient int mask;
    protected transient boolean containsNull;
    protected transient int n;
    protected transient int maxFill;
    protected final transient int minN;
    protected int size;
    protected final float f;

    @SuppressWarnings("unchecked")
    public ObjectOpenHashSet(int expected, float f) {
        if (f <= 0 || f >= 1) throw new IllegalArgumentException("Load factor must be greater than 0 and smaller than 1");
        if (expected < 0) throw new IllegalArgumentException("The expected number of elements must be nonnegative");
        this.f = f;
        minN = n = HashCommon.arraySize(expected, f);
        mask = n - 1;
        maxFill = HashCommon.maxFill(n, f);
        key = (K[]) new Object[n + 1];
    }

    public ObjectOpenHashSet(int expected) {
        this(expected, DEFAULT_LOAD_FACTOR);
    }

    public ObjectOpenHashSet() {
        this(DEFAULT_INITIAL_SIZE, DEFAULT_LOAD_FACTOR);
    }

    public ObjectOpenHashSet(Collection<? extends K> c, float f) {
        this(c.size(), f);
        addAll(c);
    }

    public ObjectOpenHashSet(Collection<? extends K> c) {
        this(c, DEFAULT_LOAD_FACTOR);
    }

    public ObjectOpenHashSet(ObjectCollection<? extends K> c, float f) {
        this(c.size(), f);
        addAll(c);
    }

    public ObjectOpenHashSet(ObjectCollection<? extends K> c) {
        this(c, DEFAULT_LOAD_FACTOR);
    }

    public ObjectOpenHashSet(Iterator<? extends K> i, float f) {
        this(DEFAULT_INITIAL_SIZE, f);
        while (i.hasNext()) add(i.next());
    }

    public ObjectOpenHashSet(Iterator<? extends K> i) {
        this(i, DEFAULT_LOAD_FACTOR);
    }

    public ObjectOpenHashSet(K[] a, int offset, int length, float f) {
        this(length < 0 ? 0 : length, f);
        if (offset < 0) throw new ArrayIndexOutOfBoundsException("Offset (" + offset + ") is negative");
        if (length < 0) throw new IllegalArgumentException("Length (" + length + ") is negative");
        if (offset + length > a.length) throw new ArrayIndexOutOfBoundsException("Last index (" + (offset + length) + ") is greater than array length (" + a.length + ")");
        for (int i = 0; i < length; i++) add(a[offset + i]);
    }

    public ObjectOpenHashSet(K[] a, int offset, int length) {
        this(a, offset, length, DEFAULT_LOAD_FACTOR);
    }

    public ObjectOpenHashSet(K[] a, float f) {
        this(a, 0, a.length, f);
    }

    public ObjectOpenHashSet(K[] a) {
        this(a, DEFAULT_LOAD_FACTOR);
    }

    private int realSize() {
        return containsNull ? size - 1 : size;
    }

    public void ensureCapacity(int capacity) {
        int needed = HashCommon.arraySize(capacity, f);
        if (needed > n) rehash(needed);
    }

    private void tryCapacity(long capacity) {
        int needed = (int) Math.min(1 << 30, Math.max(2, HashCommon.nextPowerOfTwo((long) Math.ceil(capacity / f))));
        if (needed > n) rehash(needed);
    }

    @Override
    public boolean addAll(Collection<? extends K> c) {
        if (f <= .5) ensureCapacity(c.size());
        else tryCapacity(size() + c.size());
        return super.addAll(c);
    }

    @Override
    public boolean add(K k) {
        int pos;
        if (k == null) {
            if (containsNull) return false;
            containsNull = true;
        } else {
            K curr;
            if ((curr = key[pos = HashCommon.mix(k.hashCode()) & mask]) != null) {
                if (curr.equals(k)) return false;
                while ((curr = key[pos = (pos + 1) & mask]) != null) if (curr.equals(k)) return false;
            }
            key[pos] = k;
        }
        if (size++ >= maxFill) rehash(HashCommon.arraySize(size + 1, f));
        return true;
    }

    /** The element equal to k already in it, else k added. */
    public K addOrGet(K k) {
        int pos;
        if (k == null) {
            if (containsNull) return key[n];
            containsNull = true;
        } else {
            K curr;
            if ((curr = key[pos = HashCommon.mix(k.hashCode()) & mask]) != null) {
                if (curr.equals(k)) return curr;
                while ((curr = key[pos = (pos + 1) & mask]) != null) if (curr.equals(k)) return curr;
            }
            key[pos] = k;
        }
        if (size++ >= maxFill) rehash(HashCommon.arraySize(size + 1, f));
        return k;
    }

    protected final void shiftKeys(int pos) {
        int last, slot;
        K curr;
        for (;;) {
            pos = ((last = pos) + 1) & mask;
            for (;;) {
                if ((curr = key[pos]) == null) {
                    key[last] = null;
                    return;
                }
                slot = HashCommon.mix(curr.hashCode()) & mask;
                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                pos = (pos + 1) & mask;
            }
            key[last] = curr;
        }
    }

    private boolean removeEntry(int pos) {
        size--;
        shiftKeys(pos);
        if (n > minN && size < maxFill / 4 && n > DEFAULT_INITIAL_SIZE) rehash(n / 2);
        return true;
    }

    private boolean removeNullEntry() {
        containsNull = false;
        key[n] = null;
        size--;
        if (n > minN && size < maxFill / 4 && n > DEFAULT_INITIAL_SIZE) rehash(n / 2);
        return true;
    }

    @Override
    public boolean remove(Object k) {
        if (k == null) return containsNull && removeNullEntry();
        K curr;
        int pos;
        if ((curr = key[pos = HashCommon.mix(k.hashCode()) & mask]) == null) return false;
        if (k.equals(curr)) return removeEntry(pos);
        while (true) {
            if ((curr = key[pos = (pos + 1) & mask]) == null) return false;
            if (k.equals(curr)) return removeEntry(pos);
        }
    }

    @Override
    public boolean contains(Object k) {
        if (k == null) return containsNull;
        K curr;
        int pos;
        if ((curr = key[pos = HashCommon.mix(k.hashCode()) & mask]) == null) return false;
        if (k.equals(curr)) return true;
        while (true) {
            if ((curr = key[pos = (pos + 1) & mask]) == null) return false;
            if (k.equals(curr)) return true;
        }
    }

    /** The element equal to k in it, else null. */
    public K get(Object k) {
        if (k == null) return key[n];
        K curr;
        int pos;
        if ((curr = key[pos = HashCommon.mix(k.hashCode()) & mask]) == null) return null;
        if (k.equals(curr)) return curr;
        while (true) {
            if ((curr = key[pos = (pos + 1) & mask]) == null) return null;
            if (k.equals(curr)) return curr;
        }
    }

    @Override
    public void clear() {
        if (size == 0) return;
        size = 0;
        containsNull = false;
        Arrays.fill(key, null);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /** Its elements from its last slot to its first, null first; a removal behind it is gone through as fastutil's. */
    private final class SetIterator implements ObjectIterator<K> {
        int pos = n;
        int last = -1;
        int c = size;
        boolean mustReturnNull = ObjectOpenHashSet.this.containsNull;
        java.util.ArrayList<K> wrapped;

        @Override
        public boolean hasNext() {
            return c != 0;
        }

        @Override
        public K next() {
            if (!hasNext()) throw new NoSuchElementException();
            c--;
            if (mustReturnNull) {
                mustReturnNull = false;
                last = n;
                return key[n];
            }
            for (;;) {
                if (--pos < 0) {
                    last = Integer.MIN_VALUE;
                    return wrapped.get(-pos - 1);
                }
                if (key[pos] != null) return key[last = pos];
            }
        }

        private void shiftKeys(int pos) {
            int last, slot;
            K curr;
            for (;;) {
                pos = ((last = pos) + 1) & mask;
                for (;;) {
                    if ((curr = key[pos]) == null) {
                        key[last] = null;
                        return;
                    }
                    slot = HashCommon.mix(curr.hashCode()) & mask;
                    if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                    pos = (pos + 1) & mask;
                }
                if (pos < last) {
                    if (wrapped == null) wrapped = new java.util.ArrayList<>(2);
                    wrapped.add(key[pos]);
                }
                key[last] = curr;
            }
        }

        @Override
        public void remove() {
            if (last == -1) throw new IllegalStateException();
            if (last == n) {
                ObjectOpenHashSet.this.containsNull = false;
                key[n] = null;
            } else if (pos >= 0) {
                shiftKeys(last);
            } else {
                ObjectOpenHashSet.this.remove(wrapped.set(-pos - 1, null));
                last = -1;
                return;
            }
            size--;
            last = -1;
        }
    }

    @Override
    public ObjectIterator<K> iterator() {
        return new SetIterator();
    }

    @Override
    public ObjectSpliterator<K> spliterator() {
        return new HorizonObjectSpliterator<>(java.util.Spliterators.spliterator(this, java.util.Spliterator.DISTINCT));
    }

    @Override
    public void forEach(Consumer<? super K> action) {
        if (containsNull) action.accept(key[n]);
        for (int pos = n; pos-- != 0;) if (key[pos] != null) action.accept(key[pos]);
    }

    public boolean trim() {
        return trim(size);
    }

    public boolean trim(int wanted) {
        int l = HashCommon.nextPowerOfTwo((int) Math.ceil(wanted / f));
        if (l >= this.n || size > HashCommon.maxFill(l, f)) return true;
        rehash(l);
        return true;
    }

    @SuppressWarnings("unchecked")
    protected void rehash(int newN) {
        K[] key = this.key;
        int mask = newN - 1;
        K[] newKey = (K[]) new Object[newN + 1];
        int i = n, pos;
        for (int j = realSize(); j-- != 0;) {
            while (key[--i] == null) {
            }
            if (newKey[pos = HashCommon.mix(key[i].hashCode()) & mask] != null) while (newKey[pos = (pos + 1) & mask] != null) {
            }
            newKey[pos] = key[i];
        }
        n = newN;
        this.mask = mask;
        maxFill = HashCommon.maxFill(n, f);
        this.key = newKey;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ObjectOpenHashSet<K> clone() {
        ObjectOpenHashSet<K> c = new ObjectOpenHashSet<>(0, f);
        c.key = key.clone();
        c.mask = mask;
        c.n = n;
        c.maxFill = maxFill;
        c.size = size;
        c.containsNull = containsNull;
        return c;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int j = realSize(), i = 0; j-- != 0;) {
            while (key[i] == null) i++;
            if (this != key[i]) h += key[i].hashCode();
            i++;
        }
        return h;
    }
}
