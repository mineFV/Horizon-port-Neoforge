package it.unimi.dsi.fastutil.ints;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * The stand-in for fastutil's AbstractIntList: a list of ints from getInt and size (changing it refused unless the list
 * does it), its text, equality, hash and order as fastutil's, its iterator an index-based one (not fastutil's class).
 * A part of it as a view (subList) refuses: its indices are checked, then it says it isn't modelled. Another list is
 * gone through with its iterator where fastutil takes its list iterator: the same elements, for a list whose two agree.
 */
public abstract class AbstractIntList extends AbstractIntCollection implements IntList, IntStack {
    protected AbstractIntList() {}

    protected void ensureIndex(int index) {
        if (index < 0) throw new IndexOutOfBoundsException("Index (" + index + ") is negative");
        if (index > size()) throw new IndexOutOfBoundsException("Index (" + index + ") is greater than list size (" + size() + ")");
    }

    protected void ensureRestrictedIndex(int index) {
        if (index < 0) throw new IndexOutOfBoundsException("Index (" + index + ") is negative");
        if (index >= size()) throw new IndexOutOfBoundsException("Index (" + index + ") is greater than or equal to list size (" + size() + ")");
    }

    @Override
    public void add(int index, int k) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(int k) {
        add(size(), k);
        return true;
    }

    @Override
    public int removeInt(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int set(int index, int k) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends Integer> c) {
        if (c instanceof IntCollection ints) return addAll(index, ints);
        ensureIndex(index);
        Iterator<? extends Integer> i = c.iterator();
        boolean retVal = i.hasNext();
        while (i.hasNext()) add(index++, i.next().intValue());
        return retVal;
    }

    @Override
    public boolean addAll(Collection<? extends Integer> c) {
        return addAll(size(), c);
    }

    @Override
    public IntListIterator iterator() {
        return listIterator();
    }

    @Override
    public IntListIterator listIterator() {
        return listIterator(0);
    }

    @Override
    public IntListIterator listIterator(int index) {
        ensureIndex(index);
        return new HorizonInts.ListIterator(this, index);
    }

    @Override
    public boolean contains(int k) {
        return indexOf(k) >= 0;
    }

    @Override
    public int indexOf(int k) {
        IntListIterator i = listIterator();
        while (i.hasNext()) if (k == i.nextInt()) return i.previousIndex();
        return -1;
    }

    @Override
    public int lastIndexOf(int k) {
        IntListIterator i = listIterator(size());
        while (i.hasPrevious()) if (k == i.previousInt()) return i.nextIndex();
        return -1;
    }

    @Override
    public void size(int size) {
        int i = size();
        if (size > i) while (i++ < size) add(0);
        else while (i-- != size) removeInt(i);
    }

    @Override
    public IntList subList(int from, int to) {
        ensureIndex(from);
        ensureIndex(to);
        if (from > to) throw new IndexOutOfBoundsException("Start index (" + from + ") is greater than end index (" + to + ")");
        throw new UnsupportedOperationException("a part of a fastutil list as a view isn't modelled (horizon-port-neoforge)");
    }

    @Override
    public void forEach(java.util.function.IntConsumer action) {
        if (this instanceof java.util.RandomAccess) {
            for (int i = 0, max = size(); i < max; ++i) action.accept(getInt(i));
        } else {
            IntList.super.forEach(action);
        }
    }

    @Override
    public void clear() {
        IntListIterator i = listIterator(0);
        int n = size();
        while (n-- != 0) {
            i.nextInt();
            i.remove();
        }
    }

    /** As a List's: 31 times the hash so far plus each int, from 1. */
    @Override
    public int hashCode() {
        IntIterator i = iterator();
        int h = 1, s = size();
        while (s-- != 0) h = 31 * h + i.nextInt();
        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof List<?> l)) return false;
        int s = size();
        if (s != l.size()) return false;
        if (l instanceof IntList ints) {
            IntListIterator i1 = listIterator(), i2 = ints.listIterator();
            while (s-- != 0) if (i1.nextInt() != i2.nextInt()) return false;
            return true;
        }
        Iterator<?> i1 = listIterator(), i2 = l.iterator();
        while (s-- != 0) if (!java.util.Objects.equals(i1.next(), i2.next())) return false;
        return true;
    }

    /** Element by element, then the shorter first. */
    @Override
    @SuppressWarnings("unchecked")
    public int compareTo(List<? extends Integer> l) {
        if (l == this) return 0;
        if (l instanceof IntList ints) {
            IntListIterator i1 = listIterator(), i2 = ints.listIterator();
            int r;
            while (i1.hasNext() && i2.hasNext()) if ((r = Integer.compare(i1.nextInt(), i2.nextInt())) != 0) return r;
            return i2.hasNext() ? -1 : (i1.hasNext() ? 1 : 0);
        }
        Iterator<? extends Integer> i1 = listIterator(), i2 = l.iterator();
        int r;
        while (i1.hasNext() && i2.hasNext()) if ((r = ((Comparable<? super Integer>) i1.next()).compareTo(i2.next())) != 0) return r;
        return i2.hasNext() ? -1 : (i1.hasNext() ? 1 : 0);
    }

    @Override
    public void push(int o) {
        add(o);
    }

    @Override
    public int popInt() {
        if (isEmpty()) throw new NoSuchElementException();
        return removeInt(size() - 1);
    }

    @Override
    public int topInt() {
        if (isEmpty()) throw new NoSuchElementException();
        return getInt(size() - 1);
    }

    @Override
    public int peekInt(int i) {
        return getInt(size() - 1 - i);
    }

    @Override
    public boolean rem(int k) {
        int index = indexOf(k);
        if (index == -1) return false;
        removeInt(index);
        return true;
    }

    @Override
    public int[] toIntArray() {
        int size = size();
        if (size == 0) return IntArrays.EMPTY_ARRAY;
        int[] ret = new int[size];
        for (int i = 0; i < size; i++) ret[i] = getInt(i);
        return ret;
    }

    @Override
    public int[] toArray(int[] a) {
        int size = size();
        if (a == null) a = new int[size];
        else if (a.length < size) a = java.util.Arrays.copyOf(a, size);
        for (int i = 0; i < size; i++) a[i] = getInt(i);
        return a;
    }

    @Override
    public boolean addAll(int index, IntCollection c) {
        ensureIndex(index);
        IntIterator i = c.iterator();
        boolean retVal = i.hasNext();
        while (i.hasNext()) add(index++, i.nextInt());
        return retVal;
    }

    @Override
    public boolean addAll(IntCollection c) {
        return addAll(size(), c);
    }

    /** [a, b]: a list's text. */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        IntIterator i = iterator();
        int n = size();
        boolean first = true;
        s.append("[");
        while (n-- != 0) {
            if (first) first = false;
            else s.append(", ");
            s.append(String.valueOf(i.nextInt()));
        }
        s.append("]");
        return s.toString();
    }
}
