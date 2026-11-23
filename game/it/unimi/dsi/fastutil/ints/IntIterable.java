package it.unimi.dsi.fastutil.ints;

import java.util.Objects;
import java.util.function.Consumer;

/** The stand-in for fastutil's IntIterable: its iterator is of ints, and so is forEach where it can be. */
public interface IntIterable extends Iterable<Integer> {
    @Override
    IntIterator iterator();

    default void forEach(java.util.function.IntConsumer action) {
        Objects.requireNonNull(action);
        iterator().forEachRemaining(action);
    }

    @Override
    default void forEach(Consumer<? super Integer> action) {
        Objects.requireNonNull(action);
        forEach(action instanceof java.util.function.IntConsumer ? (java.util.function.IntConsumer) action : (java.util.function.IntConsumer) action::accept);
    }
}
