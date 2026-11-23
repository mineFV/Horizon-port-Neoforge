package java.util;

import java.util.function.Consumer;

/**
 * The model of java.util.Spliterator, for streams run one element after another: the spliterators of the model's
 * collections don't split (trySplit gives null, which the API allows). Of the primitive kinds, OfInt is here with its
 * defaults as the API specifies them, for the game's libraries' int spliterators (fastutil's IntSpliterator); OfLong
 * and OfDouble aren't, and the model's IntStream doesn't give an OfInt.
 */
public interface Spliterator<T> {
    int ORDERED = 0x00000010;
    int DISTINCT = 0x00000001;
    int SORTED = 0x00000004;
    int SIZED = 0x00000040;
    int NONNULL = 0x00000100;
    int IMMUTABLE = 0x00000400;
    int CONCURRENT = 0x00001000;
    int SUBSIZED = 0x00004000;

    boolean tryAdvance(Consumer<? super T> action);

    default void forEachRemaining(Consumer<? super T> action) {
        do {
        } while (tryAdvance(action));
    }

    Spliterator<T> trySplit();

    long estimateSize();

    default long getExactSizeIfKnown() {
        return (characteristics() & SIZED) == 0 ? -1L : estimateSize();
    }

    int characteristics();

    default boolean hasCharacteristics(int characteristics) {
        return (characteristics() & characteristics) == characteristics;
    }

    default Comparator<? super T> getComparator() {
        throw new IllegalStateException();
    }

    /** A spliterator of a primitive kind. */
    interface OfPrimitive<T, T_CONS, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> extends Spliterator<T> {
        @Override
        T_SPLITR trySplit();

        boolean tryAdvance(T_CONS action);

        default void forEachRemaining(T_CONS action) {
            do {
            } while (tryAdvance(action));
        }
    }

    /** A spliterator of ints: a Consumer that isn't an IntConsumer gets them boxed. */
    interface OfInt extends OfPrimitive<Integer, java.util.function.IntConsumer, OfInt> {
        @Override
        OfInt trySplit();

        @Override
        boolean tryAdvance(java.util.function.IntConsumer action);

        @Override
        default void forEachRemaining(java.util.function.IntConsumer action) {
            do {
            } while (tryAdvance(action));
        }

        @Override
        default boolean tryAdvance(Consumer<? super Integer> action) {
            if (action instanceof java.util.function.IntConsumer) return tryAdvance((java.util.function.IntConsumer) action);
            Objects.requireNonNull(action);
            return tryAdvance((java.util.function.IntConsumer) action::accept);
        }

        @Override
        default void forEachRemaining(Consumer<? super Integer> action) {
            if (action instanceof java.util.function.IntConsumer) {
                forEachRemaining((java.util.function.IntConsumer) action);
            } else {
                Objects.requireNonNull(action);
                forEachRemaining((java.util.function.IntConsumer) action::accept);
            }
        }
    }
}
