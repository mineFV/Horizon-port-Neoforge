package it.unimi.dsi.fastutil.longs;

/** The stand-in for fastutil's LongCollection: a collection of longs, its boxed methods unboxing them. */
public interface LongCollection extends java.util.Collection<Long>, LongIterable {
    @Override
    LongIterator iterator();

    @Override
    default LongIterator longIterator() {
        return iterator();
    }

    boolean add(long key);

    boolean contains(long key);

    boolean rem(long key);

    @Override
    default boolean add(Long key) {
        return add(key.longValue());
    }

    @Override
    default boolean contains(Object key) {
        if (key == null) return false;
        return contains(((Long) key).longValue());
    }

    @Override
    default boolean remove(Object key) {
        if (key == null) return false;
        return rem(((Long) key).longValue());
    }

    long[] toLongArray();

    default long[] toLongArray(long[] a) {
        long[] out = a != null && a.length >= size() ? a : new long[size()];
        LongIterator i = iterator();
        for (int k = 0; i.hasNext(); k++) out[k] = i.nextLong();
        return out;
    }

    boolean addAll(LongCollection c);

    boolean containsAll(LongCollection c);

    boolean removeAll(LongCollection c);

    boolean retainAll(LongCollection c);
}
