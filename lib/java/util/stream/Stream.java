package java.util.stream;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * The model of java.util.stream.Stream, run one element after another as the JDK runs a sequential stream (a
 * parallel one too: a sequential run is one of the runs it allows). Not modelled: limit, skip, takeWhile, dropWhile,
 * mapMulti, iterate, generate, mapToDouble and DoubleStream.
 */
public interface Stream<T> extends BaseStream<T, Stream<T>> {
    Stream<T> filter(Predicate<? super T> predicate);

    <R> Stream<R> map(Function<? super T, ? extends R> mapper);

    IntStream mapToInt(ToIntFunction<? super T> mapper);

    LongStream mapToLong(ToLongFunction<? super T> mapper);

    <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

    Stream<T> distinct();

    Stream<T> sorted();

    Stream<T> sorted(Comparator<? super T> comparator);

    Stream<T> peek(Consumer<? super T> action);

    void forEach(Consumer<? super T> action);

    void forEachOrdered(Consumer<? super T> action);

    Object[] toArray();

    <A> A[] toArray(IntFunction<A[]> generator);

    T reduce(T identity, BinaryOperator<T> accumulator);

    Optional<T> reduce(BinaryOperator<T> accumulator);

    <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner);

    <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner);

    <R, A> R collect(Collector<? super T, A, R> collector);

    @SuppressWarnings("unchecked")
    default List<T> toList() {
        return (List<T>) java.util.Collections.unmodifiableList(new java.util.ArrayList<>(java.util.Arrays.asList(this.toArray())));
    }

    Optional<T> min(Comparator<? super T> comparator);

    Optional<T> max(Comparator<? super T> comparator);

    long count();

    boolean anyMatch(Predicate<? super T> predicate);

    boolean allMatch(Predicate<? super T> predicate);

    boolean noneMatch(Predicate<? super T> predicate);

    Optional<T> findFirst();

    Optional<T> findAny();

    static <T> Builder<T> builder() {
        return new Streams.StreamBuilder<>();
    }

    static <T> Stream<T> empty() {
        return StreamSupport.stream(Spliterators.<T>emptySpliterator(), false);
    }

    static <T> Stream<T> of(T t) {
        return StreamSupport.stream(Spliterators.spliterator(new Object[] {t}, Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
    }

    static <T> Stream<T> ofNullable(T t) {
        return t == null ? Stream.empty() : Stream.of(t);
    }

    @SafeVarargs
    static <T> Stream<T> of(T... values) {
        return java.util.Arrays.stream(values);
    }

    @SuppressWarnings("unchecked")
    static <T> Stream<T> concat(Stream<? extends T> a, Stream<? extends T> b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Spliterator<T> split = new Streams.ConcatSpliterator<>((Spliterator<T>) a.spliterator(), (Spliterator<T>) b.spliterator());
        Stream<T> stream = StreamSupport.stream(split, a.isParallel() || b.isParallel());
        return stream.onClose(Streams.composedClose(a, b));
    }

    interface Builder<T> extends Consumer<T> {
        @Override
        void accept(T t);

        default Builder<T> add(T t) {
            accept(t);
            return this;
        }

        Stream<T> build();
    }
}
