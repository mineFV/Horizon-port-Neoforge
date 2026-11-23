package it.unimi.dsi.fastutil.ints;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * The stand-in for fastutil's IntArrayList: ints in an array that grows by half (a list made without a capacity starts
 * empty and takes 10 at its first element), its errors' texts, its list iterator ($1) and spliterator (late-binding,
 * splitting in halves) as fastutil's, observed against fastutil (`standins`). A part of it as a view refuses unless it is
 * the whole list (itself, as fastutil gives it); a subclass's clone refuses.
 */
public class IntArrayList extends AbstractIntList implements RandomAccess, Cloneable, java.io.Serializable {
    public static final int DEFAULT_INITIAL_CAPACITY = 10;

    /** fastutil's largest array (it.unimi.dsi.fastutil.Arrays.MAX_ARRAY_SIZE). */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    protected transient int[] a;
    protected int size;

    protected IntArrayList(int[] a, boolean wrapped) {
        this.a = a;
    }

    public IntArrayList(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("Initial capacity (" + capacity + ") is negative");
        a = capacity == 0 ? IntArrays.EMPTY_ARRAY : new int[capacity];
    }

    public IntArrayList() {
        a = IntArrays.DEFAULT_EMPTY_ARRAY;
    }

    public IntArrayList(Collection<? extends Integer> c) {
        this(c.size());
        if (c instanceof IntCollection ints) {
            IntIterator i = ints.iterator();
            while (size < a.length && i.hasNext()) a[size++] = i.nextInt();
        } else {
            java.util.Iterator<? extends Integer> i = c.iterator();
            while (size < a.length && i.hasNext()) a[size++] = i.next().intValue();
        }
    }

    public IntArrayList(int[] a) {
        this(a, 0, a.length);
    }

    public IntArrayList(int[] a, int offset, int length) {
        this(length);
        System.arraycopy(a, offset, this.a, 0, length);
        size = length;
    }

    /** The backing array itself (its length is the capacity). */
    public int[] elements() {
        return a;
    }

    public static IntArrayList wrap(int[] a, int length) {
        if (length > a.length) throw new IllegalArgumentException("The specified length (" + length + ") is greater than the array size (" + a.length + ")");
        IntArrayList l = new IntArrayList(a, true);
        l.size = length;
        return l;
    }

    public static IntArrayList wrap(int[] a) {
        return wrap(a, a.length);
    }

    public static IntArrayList of() {
        return new IntArrayList();
    }

    public static IntArrayList of(int... init) {
        return wrap(init);
    }

    public void ensureCapacity(int capacity) {
        if (capacity <= a.length || (a == IntArrays.DEFAULT_EMPTY_ARRAY && capacity <= DEFAULT_INITIAL_CAPACITY)) return;
        int[] t = new int[capacity];
        System.arraycopy(a, 0, t, 0, size);
        a = t;
    }

    private void grow(int capacity) {
        if (capacity <= a.length) return;
        if (a != IntArrays.DEFAULT_EMPTY_ARRAY) capacity = (int) Math.max(Math.min((long) a.length + (a.length >> 1), MAX_ARRAY_SIZE), capacity);
        else if (capacity < DEFAULT_INITIAL_CAPACITY) capacity = DEFAULT_INITIAL_CAPACITY;
        int[] t = new int[capacity];
        System.arraycopy(a, 0, t, 0, size);
        a = t;
    }

    @Override
    public void add(int index, int k) {
        ensureIndex(index);
        grow(size + 1);
        if (index != size) System.arraycopy(a, index, a, index + 1, size - index);
        a[index] = k;
        size++;
    }

    @Override
    public boolean add(int k) {
        grow(size + 1);
        a[size++] = k;
        return true;
    }

    @Override
    public int getInt(int index) {
        if (index >= size) throw new IndexOutOfBoundsException("Index (" + index + ") is greater than or equal to list size (" + size + ")");
        return a[index];
    }

    @Override
    public int indexOf(int k) {
        for (int i = 0; i < size; i++) if (k == a[i]) return i;
        return -1;
    }

    @Override
    public int lastIndexOf(int k) {
        for (int i = size; i-- != 0;) if (k == a[i]) return i;
        return -1;
    }

    @Override
    public int removeInt(int index) {
        if (index >= size) throw new IndexOutOfBoundsException("Index (" + index + ") is greater than or equal to list size (" + size + ")");
        int old = a[index];
        size--;
        if (index != size) System.arraycopy(a, index + 1, a, index, size - index);
        return old;
    }

    @Override
    public boolean rem(int k) {
        int index = indexOf(k);
        if (index == -1) return false;
        removeInt(index);
        return true;
    }

    @Override
    public int set(int index, int k) {
        if (index >= size) throw new IndexOutOfBoundsException("Index (" + index + ") is greater than or equal to list size (" + size + ")");
        int old = a[index];
        a[index] = k;
        return old;
    }

    @Override
    public void clear() {
        size = 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void size(int size) {
        if (size > a.length) {
            int[] t = new int[size];
            System.arraycopy(a, 0, t, 0, this.size);
            a = t;
        }
        if (size > this.size) java.util.Arrays.fill(a, this.size, size, 0);
        this.size = size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    public void trim() {
        trim(0);
    }

    public void trim(int n) {
        if (n >= a.length || size == a.length) return;
        int[] t = new int[Math.max(n, size)];
        System.arraycopy(a, 0, t, 0, size);
        a = t;
    }

    @Override
    public IntList subList(int from, int to) {
        if (from == 0 && to == size()) return this;
        return super.subList(from, to);
    }

    @Override
    public void forEach(java.util.function.IntConsumer action) {
        for (int i = 0; i < size; ++i) action.accept(a[i]);
    }

    /** The collection's ints inserted at the index; another IntArrayList's array is read as it is once room is made. */
    @Override
    public boolean addAll(int index, IntCollection c) {
        ensureIndex(index);
        int n = c.size();
        if (n == 0) return false;
        grow(size + n);
        System.arraycopy(a, index, a, index + n, size - index);
        if (c instanceof IntArrayList l) {
            System.arraycopy(l.a, 0, a, index, n);
            size += n;
            return true;
        }
        IntIterator i = c.iterator();
        size += n;
        while (n-- != 0) a[index++] = i.nextInt();
        return true;
    }

    @Override
    public boolean removeAll(IntCollection c) {
        int[] a = this.a;
        int j = 0;
        for (int i = 0; i < size; i++) if (!c.contains(a[i])) a[j++] = a[i];
        boolean modified = size != j;
        size = j;
        return modified;
    }

    @Override
    public int[] toArray(int[] a) {
        if (a == null || a.length < size) a = java.util.Arrays.copyOf(a, size);
        System.arraycopy(this.a, 0, a, 0, size);
        return a;
    }

    @Override
    public IntListIterator listIterator(final int index) {
        ensureIndex(index);
        return new IntListIterator() {
            int pos = index, last = -1;

            @Override
            public boolean hasNext() {
                return pos < size;
            }

            @Override
            public boolean hasPrevious() {
                return pos > 0;
            }

            @Override
            public int nextInt() {
                if (!hasNext()) throw new NoSuchElementException();
                return a[last = pos++];
            }

            @Override
            public int previousInt() {
                if (!hasPrevious()) throw new NoSuchElementException();
                return a[last = --pos];
            }

            @Override
            public int nextIndex() {
                return pos;
            }

            @Override
            public int previousIndex() {
                return pos - 1;
            }

            @Override
            public void add(int k) {
                IntArrayList.this.add(pos++, k);
                last = -1;
            }

            @Override
            public void set(int k) {
                if (last == -1) throw new IllegalStateException();
                IntArrayList.this.set(last, k);
            }

            @Override
            public void remove() {
                if (last == -1) throw new IllegalStateException();
                IntArrayList.this.removeInt(last);
                if (last < pos) pos--;
                last = -1;
            }

            @Override
            public void forEachRemaining(java.util.function.IntConsumer action) {
                while (pos < size) action.accept(a[last = pos++]);
            }

            /**
             * As fastutil 8.5.12's (observed): how far it may go back is measured from the end, so that at the end it
             * goes back to the start and says 0, and from near the start it goes before it.
             */
            @Override
            public int back(int n) {
                if (n < 0) throw new IllegalArgumentException("Argument must be nonnegative: " + n);
                int remaining = size - pos;
                if (n < remaining) {
                    pos -= n;
                } else {
                    n = remaining;
                    pos = 0;
                }
                last = pos;
                return n;
            }

            @Override
            public int skip(int n) {
                if (n < 0) throw new IllegalArgumentException("Argument must be nonnegative: " + n);
                int remaining = size - pos;
                if (n < remaining) {
                    pos += n;
                } else {
                    n = remaining;
                    pos = size;
                }
                last = pos - 1;
                return n;
            }
        };
    }

    /** The list's ints from a position up to its size when first used (or the bound a split gave it). */
    final class Spliterator implements IntSpliterator {
        boolean hasSplit = false;
        int pos, max;

        public Spliterator() {
            this(0, IntArrayList.this.size, false);
        }

        private Spliterator(int pos, int max, boolean hasSplit) {
            this.pos = pos;
            this.max = max;
            this.hasSplit = hasSplit;
        }

        private int getWorkingMax() {
            return hasSplit ? max : IntArrayList.this.size;
        }

        @Override
        public int characteristics() {
            return java.util.Spliterator.ORDERED | java.util.Spliterator.SIZED | java.util.Spliterator.SUBSIZED | java.util.Spliterator.NONNULL;
        }

        @Override
        public long estimateSize() {
            return getWorkingMax() - pos;
        }

        @Override
        public boolean tryAdvance(java.util.function.IntConsumer action) {
            if (pos >= getWorkingMax()) return false;
            action.accept(a[pos++]);
            return true;
        }

        @Override
        public void forEachRemaining(java.util.function.IntConsumer action) {
            for (int max = getWorkingMax(); pos < max; ++pos) action.accept(a[pos]);
        }

        @Override
        public long skip(long n) {
            if (n < 0) throw new IllegalArgumentException("Argument must be nonnegative: " + n);
            int max = getWorkingMax();
            if (pos >= max) return 0;
            int remaining = max - pos;
            if (n < remaining) {
                pos = Math.toIntExact(pos + n);
                return n;
            }
            n = remaining;
            pos = max;
            return n;
        }

        @Override
        public IntSpliterator trySplit() {
            int max = getWorkingMax();
            int retLen = (max - pos) >> 1;
            if (retLen <= 1) return null;
            this.max = max;
            int myNewPos = pos + retLen;
            int oldPos = pos;
            this.pos = myNewPos;
            this.hasSplit = true;
            return new Spliterator(oldPos, myNewPos, true);
        }
    }

    @Override
    public IntSpliterator spliterator() {
        return new Spliterator();
    }

    /** Sorted in place by the comparator, stably (null: ascending); see IntList.sort. */
    @Override
    public void sort(Comparator<? super Integer> comparator) {
        HorizonInts.sort(a, 0, size, comparator);
    }

    /** A copy whose array is as long as the list (the shared empty array when it is empty). */
    @Override
    public IntArrayList clone() {
        if (getClass() != IntArrayList.class) throw new UnsupportedOperationException("cloning a subclass of fastutil's IntArrayList isn't modelled (horizon-port-neoforge)");
        IntArrayList cloned = new IntArrayList(size == 0 ? IntArrays.EMPTY_ARRAY : java.util.Arrays.copyOf(a, size), false);
        cloned.size = size;
        return cloned;
    }

    public boolean equals(IntArrayList l) {
        if (l == this) return true;
        int s = size();
        if (s != l.size()) return false;
        int[] a1 = a, a2 = l.a;
        if (a1 == a2) return true;
        while (s-- != 0) if (a1[s] != a2[s]) return false;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof List)) return false;
        if (o instanceof IntArrayList l) return equals(l);
        return super.equals(o);
    }

    public int compareTo(IntArrayList l) {
        int s1 = size(), s2 = l.size();
        int[] a1 = a, a2 = l.a;
        if (a1 == a2 && s1 == s2) return 0;
        int r, i;
        for (i = 0; i < s1 && i < s2; i++) if ((r = Integer.compare(a1[i], a2[i])) != 0) return r;
        return i < s2 ? -1 : (i < s1 ? 1 : 0);
    }

    @Override
    public int compareTo(List<? extends Integer> l) {
        if (l instanceof IntArrayList ints) return compareTo(ints);
        return super.compareTo(l);
    }
}
