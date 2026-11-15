package java.util.stream;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.OptionalDouble;
import java.util.Spliterator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;

/** A LongStream: a stream of Long, whose operations unbox and box as they go. */
final class LongPipeline implements LongStream {
    final ReferencePipeline<Long> p;

    LongPipeline(ReferencePipeline<Long> p) {
        this.p = p;
    }

    public LongStream filter(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new LongPipeline(p.filter(x -> predicate.test(x)));
    }

    public LongStream map(LongUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline(p.map(x -> mapper.applyAsLong(x)));
    }

    public <U> Stream<U> mapToObj(LongFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return p.map(x -> mapper.apply(x));
    }

    public IntStream mapToInt(java.util.function.LongToIntFunction mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline(p.map(x -> mapper.applyAsInt(x)));
    }

    public Stream<Long> boxed() {
        return mapToObj(Long::valueOf);
    }

    public LongStream distinct() {
        return new LongPipeline(p.distinct());
    }

    public LongStream sorted() {
        return new LongPipeline(p.sorted());
    }

    public LongStream peek(LongConsumer action) {
        Objects.requireNonNull(action);
        return new LongPipeline(p.peek(x -> action.accept(x)));
    }

    public void forEach(LongConsumer action) {
        Objects.requireNonNull(action);
        p.forEach(x -> action.accept(x));
    }

    public void forEachOrdered(LongConsumer action) {
        forEach(action);
    }

    public long[] toArray() {
        Object[] boxes = p.toArray();
        long[] out = new long[boxes.length];
        for (int i = 0; i < boxes.length; i++) out[i] = (Long) boxes[i];
        return out;
    }

    public long reduce(long identity, LongBinaryOperator op) {
        Objects.requireNonNull(op);
        return p.reduce(identity, (a, b) -> op.applyAsLong(a, b));
    }

    public OptionalLong reduce(LongBinaryOperator op) {
        Objects.requireNonNull(op);
        Optional<Long> r = p.reduce((a, b) -> op.applyAsLong(a, b));
        return r.isPresent() ? OptionalLong.of(r.get()) : OptionalLong.empty();
    }

    public long sum() {
        return reduce(0, Long::sum);
    }

    public OptionalLong min() {
        return reduce((a, b) -> Math.min(a, b));
    }

    public OptionalLong max() {
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

    public boolean anyMatch(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return p.anyMatch(x -> predicate.test(x));
    }

    public boolean allMatch(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return p.allMatch(x -> predicate.test(x));
    }

    public boolean noneMatch(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return p.noneMatch(x -> predicate.test(x));
    }

    public OptionalLong findFirst() {
        Optional<Long> r = p.findFirst();
        return r.isPresent() ? OptionalLong.of(r.get()) : OptionalLong.empty();
    }

    public OptionalLong findAny() {
        return findFirst();
    }

    public Iterator<Long> iterator() {
        return p.iterator();
    }

    public Spliterator<Long> spliterator() {
        return p.spliterator();
    }

    public boolean isParallel() {
        return p.isParallel();
    }

    public LongStream sequential() {
        p.sequential();
        return this;
    }

    public LongStream parallel() {
        p.parallel();
        return this;
    }

    public LongStream unordered() {
        ReferencePipeline<Long> u = p.unordered();
        return u == p ? this : new LongPipeline(u);
    }

    public LongStream onClose(Runnable closeHandler) {
        p.onClose(closeHandler);
        return this;
    }

    public void close() {
        p.close();
    }
}
