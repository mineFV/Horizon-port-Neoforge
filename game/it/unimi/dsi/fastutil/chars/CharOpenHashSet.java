package it.unimi.dsi.fastutil.chars;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;

/**
 * The stand-in for fastutil's CharOpenHashSet: ObjectOpenHashSet's open hash table for chars, '\0' kept apart as null
 * is there; gone through from its last slot to its first, '\0' first, removals while going through included
 * (observed against fastutil, `standins`).
 */
public class CharOpenHashSet extends AbstractCharSet implements java.io.Serializable, Cloneable, Hash {
    protected transient char[] key;
    protected transient int mask;
    protected transient boolean containsNull;
    protected transient int n;
    protected transient int maxFill;
    protected final transient int minN;
    protected int size;
    protected final float f;

    public CharOpenHashSet(int expected, float f) {
        if (f <= 0 || f >= 1) throw new IllegalArgumentException("Load factor must be greater than 0 and smaller than 1");
        if (expected < 0) throw new IllegalArgumentException("The expected number of elements must be nonnegative");
        this.f = f;
        minN = n = HashCommon.arraySize(expected, f);
        mask = n - 1;
        maxFill = HashCommon.maxFill(n, f);
        key = new char[n + 1];
    }

    public CharOpenHashSet(int expected) {
        this(expected, DEFAULT_LOAD_FACTOR);
    }

    public CharOpenHashSet() {
        this(DEFAULT_INITIAL_SIZE, DEFAULT_LOAD_FACTOR);
    }

    public CharOpenHashSet(char[] a, int offset, int length, float f) {
        this(length < 0 ? 0 : length, f);
        if (offset < 0) throw new ArrayIndexOutOfBoundsException("Offset (" + offset + ") is negative");
        if (length < 0) throw new IllegalArgumentException("Length (" + length + ") is negative");
        if (offset + length > a.length) throw new ArrayIndexOutOfBoundsException("Last index (" + (offset + length) + ") is greater than array length (" + a.length + ")");
        for (int i = 0; i < length; i++) add(a[offset + i]);
    }

    public CharOpenHashSet(char[] a, int offset, int length) {
        this(a, offset, length, DEFAULT_LOAD_FACTOR);
    }

    public CharOpenHashSet(char[] a, float f) {
        this(a, 0, a.length, f);
    }

    public CharOpenHashSet(char[] a) {
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

    /** Observed: the table sized for them first, as ObjectOpenHashSet's. */
    @Override
    public boolean addAll(java.util.Collection<? extends Character> c) {
        if (f <= .5) ensureCapacity(c.size());
        else tryCapacity(size() + c.size());
        return super.addAll(c);
    }

    @Override
    public boolean add(char k) {
        int pos;
        if (k == 0) {
            if (containsNull) return false;
            containsNull = true;
        } else {
            char curr;
            if ((curr = key[pos = HashCommon.mix(k) & mask]) != 0) {
                if (curr == k) return false;
                while ((curr = key[pos = (pos + 1) & mask]) != 0) if (curr == k) return false;
            }
            key[pos] = k;
        }
        if (size++ >= maxFill) rehash(HashCommon.arraySize(size + 1, f));
        return true;
    }

    protected final void shiftKeys(int pos) {
        int last, slot;
        char curr;
        for (;;) {
            pos = ((last = pos) + 1) & mask;
            for (;;) {
                if ((curr = key[pos]) == 0) {
                    key[last] = 0;
                    return;
                }
                slot = HashCommon.mix(curr) & mask;
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
        key[n] = 0;
        size--;
        if (n > minN && size < maxFill / 4 && n > DEFAULT_INITIAL_SIZE) rehash(n / 2);
        return true;
    }

    @Override
    public boolean remove(char k) {
        if (k == 0) return containsNull && removeNullEntry();
        char curr;
        int pos;
        if ((curr = key[pos = HashCommon.mix(k) & mask]) == 0) return false;
        if (k == curr) return removeEntry(pos);
        while (true) {
            if ((curr = key[pos = (pos + 1) & mask]) == 0) return false;
            if (k == curr) return removeEntry(pos);
        }
    }

    @Override
    public boolean rem(char k) {
        return remove(k);
    }

    @Override
    public boolean contains(char k) {
        if (k == 0) return containsNull;
        char curr;
        int pos;
        if ((curr = key[pos = HashCommon.mix(k) & mask]) == 0) return false;
        if (k == curr) return true;
        while (true) {
            if ((curr = key[pos = (pos + 1) & mask]) == 0) return false;
            if (k == curr) return true;
        }
    }

    @Override
    public void clear() {
        if (size == 0) return;
        size = 0;
        containsNull = false;
        java.util.Arrays.fill(key, (char) 0);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /** Its chars from its last slot to its first, '\0' first. */
    @Override
    public CharIterator iterator() {
        return new SetIterator();
    }

    /** Its chars from its last slot to its first, '\0' first; a removal behind it is gone through as fastutil's. */
    private final class SetIterator implements CharIterator {
        int pos = n;
        int last = -1;
        int c = size;
        boolean mustReturnNull = containsNull;
        it.unimi.dsi.fastutil.chars.HorizonCharList wrapped;

        @Override
        public boolean hasNext() {
            return c != 0;
        }

        @Override
        public char nextChar() {
            if (!hasNext()) throw new java.util.NoSuchElementException();
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
                if (key[pos] != 0) return key[last = pos];
            }
        }

        private void shiftKeys(int pos) {
            int last, slot;
            char curr;
            for (;;) {
                pos = ((last = pos) + 1) & mask;
                for (;;) {
                    if ((curr = key[pos]) == 0) {
                        key[last] = 0;
                        return;
                    }
                    slot = HashCommon.mix(curr) & mask;
                    if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                    pos = (pos + 1) & mask;
                }
                if (pos < last) {
                    if (wrapped == null) wrapped = new it.unimi.dsi.fastutil.chars.HorizonCharList();
                    wrapped.add(key[pos]);
                }
                key[last] = curr;
            }
        }

        @Override
        public void remove() {
            if (last == -1) throw new IllegalStateException();
            if (last == n) {
                containsNull = false;
                key[n] = 0;
            } else if (pos >= 0) {
                shiftKeys(last);
            } else {
                CharOpenHashSet.this.remove(wrapped.get(-pos - 1));
                last = -1;
                return;
            }
            size--;
            last = -1;
        }
    }

    /** A copy: its own table, of its length. */
    @Override
    public CharOpenHashSet clone() {
        CharOpenHashSet c = new CharOpenHashSet(0, f);
        c.key = key.clone();
        c.mask = mask;
        c.n = n;
        c.maxFill = maxFill;
        c.size = size;
        c.containsNull = containsNull;
        return c;
    }

    protected void rehash(int newN) {
        char[] key = this.key;
        int mask = newN - 1;
        char[] newKey = new char[newN + 1];
        int i = n, pos;
        for (int j = realSize(); j-- != 0;) {
            while (key[--i] == 0) {
            }
            if (newKey[pos = HashCommon.mix(key[i]) & mask] != 0) while (newKey[pos = (pos + 1) & mask] != 0) {
            }
            newKey[pos] = key[i];
        }
        n = newN;
        this.mask = mask;
        maxFill = HashCommon.maxFill(n, f);
        this.key = newKey;
    }

    /** Observed: the sum of its chars. */
    @Override
    public int hashCode() {
        int h = 0;
        for (int j = realSize(), i = 0; j-- != 0;) {
            while (key[i] == 0) i++;
            h += key[i];
            i++;
        }
        return h;
    }
}
