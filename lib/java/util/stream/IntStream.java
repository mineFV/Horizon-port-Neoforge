package java.util.stream;

import java.util.Iterator;
import java.util.OptionalInt;
import java.util.OptionalDouble;
import java.util.Spliterator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

/**
 * The model of java.util.stream.IntStream, run as a stream of Integer (the operations get and give ints, as the JDK's).
 * Its iterator and spliterator are of Integer, not the JDK's primitive ones (PrimitiveIterator.OfInt, which isn't
 * modelled).
 */
public interface IntStream extends BaseStream<Integer, IntStream> {
    IntStream filter(IntPredicate predicate);

    IntStream map(IntUnaryOperator mapper);

    <U> Stream<U> mapToObj(IntFunction<? extends U> mapper);

    LongStream mapToLong(java.util.function.IntToLongFunction mapper);

    LongStream asLongStream();

    Stream<Integer> boxed();

    IntStream distinct();

    IntStream sorted();

    IntStream peek(IntConsumer action);

    void forEach(IntConsumer action);

    void forEachOrdered(IntConsumer action);

    int[] toArray();

    int reduce(int identity, IntBinaryOperator op);

    OptionalInt reduce(IntBinaryOperator op);

    int sum();

    OptionalInt min();

    OptionalInt max();

    long count();

    OptionalDouble average();

    boolean anyMatch(IntPredicate predicate);

    boolean allMatch(IntPredicate predicate);

    boolean noneMatch(IntPredicate predicate);

    OptionalInt findFirst();

    OptionalInt findAny();

    static IntStream empty() {
        return new IntPipeline(new ReferencePipeline<Integer>(java.util.Spliterators.emptySpliterator(), false));
    }

    static IntStream of(int value) {
        return jOf(new int[] {value}, 0, 1);
    }

    static IntStream of(int... values) {
        return java.util.Arrays.stream(values);
    }

    /** Arrays.stream's: not the JDK's API, a mod (built against the JDK) can't call it. */
    static IntStream jOf(int[] array, int startInclusive, int endExclusive) {
        java.util.Objects.requireNonNull(array);
        if (startInclusive > endExclusive) throw new ArrayIndexOutOfBoundsException("origin(".concat(Integer.toString(startInclusive)).concat(") > fence(").concat(Integer.toString(endExclusive)).concat(")"));
        if (startInclusive < 0) throw new ArrayIndexOutOfBoundsException(startInclusive);
        if (endExclusive > array.length) throw new ArrayIndexOutOfBoundsException(endExclusive);
        return new IntPipeline(new ReferencePipeline<Integer>(new Streams.PrimitiveArraySpliterator(array, null, startInclusive, endExclusive), false));
    }

    static IntStream range(int startInclusive, int endExclusive) {
        if (startInclusive >= endExclusive) return empty();
        return new IntPipeline(new ReferencePipeline<Integer>(new Streams.RangeSpliterator(startInclusive, endExclusive, true), false));
    }

    static IntStream rangeClosed(int startInclusive, int endInclusive) {
        if (startInclusive > endInclusive) return empty();
        return new IntPipeline(new ReferencePipeline<Integer>(new Streams.RangeSpliterator(startInclusive, (long) endInclusive + 1, true), false));
    }
}
