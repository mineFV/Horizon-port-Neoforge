package it.unimi.dsi.fastutil.longs;

/** The stand-in for fastutil's LongObjectPair: a long and a value (an entry of a map of longs is one). */
public interface LongObjectPair<V> extends it.unimi.dsi.fastutil.Pair<Long, V> {
    long leftLong();

    @Override
    default Long left() {
        return leftLong();
    }

    default LongObjectPair<V> left(long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    default LongObjectPair<V> left(Long l) {
        return left(l.longValue());
    }

    default long firstLong() {
        return leftLong();
    }

    @Override
    default Long first() {
        return firstLong();
    }

    default LongObjectPair<V> first(long l) {
        return left(l);
    }

    @Override
    default LongObjectPair<V> first(Long l) {
        return first(l.longValue());
    }

    default long keyLong() {
        return firstLong();
    }

    @Override
    default Long key() {
        return keyLong();
    }

    default LongObjectPair<V> key(long l) {
        return left(l);
    }

    @Override
    default LongObjectPair<V> key(Long l) {
        return key(l.longValue());
    }
}
