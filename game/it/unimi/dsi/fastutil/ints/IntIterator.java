package it.unimi.dsi.fastutil.ints;

import java.util.function.Consumer;

/** The stand-in for fastutil's IntIterator: an iterator of ints, boxing them where it is asked for Integers. */
public interface IntIterator extends java.util.PrimitiveIterator.OfInt {
    @Override
    int nextInt();

    @Override
    default Integer next() {
        return Integer.valueOf(nextInt());
    }

    @Override
    default void forEachRemaining(Consumer<? super Integer> action) {
        forEachRemaining(action instanceof java.util.function.IntConsumer ? (java.util.function.IntConsumer) action : (java.util.function.IntConsumer) action::accept);
    }

    /** Skips n elements (or as many as there are): how many were skipped. */
    default int skip(int n) {
        if (n < 0) throw new IllegalArgumentException("Argument must be nonnegative: " + n);
        int i = n;
        while (i-- != 0 && hasNext()) nextInt();
        return n - i - 1;
    }
}
