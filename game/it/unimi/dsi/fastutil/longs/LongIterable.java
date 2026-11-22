package it.unimi.dsi.fastutil.longs;

/** The stand-in for fastutil's LongIterable. */
public interface LongIterable extends Iterable<Long> {
    @Override
    LongIterator iterator();

    default LongIterator longIterator() {
        return iterator();
    }

    default void forEach(java.util.function.LongConsumer action) {
        java.util.Objects.requireNonNull(action);
        LongIterator i = iterator();
        while (i.hasNext()) action.accept(i.nextLong());
    }

    default void forEach(LongConsumer action) {
        forEach((java.util.function.LongConsumer) action);
    }

    @Override
    default void forEach(java.util.function.Consumer<? super Long> action) {
        java.util.Objects.requireNonNull(action);
        LongIterator i = iterator();
        while (i.hasNext()) action.accept(i.nextLong());
    }
}
