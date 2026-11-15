package java.util.stream;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalDouble;
import java.util.Spliterator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

/** A IntStream: a stream of Integer, whose operations unbox and box as they go. */
final class IntPipeline implements IntStream {
    final ReferencePipeline<Integer> p;

    IntPipeline(ReferencePipeline<Integer> p) {
        this.p = p;
    }

    public IntStream filter(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new IntPipeline(p.filter(x -> predicate.test(x)));
    }

    public IntStream map(IntUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline(p.map(x -> mapper.applyAsInt(x)));
    }

    public <U> Stream<U> mapToObj(IntFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return p.map(x -> mapper.apply(x));
    }

    public LongStream mapToLong(java.util.function.IntToLongFunction mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline(p.map(x -> mapper.applyAsLong(x)));
    }

    public LongStream asLongStream() {
        return mapToLong(x -> x);
    }

    public Stream<Integer> boxed() {
        return mapToObj(Integer::valueOf);
    }

    public IntStream distinct() {
        return new IntPipeline(p.distinct());
    }

    public IntStream sorted() {
        return new IntPipeline(p.sorted());
    }

    public IntStream peek(IntConsumer action) {
        Objects.requireNonNull(action);
        return new IntPipeline(p.peek(x -> action.accept(x)));
    }

    public void forEach(IntConsumer action) {
        Objects.requireNonNull(action);
        p.forEach(x -> action.accept(x));
    }

    public void forEachOrdered(IntConsumer action) {
        forEach(action);
    }

    public int[] toArray() {
        Object[] boxes = p.toArray();
        int[] out = new int[boxes.length];
        for (int i = 0; i < boxes.length; i++) out[i] = (Integer) boxes[i];
        return out;
    }

    public int reduce(int identity, IntBinaryOperator op) {
        Objects.requireNonNull(op);
        return p.reduce(identity, (a, b) -> op.applyAsInt(a, b));
    }

    public OptionalInt reduce(IntBinaryOperator op) {
        Objects.requireNonNull(op);
        Optional<Integer> r = p.reduce((a, b) -> op.applyAsInt(a, b));
        return r.isPresent() ? OptionalInt.of(r.get()) : OptionalInt.empty();
    }

    public int sum() {
        return reduce(0, Integer::sum);
    }

    public OptionalInt min() {
        return reduce((a, b) -> Math.min(a, b));
    }

    public OptionalInt max() {
        return reduce((a, b) -> Math.max(a, b));
    }

    public long count() {
        return p.count();
    }

    public OptionalDouble average() {
        long[] avg = p.collect(() -> new long[2], (ll, i) -> {
            ll[0]++;
            ll[1] += i;
        }, (ll, rr) -> {
            ll[0] += rr[0];
            ll[1] += rr[1];
        });
        return avg[0] > 0 ? OptionalDouble.of((double) avg[1] / avg[0]) : OptionalDouble.empty();
    }

    public boolean anyMatch(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return p.anyMatch(x -> predicate.test(x));
    }

    public boolean allMatch(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return p.allMatch(x -> predicate.test(x));
    }

    public boolean noneMatch(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return p.noneMatch(x -> predicate.test(x));
    }

    public OptionalInt findFirst() {
        Optional<Integer> r = p.findFirst();
        return r.isPresent() ? OptionalInt.of(r.get()) : OptionalInt.empty();
    }

    public OptionalInt findAny() {
        return findFirst();
    }

    public Iterator<Integer> iterator() {
        return p.iterator();
    }

    public Spliterator<Integer> spliterator() {
        return p.spliterator();
    }

    public boolean isParallel() {
        return p.isParallel();
    }

    public IntStream sequential() {
        p.sequential();
        return this;
    }

    public IntStream parallel() {
        p.parallel();
        return this;
    }

    public IntStream unordered() {
        ReferencePipeline<Integer> u = p.unordered();
        return u == p ? this : new IntPipeline(u);
    }

    public IntStream onClose(Runnable closeHandler) {
        p.onClose(closeHandler);
        return this;
    }

    public void close() {
        p.close();
    }
}
