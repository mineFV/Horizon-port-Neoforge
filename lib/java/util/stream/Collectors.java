package java.util.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The model of java.util.stream.Collectors: toCollection, toList, toUnmodifiableList, toSet, toUnmodifiableSet,
 * joining, mapping, counting, groupingBy and toMap, with the containers and messages of the JDK's.
 */
public final class Collectors {
    static final Set<Collector.Characteristics> CH_ID = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));
    static final Set<Collector.Characteristics> CH_UNORDERED_ID = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.UNORDERED, Collector.Characteristics.IDENTITY_FINISH));
    static final Set<Collector.Characteristics> CH_NOID = Collections.emptySet();
    static final Set<Collector.Characteristics> CH_UNORDERED_NOID = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.UNORDERED));

    private Collectors() {}

    static final class CollectorImpl<T, A, R> implements Collector<T, A, R> {
        private final Supplier<A> supplier;
        private final BiConsumer<A, T> accumulator;
        private final BinaryOperator<A> combiner;
        private final Function<A, R> finisher;
        private final Set<Characteristics> characteristics;

        CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner, Function<A, R> finisher, Set<Characteristics> characteristics) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }

        CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner, Set<Characteristics> characteristics) {
            this(supplier, accumulator, combiner, castingIdentity(), characteristics);
        }

        public BiConsumer<A, T> accumulator() {
            return accumulator;
        }

        public Supplier<A> supplier() {
            return supplier;
        }

        public BinaryOperator<A> combiner() {
            return combiner;
        }

        public Function<A, R> finisher() {
            return finisher;
        }

        public Set<Characteristics> characteristics() {
            return characteristics;
        }
    }

    @SuppressWarnings("unchecked")
    private static <I, R> Function<I, R> castingIdentity() {
        return i -> (R) i;
    }

    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(Supplier<C> collectionFactory) {
        return new CollectorImpl<>(collectionFactory, Collection<T>::add, (r1, r2) -> {
            r1.addAll(r2);
            return r1;
        }, CH_ID);
    }

    public static <T> Collector<T, ?, List<T>> toList() {
        return new CollectorImpl<>(ArrayList::new, List::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, CH_ID);
    }

    /** List.of's list of the elements: a null among them is a NullPointerException. */
    @SuppressWarnings("unchecked")
    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return new CollectorImpl<>(ArrayList::new, List::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, list -> {
            if (list.getClass() == ArrayList.class) return (List<T>) List.of(list.toArray());
            throw new IllegalArgumentException();
        }, CH_NOID);
    }

    public static <T> Collector<T, ?, Set<T>> toSet() {
        return new CollectorImpl<>(HashSet::new, Set::add, (left, right) -> {
            if (left.size() < right.size()) {
                right.addAll(left);
                return right;
            }
            left.addAll(right);
            return left;
        }, CH_UNORDERED_ID);
    }

    @SuppressWarnings("unchecked")
    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        return new CollectorImpl<>(HashSet::new, Set::add, (left, right) -> {
            if (left.size() < right.size()) {
                right.addAll(left);
                return right;
            }
            left.addAll(right);
            return left;
        }, set -> (Set<T>) Set.of(set.toArray()), CH_UNORDERED_NOID);
    }

    public static Collector<CharSequence, ?, String> joining() {
        return new CollectorImpl<CharSequence, StringBuilder, String>(StringBuilder::new, StringBuilder::append, (r1, r2) -> {
            r1.append(r2);
            return r1;
        }, StringBuilder::toString, CH_NOID);
    }

    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter) {
        return joining(delimiter, "", "");
    }

    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return new CollectorImpl<>(() -> new StringJoiner(delimiter, prefix, suffix), StringJoiner::add, StringJoiner::merge, StringJoiner::toString, CH_NOID);
    }

    public static <T, U, A, R> Collector<T, ?, R> mapping(Function<? super T, ? extends U> mapper, Collector<? super U, A, R> downstream) {
        BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();
        return new CollectorImpl<>(downstream.supplier(), (r, t) -> downstreamAccumulator.accept(r, mapper.apply(t)), downstream.combiner(), downstream.finisher(),
                downstream.characteristics());
    }

    public static <T> Collector<T, ?, Long> counting() {
        return new CollectorImpl<T, long[], Long>(() -> new long[1], (a, t) -> {
            a[0] += 1L;
        }, (a, b) -> {
            a[0] += b[0];
            return a;
        }, a -> a[0], CH_NOID);
    }

    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(Function<? super T, ? extends K> classifier) {
        return groupingBy(classifier, toList());
    }

    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return groupingBy(classifier, HashMap::new, downstream);
    }

    @SuppressWarnings("unchecked")
    public static <T, K, D, A, M extends Map<K, D>> Collector<T, ?, M> groupingBy(Function<? super T, ? extends K> classifier, Supplier<M> mapFactory,
            Collector<? super T, A, D> downstream) {
        Objects.requireNonNull(classifier, "classifier");
        Objects.requireNonNull(mapFactory, "mapFactory");
        Objects.requireNonNull(downstream, "downstream");
        Supplier<A> downstreamSupplier = downstream.supplier();
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BiConsumer<Map<K, A>, T> accumulator = (m, t) -> {
            K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
            A container = m.computeIfAbsent(key, k -> downstreamSupplier.get());
            downstreamAccumulator.accept(container, t);
        };
        BinaryOperator<Map<K, A>> merger = Collectors.<K, A, Map<K, A>>mapMerger(downstream.combiner());
        Supplier<Map<K, A>> mangledFactory = (Supplier<Map<K, A>>) mapFactory;
        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(mangledFactory, accumulator, merger, CH_ID);
        }
        Function<A, A> downstreamFinisher = (Function<A, A>) downstream.finisher();
        Function<Map<K, A>, M> finisher = intermediate -> {
            intermediate.replaceAll((k, v) -> downstreamFinisher.apply(v));
            return (M) intermediate;
        };
        return new CollectorImpl<>(mangledFactory, accumulator, merger, finisher, CH_NOID);
    }

    private static <K, V, M extends Map<K, V>> BinaryOperator<M> mapMerger(BinaryOperator<V> mergeFunction) {
        return (m1, m2) -> {
            for (Map.Entry<K, V> e : m2.entrySet()) m1.merge(e.getKey(), e.getValue(), mergeFunction);
            return m1;
        };
    }

    private static IllegalStateException duplicateKeyException(Object k, Object u, Object v) {
        return new IllegalStateException("Duplicate key ".concat(String.valueOf(k)).concat(" (attempted merging values ").concat(String.valueOf(u)).concat(" and ")
                .concat(String.valueOf(v)).concat(")"));
    }

    private static <T, K, V> BiConsumer<Map<K, V>, T> uniqKeysMapAccumulator(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return (map, element) -> {
            K k = keyMapper.apply(element);
            V v = Objects.requireNonNull(valueMapper.apply(element));
            V u = map.putIfAbsent(k, v);
            if (u != null) throw duplicateKeyException(k, u, v);
        };
    }

    private static <K, V, M extends Map<K, V>> BinaryOperator<M> uniqKeysMapMerger() {
        return (m1, m2) -> {
            for (Map.Entry<K, V> e : m2.entrySet()) {
                K k = e.getKey();
                V v = Objects.requireNonNull(e.getValue());
                V u = m1.putIfAbsent(k, v);
                if (u != null) throw duplicateKeyException(k, u, v);
            }
            return m1;
        };
    }

    /** A key met twice: an IllegalStateException; a null value: a NullPointerException. */
    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        Objects.requireNonNull(keyMapper);
        Objects.requireNonNull(valueMapper);
        return new CollectorImpl<>(HashMap::new, uniqKeysMapAccumulator(keyMapper, valueMapper), uniqKeysMapMerger(), CH_ID);
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper,
            BinaryOperator<U> mergeFunction) {
        return toMap(keyMapper, valueMapper, mergeFunction, HashMap::new);
    }

    public static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper,
            BinaryOperator<U> mergeFunction, Supplier<M> mapFactory) {
        Objects.requireNonNull(keyMapper);
        Objects.requireNonNull(valueMapper);
        Objects.requireNonNull(mergeFunction);
        Objects.requireNonNull(mapFactory);
        BiConsumer<M, T> accumulator = (map, element) -> map.merge(keyMapper.apply(element), valueMapper.apply(element), mergeFunction);
        return new CollectorImpl<>(mapFactory, accumulator, mapMerger(mergeFunction), CH_ID);
    }
}
