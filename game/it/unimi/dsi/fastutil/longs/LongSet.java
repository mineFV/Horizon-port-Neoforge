package it.unimi.dsi.fastutil.longs;

/** The stand-in for fastutil's LongSet. */
public interface LongSet extends LongCollection, java.util.Set<Long> {
    @Override
    LongIterator iterator();

    boolean remove(long k);

    @Override
    default boolean remove(Object o) {
        return LongCollection.super.remove(o);
    }

    @Override
    default boolean add(Long o) {
        return LongCollection.super.add(o);
    }

    @Override
    default boolean contains(Object o) {
        return LongCollection.super.contains(o);
    }

    @Override
    default boolean rem(long k) {
        return remove(k);
    }
}
