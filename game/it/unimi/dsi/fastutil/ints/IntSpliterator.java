package it.unimi.dsi.fastutil.ints;

import java.util.function.Consumer;

/** The stand-in for fastutil's IntSpliterator (getComparator is Spliterator's: it refuses, as fastutil's does). */
public interface IntSpliterator extends java.util.Spliterator.OfInt {
    @Override
    default boolean tryAdvance(Consumer<? super Integer> action) {
        return tryAdvance(action instanceof java.util.function.IntConsumer ? (java.util.function.IntConsumer) action : (java.util.function.IntConsumer) action::accept);
    }

    @Override
    default void forEachRemaining(Consumer<? super Integer> action) {
        forEachRemaining(action instanceof java.util.function.IntConsumer ? (java.util.function.IntConsumer) action : (java.util.function.IntConsumer) action::accept);
    }

    /** Skips n elements (or as many as there are): how many were skipped. */
    default long skip(long n) {
        if (n < 0) throw new IllegalArgumentException("Argument must be nonnegative: " + n);
        long i = n;
        while (i-- != 0 && tryAdvance((int unused) -> {})) {
        }
        return n - i - 1;
    }

    @Override
    IntSpliterator trySplit();
}
