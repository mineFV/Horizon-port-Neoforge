package it.unimi.dsi.fastutil.ints;

import java.util.Collection;

/**
 * The stand-in for fastutil's AbstractIntCollection: what a collection of ints does from its iterator, an IntCollection
 * given to its bulk methods taken int by int, any other collection as Java's AbstractCollection takes it.
 */
public abstract class AbstractIntCollection extends java.util.AbstractCollection<Integer> implements IntCollection {
    protected AbstractIntCollection() {}

    @Override
    public abstract IntIterator iterator();

    @Override
    public boolean add(int k) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(int k) {
        IntIterator iterator = iterator();
        while (iterator.hasNext()) if (k == iterator.nextInt()) return true;
        return false;
    }

    @Override
    public boolean rem(int k) {
        IntIterator iterator = iterator();
        while (iterator.hasNext()) {
            if (k == iterator.nextInt()) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean add(Integer key) {
        return IntCollection.super.add(key);
    }

    @Override
    public boolean contains(Object k) {
        return IntCollection.super.contains(k);
    }

    @Override
    public boolean remove(Object k) {
        return IntCollection.super.remove(k);
    }

    @Override
    public int[] toArray(int[] a) {
        int size = size();
        if (a == null) a = new int[size];
        else if (a.length < size) a = java.util.Arrays.copyOf(a, size);
        IntIterator i = iterator();
        for (int j = 0; j < size; j++) a[j] = i.nextInt();
        return a;
    }

    @Override
    public int[] toIntArray() {
        int size = size();
        if (size == 0) return IntArrays.EMPTY_ARRAY;
        int[] a = new int[size];
        IntIterator i = iterator();
        for (int j = 0; j < size; j++) a[j] = i.nextInt();
        return a;
    }

    @Override
    public int[] toIntArray(int[] a) {
        return toArray(a);
    }

    @Override
    public boolean addAll(IntCollection c) {
        boolean retVal = false;
        for (IntIterator i = c.iterator(); i.hasNext();) if (add(i.nextInt())) retVal = true;
        return retVal;
    }

    @Override
    public boolean addAll(Collection<? extends Integer> c) {
        if (c instanceof IntCollection ints) return addAll(ints);
        return super.addAll(c);
    }

    @Override
    public boolean containsAll(IntCollection c) {
        for (IntIterator i = c.iterator(); i.hasNext();) if (!contains(i.nextInt())) return false;
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c instanceof IntCollection ints) return containsAll(ints);
        return super.containsAll(c);
    }

    @Override
    public boolean removeAll(IntCollection c) {
        boolean retVal = false;
        for (IntIterator i = c.iterator(); i.hasNext();) if (rem(i.nextInt())) retVal = true;
        return retVal;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c instanceof IntCollection ints) return removeAll(ints);
        return super.removeAll(c);
    }

    @Override
    public boolean retainAll(IntCollection c) {
        boolean retVal = false;
        for (IntIterator i = iterator(); i.hasNext();) {
            if (!c.contains(i.nextInt())) {
                i.remove();
                retVal = true;
            }
        }
        return retVal;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c instanceof IntCollection ints) return retainAll(ints);
        return super.retainAll(c);
    }

    /** {a, b}: a collection's text (a list's is in brackets). */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        IntIterator i = iterator();
        int n = size();
        boolean first = true;
        s.append("{");
        while (n-- != 0) {
            if (first) first = false;
            else s.append(", ");
            s.append(String.valueOf(i.nextInt()));
        }
        s.append("}");
        return s.toString();
    }
}
