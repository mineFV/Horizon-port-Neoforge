package java.util;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * The model of java.util.PrimitiveIterator, with OfInt, OfLong and OfDouble and their defaults as the API specifies them
 * (a Consumer that isn't of the primitive kind gets the values boxed). The model's IntStream and LongStream give boxed
 * iterators, not these: they are for the game's libraries' primitive iterators (fastutil's IntIterator, LongIterator).
 */
public interface PrimitiveIterator<T, T_CONS> extends Iterator<T> {
    void forEachRemaining(T_CONS action);

    /** An iterator of ints. */
    interface OfInt extends PrimitiveIterator<Integer, IntConsumer> {
        int nextInt();

        @Override
        default void forEachRemaining(IntConsumer action) {
            Objects.requireNonNull(action);
            while (hasNext()) action.accept(nextInt());
        }

        @Override
        default Integer next() {
            return nextInt();
        }

        @Override
        default void forEachRemaining(Consumer<? super Integer> action) {
            if (action instanceof IntConsumer) {
                forEachRemaining((IntConsumer) action);
            } else {
                Objects.requireNonNull(action);
                forEachRemaining((IntConsumer) action::accept);
            }
        }
    }

    /** An iterator of longs. */
    interface OfLong extends PrimitiveIterator<Long, LongConsumer> {
        long nextLong();

        @Override
        default void forEachRemaining(LongConsumer action) {
            Objects.requireNonNull(action);
            while (hasNext()) action.accept(nextLong());
        }

        @Override
        default Long next() {
            return nextLong();
        }

        @Override
        default void forEachRemaining(Consumer<? super Long> action) {
            if (action instanceof LongConsumer) {
                forEachRemaining((LongConsumer) action);
            } else {
                Objects.requireNonNull(action);
                forEachRemaining((LongConsumer) action::accept);
            }
        }
    }

    /** An iterator of doubles. */
    interface OfDouble extends PrimitiveIterator<Double, DoubleConsumer> {
        double nextDouble();

        @Override
        default void forEachRemaining(DoubleConsumer action) {
            Objects.requireNonNull(action);
            while (hasNext()) action.accept(nextDouble());
        }

        @Override
        default Double next() {
            return nextDouble();
        }

        @Override
        default void forEachRemaining(Consumer<? super Double> action) {
            if (action instanceof DoubleConsumer) {
                forEachRemaining((DoubleConsumer) action);
            } else {
                Objects.requireNonNull(action);
                forEachRemaining((DoubleConsumer) action::accept);
            }
        }
    }
}
