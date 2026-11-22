package it.unimi.dsi.fastutil.longs;

/** The stand-in for fastutil's LongIterator: an iterator of longs, boxing them for next(). */
public interface LongIterator extends java.util.PrimitiveIterator.OfLong {
    @Override
    long nextLong();

    @Override
    default Long next() {
        return nextLong();
    }

    default void forEachRemaining(LongConsumer action) {
        java.util.Objects.requireNonNull(action);
        while (hasNext()) action.accept(nextLong());
    }

    @Override
    default void forEachRemaining(java.util.function.Consumer<? super Long> action) {
        java.util.Objects.requireNonNull(action);
        while (hasNext()) action.accept(nextLong());
    }

    default int skip(int n) {
        if (n < 0) throw new IllegalArgumentException("Argument must be nonnegative: " + n);
        int i = n;
        while (i-- != 0 && hasNext()) nextLong();
        return n - i - 1;
    }
}
