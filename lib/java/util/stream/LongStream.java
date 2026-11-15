package java.util.stream;

import java.util.Iterator;
import java.util.OptionalLong;
import java.util.OptionalDouble;
import java.util.Spliterator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;

/**
 * The model of java.util.stream.LongStream, run as a stream of Long (the operations get and give longs, as the JDK's).
 * Its iterator and spliterator are of Long, not the JDK's primitive ones (PrimitiveIterator.OfLong, which isn't
 * modelled).
 */
public interface LongStream extends BaseStream<Long, LongStream> {
    LongStream filter(LongPredicate predicate);

    LongStream map(LongUnaryOperator mapper);

    <U> Stream<U> mapToObj(LongFunction<? extends U> mapper);

    IntStream mapToInt(java.util.function.LongToIntFunction mapper);

    Stream<Long> boxed();

    LongStream distinct();

    LongStream sorted();

    LongStream peek(LongConsumer action);

    void forEach(LongConsumer action);

    void forEachOrdered(LongConsumer action);

    long[] toArray();

    long reduce(long identity, LongBinaryOperator op);

    OptionalLong reduce(LongBinaryOperator op);

    long sum();

    OptionalLong min();

    OptionalLong max();

    long count();

    OptionalDouble average();

    boolean anyMatch(LongPredicate predicate);

    boolean allMatch(LongPredicate predicate);

    boolean noneMatch(LongPredicate predicate);

    OptionalLong findFirst();

    OptionalLong findAny();

    static LongStream empty() {
        return new LongPipeline(new ReferencePipeline<Long>(java.util.Spliterators.emptySpliterator(), false));
    }

    static LongStream of(long value) {
        return jOf(new long[] {value}, 0, 1);
    }

    static LongStream of(long... values) {
        return java.util.Arrays.stream(values);
    }

    /** Arrays.stream's: not the JDK's API, a mod (built against the JDK) can't call it. */
    static LongStream jOf(long[] array, int startInclusive, int endExclusive) {
        java.util.Objects.requireNonNull(array);
        if (startInclusive > endExclusive) throw new ArrayIndexOutOfBoundsException("origin(".concat(Integer.toString(startInclusive)).concat(") > fence(").concat(Integer.toString(endExclusive)).concat(")"));
        if (startInclusive < 0) throw new ArrayIndexOutOfBoundsException(startInclusive);
        if (endExclusive > array.length) throw new ArrayIndexOutOfBoundsException(endExclusive);
        return new LongPipeline(new ReferencePipeline<Long>(new Streams.PrimitiveArraySpliterator(null, array, startInclusive, endExclusive), false));
    }

    static LongStream range(long startInclusive, long endExclusive) {
        if (startInclusive >= endExclusive) return empty();
        return new LongPipeline(new ReferencePipeline<Long>(new Streams.RangeSpliterator(startInclusive, endExclusive, false), false));
    }

    static LongStream rangeClosed(long startInclusive, long endInclusive) {
        if (startInclusive > endInclusive) return empty();
        return new LongPipeline(new ReferencePipeline<Long>(new Streams.RangeSpliterator(startInclusive, (long) endInclusive + 1, false), false));
    }
}
