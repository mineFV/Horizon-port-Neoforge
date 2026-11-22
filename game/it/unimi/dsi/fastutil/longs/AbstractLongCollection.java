package it.unimi.dsi.fastutil.longs;

/** The stand-in for fastutil's AbstractLongCollection: what a collection of longs does from its iterator, and its text {a, b}. */
public abstract class AbstractLongCollection extends java.util.AbstractCollection<Long> implements LongCollection {
    protected AbstractLongCollection() {}

    @Override
    public abstract LongIterator iterator();

    @Override
    public boolean add(long k) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(long k) {
        LongIterator i = iterator();
        while (i.hasNext()) if (k == i.nextLong()) return true;
        return false;
    }

    @Override
    public boolean rem(long k) {
        LongIterator i = iterator();
        while (i.hasNext()) {
            if (k == i.nextLong()) {
                i.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean add(Long key) {
        return LongCollection.super.add(key);
    }

    @Override
    public boolean contains(Object key) {
        return LongCollection.super.contains(key);
    }

    @Override
    public boolean remove(Object key) {
        return LongCollection.super.remove(key);
    }

    @Override
    public long[] toLongArray() {
        int size = size();
        if (size == 0) return new long[0];
        long[] a = new long[size];
        LongIterator i = iterator();
        for (int k = 0; k < size; k++) a[k] = i.nextLong();
        return a;
    }

    @Override
    public long[] toLongArray(long[] a) {
        return LongCollection.super.toLongArray(a);
    }

    public final void forEach(LongConsumer action) {
        forEach((java.util.function.LongConsumer) action);
    }

    @Override
    public boolean addAll(LongCollection c) {
        boolean retVal = false;
        for (LongIterator i = c.iterator(); i.hasNext();) if (add(i.nextLong())) retVal = true;
        return retVal;
    }

    @Override
    public boolean addAll(java.util.Collection<? extends Long> c) {
        if (c instanceof LongCollection l) return addAll(l);
        return super.addAll(c);
    }

    @Override
    public boolean containsAll(LongCollection c) {
        for (LongIterator i = c.iterator(); i.hasNext();) if (!contains(i.nextLong())) return false;
        return true;
    }

    @Override
    public boolean containsAll(java.util.Collection<?> c) {
        if (c instanceof LongCollection l) return containsAll(l);
        return super.containsAll(c);
    }

    @Override
    public boolean removeAll(LongCollection c) {
        boolean retVal = false;
        for (LongIterator i = c.iterator(); i.hasNext();) if (rem(i.nextLong())) retVal = true;
        return retVal;
    }

    @Override
    public boolean removeAll(java.util.Collection<?> c) {
        if (c instanceof LongCollection l) return removeAll(l);
        return super.removeAll(c);
    }

    @Override
    public boolean retainAll(LongCollection c) {
        boolean retVal = false;
        for (LongIterator i = iterator(); i.hasNext();) {
            if (!c.contains(i.nextLong())) {
                i.remove();
                retVal = true;
            }
        }
        return retVal;
    }

    @Override
    public boolean retainAll(java.util.Collection<?> c) {
        if (c instanceof LongCollection l) return retainAll(l);
        return super.retainAll(c);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("{");
        LongIterator i = iterator();
        boolean first = true;
        while (i.hasNext()) {
            if (first) first = false;
            else s.append(", ");
            s.append(String.valueOf(i.nextLong()));
        }
        return s.append('}').toString();
    }
}
