package java.util.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
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
 * A stream, as the JDK runs a sequential one: a chain of stages from a head that holds the source spliterator. An
 * operation links a new stage (a stage already linked or consumed can't be used again); a terminal one pushes the
 * source's elements through the stages' sinks: begin with the source's size if known, each element, end; a
 * short-circuiting one (anyMatch, findFirst...) asks before each element whether to go on. The flags the JDK keeps
 * that change what runs are kept: SIZED (count and toArray know the size without running the stages), DISTINCT
 * (distinct passes all on), SORTED (natural order: sorted() passes all on, distinct compares neighbours), ORDERED
 * (unordered() links nothing on a stream already unordered).
 */
class ReferencePipeline<T> implements Stream<T> {
    static final String MSG_STREAM_LINKED = "stream has already been operated upon or closed";
    static final String MSG_CONSUMED = "source already consumed or closed";
    static final String BAD_SIZE = "Stream size exceeds max array size";
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    static final int SIZED = 1, DISTINCT = 2, SORTED = 4, ORDERED = 8;

    /** How a stage passes elements on: the sink it puts before downstream's, given the flags of what comes in. */
    interface Stage {
        Sink wrap(int upstreamFlags, Sink downstream);
    }

    private final ReferencePipeline<?> head;
    private final ReferencePipeline<?> previous;
    private final Stage stage;
    final int flags;
    private boolean linkedOrConsumed;
    // The head's alone:
    private Spliterator<?> source;
    private Runnable closeAction;
    private boolean parallel;

    ReferencePipeline(Spliterator<?> source, boolean parallel) {
        this.head = this;
        this.previous = null;
        this.stage = null;
        this.source = source;
        this.flags = flagsOf(source);
        this.parallel = parallel;
    }

    ReferencePipeline(ReferencePipeline<?> previous, int flags, Stage stage) {
        if (previous.linkedOrConsumed) throw new IllegalStateException(MSG_STREAM_LINKED);
        previous.linkedOrConsumed = true;
        this.head = previous.head;
        this.previous = previous;
        this.stage = stage;
        this.flags = flags;
    }

    /** A source's flags: SORTED only in its natural order, as the JDK's. */
    static int flagsOf(Spliterator<?> s) {
        int c = s.characteristics();
        int f = 0;
        if ((c & Spliterator.SIZED) != 0) f |= SIZED;
        if ((c & Spliterator.DISTINCT) != 0) f |= DISTINCT;
        if ((c & Spliterator.ORDERED) != 0) f |= ORDERED;
        if ((c & Spliterator.SORTED) != 0 && s.getComparator() == null) f |= SORTED;
        return f;
    }

    static int characteristicsOf(int flags) {
        int c = 0;
        if ((flags & SIZED) != 0) c |= Spliterator.SIZED | Spliterator.SUBSIZED;
        if ((flags & DISTINCT) != 0) c |= Spliterator.DISTINCT;
        if ((flags & SORTED) != 0) c |= Spliterator.SORTED;
        if ((flags & ORDERED) != 0) c |= Spliterator.ORDERED;
        return c;
    }

    // ---- running ----

    final Spliterator<?> sourceSpliterator() {
        if (head.source == null) throw new IllegalStateException(MSG_CONSUMED);
        Spliterator<?> s = head.source;
        head.source = null;
        return s;
    }

    /** Marks this stage consumed: a terminal operation's start. */
    final void consume() {
        if (linkedOrConsumed) throw new IllegalStateException(MSG_STREAM_LINKED);
        linkedOrConsumed = true;
    }

    /** The sink the source's elements go into: each stage's, from the head's next to this one's, then terminal. */
    final Sink wrap(Sink sink) {
        for (ReferencePipeline<?> p = this; p.previous != null; p = p.previous) sink = p.stage.wrap(p.previous.flags, sink);
        return sink;
    }

    static void copyInto(Sink sink, Spliterator<?> spliterator, boolean shortCircuit) {
        sink.begin(spliterator.getExactSizeIfKnown());
        if (shortCircuit) {
            do {
            } while (!sink.cancellationRequested() && spliterator.tryAdvance(sink));
        } else {
            spliterator.forEachRemaining(sink);
        }
        sink.end();
    }

    final void run(Sink terminal, boolean shortCircuit) {
        consume();
        Spliterator<?> s = sourceSpliterator();
        copyInto(wrap(terminal), s, shortCircuit);
    }

    // ---- intermediate operations ----

    @SuppressWarnings("unchecked")
    public ReferencePipeline<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return new ReferencePipeline<>(this, flags & ~SIZED, (f, down) -> new Sink.Chained(down) {
            public void begin(long size) {
                downstream.begin(-1);
            }

            public void accept(Object u) {
                if (predicate.test((T) u)) downstream.accept(u);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public <R> ReferencePipeline<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return new ReferencePipeline<>(this, flags & ~(DISTINCT | SORTED), (f, down) -> new Sink.Chained(down) {
            public void accept(Object u) {
                downstream.accept(mapper.apply((T) u));
            }
        });
    }

    @SuppressWarnings("unchecked")
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline(new ReferencePipeline<Integer>(this, flags & ~(DISTINCT | SORTED), (f, down) -> new Sink.Chained(down) {
            public void accept(Object u) {
                downstream.accept(mapper.applyAsInt((T) u));
            }
        }));
    }

    @SuppressWarnings("unchecked")
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline(new ReferencePipeline<Long>(this, flags & ~(DISTINCT | SORTED), (f, down) -> new Sink.Chained(down) {
            public void accept(Object u) {
                downstream.accept(mapper.applyAsLong((T) u));
            }
        }));
    }

    /**
     * Each stream the mapper gives is run into the stages after, then closed (if it throws, closed too, with what
     * closing throws suppressed). Once a short-circuiting operation has asked whether to go on, its elements are
     * taken one at a time, asking before each.
     */
    @SuppressWarnings("unchecked")
    public <R> ReferencePipeline<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        Objects.requireNonNull(mapper);
        return new ReferencePipeline<>(this, flags & ~(SIZED | DISTINCT | SORTED), (f, down) -> new Sink.Chained(down) {
            boolean cancellationRequestedCalled;

            public void begin(long size) {
                downstream.begin(-1);
            }

            public void accept(Object u) {
                Stream<? extends R> result = mapper.apply((T) u);
                if (result == null) return;
                try {
                    if (!cancellationRequestedCalled) {
                        result.sequential().forEach(downstream);
                    } else {
                        Spliterator<? extends R> s = result.sequential().spliterator();
                        do {
                        } while (!downstream.cancellationRequested() && s.tryAdvance(downstream));
                    }
                } catch (Throwable t) {
                    try {
                        result.close();
                    } catch (Throwable c) {
                        t.addSuppressed(c);
                    }
                    throw t;
                }
                result.close();
            }

            public boolean cancellationRequested() {
                cancellationRequestedCalled = true;
                return downstream.cancellationRequested();
            }
        });
    }

    /** By a HashSet; by comparing with the last one on a stream sorted in its natural order; none on one distinct. */
    public ReferencePipeline<T> distinct() {
        return new ReferencePipeline<>(this, (flags | DISTINCT) & ~SIZED, (f, down) -> {
            if ((f & DISTINCT) != 0) return down;
            if ((f & SORTED) != 0) {
                return new Sink.Chained(down) {
                    boolean seenNull;
                    Object lastSeen;

                    public void begin(long size) {
                        seenNull = false;
                        lastSeen = null;
                        downstream.begin(-1);
                    }

                    public void end() {
                        seenNull = false;
                        lastSeen = null;
                        downstream.end();
                    }

                    public void accept(Object t) {
                        if (t == null) {
                            if (!seenNull) {
                                seenNull = true;
                                downstream.accept(lastSeen = null);
                            }
                        } else if (lastSeen == null || !t.equals(lastSeen)) {
                            downstream.accept(lastSeen = t);
                        }
                    }
                };
            }
            return new Sink.Chained(down) {
                HashSet<Object> seen;

                public void begin(long size) {
                    seen = new HashSet<>();
                    downstream.begin(-1);
                }

                public void end() {
                    seen = null;
                    downstream.end();
                }

                public void accept(Object t) {
                    if (seen.add(t)) downstream.accept(t);
                }
            };
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ReferencePipeline<T> sorted() {
        return sorting((Comparator<? super T>) (Comparator) Comparator.naturalOrder(), true);
    }

    public ReferencePipeline<T> sorted(Comparator<? super T> comparator) {
        return sorting(Objects.requireNonNull(comparator), false);
    }

    /**
     * Takes every element in, sorts them (stably, as the JDK's sort), then passes them on; once a short-circuiting
     * operation has asked whether to go on, it asks before each.
     */
    @SuppressWarnings("unchecked")
    private ReferencePipeline<T> sorting(Comparator<? super T> comparator, boolean natural) {
        return new ReferencePipeline<>(this, natural ? flags | SORTED | ORDERED : (flags & ~SORTED) | ORDERED, (f, down) -> {
            if (natural && (f & SORTED) != 0) return down;
            return new Sink.Chained(down) {
                ArrayList<Object> list;
                boolean cancellationRequestedCalled;

                public void begin(long size) {
                    if (size >= MAX_ARRAY_SIZE) throw new IllegalArgumentException(BAD_SIZE);
                    list = size >= 0 ? new ArrayList<>((int) size) : new ArrayList<>();
                }

                public void end() {
                    list.sort((Comparator<Object>) comparator);
                    downstream.begin(list.size());
                    if (!cancellationRequestedCalled) {
                        for (Object t : list) downstream.accept(t);
                    } else {
                        for (Object t : list) {
                            if (downstream.cancellationRequested()) break;
                            downstream.accept(t);
                        }
                    }
                    downstream.end();
                    list = null;
                }

                public boolean cancellationRequested() {
                    cancellationRequestedCalled = true;
                    return false;
                }

                public void accept(Object t) {
                    list.add(t);
                }
            };
        });
    }

    @SuppressWarnings("unchecked")
    public ReferencePipeline<T> peek(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        return new ReferencePipeline<>(this, flags, (f, down) -> new Sink.Chained(down) {
            public void accept(Object u) {
                action.accept((T) u);
                downstream.accept(u);
            }
        });
    }

    public ReferencePipeline<T> unordered() {
        if ((flags & ORDERED) == 0) return this;
        return new ReferencePipeline<>(this, flags & ~ORDERED, (f, down) -> down);
    }

    // ---- terminal operations ----

    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        run(t -> action.accept((T) t), false);
    }

    public void forEachOrdered(Consumer<? super T> action) {
        forEach(action);
    }

    public Object[] toArray() {
        return toArray(Object[]::new);
    }

    /**
     * Of a size known without running the stages: the generator's array first, filled as the elements come. Else the
     * elements first, then the generator's array, copied into as System.arraycopy does.
     */
    @SuppressWarnings("unchecked")
    public <A> A[] toArray(IntFunction<A[]> generator) {
        consume();
        Spliterator<?> s = sourceSpliterator();
        long size = (flags & SIZED) != 0 ? s.getExactSizeIfKnown() : -1;
        if (size >= 0) {
            if (size >= MAX_ARRAY_SIZE) throw new IllegalArgumentException(BAD_SIZE);
            Object[] array = generator.apply((int) size);
            copyInto(wrap(new Sink() {
                int curSize;

                public void begin(long n) {
                    if (n != array.length) {
                        throw new IllegalStateException("Begin size ".concat(Long.toString(n)).concat(" is not equal to fixed size ").concat(Integer.toString(array.length)));
                    }
                    curSize = 0;
                }

                public void accept(Object t) {
                    if (curSize < array.length) {
                        array[curSize++] = t;
                    } else {
                        throw new IllegalStateException("Accept exceeded fixed size of ".concat(Integer.toString(array.length)));
                    }
                }

                public void end() {
                    if (curSize < array.length) {
                        throw new IllegalStateException("End size ".concat(Integer.toString(curSize)).concat(" is less than fixed size ").concat(Integer.toString(array.length)));
                    }
                }
            }), s, false);
            return (A[]) array;
        }
        ArrayList<Object> buffer = new ArrayList<>();
        copyInto(wrap(buffer::add), s, false);
        int n = buffer.size();
        Object[] result = generator.apply(n);
        if (n > result.length) throw new IndexOutOfBoundsException("does not fit");
        for (int i = 0; i < n; i++) {
            try {
                result[i] = buffer.get(i);
            } catch (ArrayStoreException e) {
                String name = result.getClass().getName().substring(1);
                if (name.startsWith("L")) name = name.substring(1, name.length() - 1);
                throw new ArrayStoreException("arraycopy: element type mismatch: can not cast one of the elements of java.lang.Object[] to the type of the destination array, ".concat(name));
            }
        }
        return (A[]) result;
    }

    @SuppressWarnings("unchecked")
    public java.util.List<T> toList() {
        return Collections.jListFromTrustedArrayNullsAllowed(toArray());
    }

    @SuppressWarnings("unchecked")
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        Objects.requireNonNull(accumulator);
        class Reducing implements Sink {
            Object state;

            public void begin(long size) {
                state = identity;
            }

            public void accept(Object t) {
                state = accumulator.apply((T) state, (T) t);
            }
        }
        Reducing r = new Reducing();
        run(r, false);
        return (T) r.state;
    }

    @SuppressWarnings("unchecked")
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        Objects.requireNonNull(accumulator);
        class Reducing implements Sink {
            boolean empty;
            Object state;

            public void begin(long size) {
                empty = true;
                state = null;
            }

            public void accept(Object t) {
                if (empty) {
                    empty = false;
                    state = t;
                } else {
                    state = accumulator.apply((T) state, (T) t);
                }
            }
        }
        Reducing r = new Reducing();
        run(r, false);
        return r.empty ? Optional.empty() : Optional.of((T) r.state);
    }

    @SuppressWarnings("unchecked")
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        class Reducing implements Sink {
            Object state;

            public void begin(long size) {
                state = identity;
            }

            public void accept(Object t) {
                state = accumulator.apply((U) state, (T) t);
            }
        }
        Reducing r = new Reducing();
        run(r, false);
        return (U) r.state;
    }

    @SuppressWarnings("unchecked")
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        class Collecting implements Sink {
            Object state;

            public void begin(long size) {
                state = supplier.get();
            }

            public void accept(Object t) {
                accumulator.accept((R) state, (T) t);
            }
        }
        Collecting c = new Collecting();
        run(c, false);
        return (R) c.state;
    }

    /**
     * The collector is asked for its supplier, accumulator and combiner, then (the stream taken) its characteristics,
     * the container made at the start; at the end its characteristics again, and its finisher unless it has none.
     */
    @SuppressWarnings("unchecked")
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        Supplier<A> supplier = Objects.requireNonNull(collector).supplier();
        BiConsumer<A, ? super T> accumulator = collector.accumulator();
        collector.combiner();
        consume();
        collector.characteristics();
        Spliterator<?> s = sourceSpliterator();
        class Collecting implements Sink {
            Object state;

            public void begin(long size) {
                state = supplier.get();
            }

            public void accept(Object t) {
                accumulator.accept((A) state, (T) t);
            }
        }
        Collecting c = new Collecting();
        copyInto(wrap(c), s, false);
        return collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH) ? (R) c.state : collector.finisher().apply((A) c.state);
    }

    public Optional<T> min(Comparator<? super T> comparator) {
        return reduce(BinaryOperator.minBy(comparator));
    }

    public Optional<T> max(Comparator<? super T> comparator) {
        return reduce(BinaryOperator.maxBy(comparator));
    }

    /** Of a size known without running the stages, that size: the stages (a peek) don't run. */
    public long count() {
        consume();
        Spliterator<?> s = sourceSpliterator();
        if ((flags & SIZED) != 0) return s.getExactSizeIfKnown();
        class Counting implements Sink {
            long count;

            public void begin(long size) {
                count = 0L;
            }

            public void accept(Object t) {
                count++;
            }
        }
        Counting c = new Counting();
        copyInto(wrap(c), s, false);
        return c.count;
    }

    public boolean anyMatch(Predicate<? super T> predicate) {
        return match(predicate, true, true);
    }

    public boolean allMatch(Predicate<? super T> predicate) {
        return match(predicate, false, false);
    }

    public boolean noneMatch(Predicate<? super T> predicate) {
        return match(predicate, true, false);
    }

    @SuppressWarnings("unchecked")
    private boolean match(Predicate<? super T> predicate, boolean stopOnPredicateMatches, boolean shortCircuitResult) {
        Objects.requireNonNull(predicate);
        class Matching implements Sink {
            boolean stop;
            boolean value = !shortCircuitResult;

            public void accept(Object t) {
                if (!stop && predicate.test((T) t) == stopOnPredicateMatches) {
                    stop = true;
                    value = shortCircuitResult;
                }
            }

            public boolean cancellationRequested() {
                return stop;
            }
        }
        Matching m = new Matching();
        run(m, true);
        return m.value;
    }

    @SuppressWarnings("unchecked")
    public Optional<T> findFirst() {
        class Finding implements Sink {
            boolean hasValue;
            Object value;

            public void accept(Object t) {
                if (!hasValue) {
                    hasValue = true;
                    value = t;
                }
            }

            public boolean cancellationRequested() {
                return hasValue;
            }
        }
        Finding f = new Finding();
        run(f, true);
        return f.hasValue ? Optional.of((T) f.value) : Optional.empty();
    }

    public Optional<T> findAny() {
        return findFirst();
    }

    // ---- BaseStream ----

    public Iterator<T> iterator() {
        return Spliterators.iterator(spliterator());
    }

    /** The head's: its source. Another stage's: one that runs the stages as its elements are asked for. */
    @SuppressWarnings("unchecked")
    public Spliterator<T> spliterator() {
        consume();
        if (this == head) return (Spliterator<T>) sourceSpliterator();
        return new Wrapping<>(this);
    }

    public boolean isParallel() {
        return head.parallel;
    }

    public ReferencePipeline<T> sequential() {
        head.parallel = false;
        return this;
    }

    public ReferencePipeline<T> parallel() {
        head.parallel = true;
        return this;
    }

    public ReferencePipeline<T> onClose(Runnable closeHandler) {
        if (linkedOrConsumed) throw new IllegalStateException(MSG_STREAM_LINKED);
        Objects.requireNonNull(closeHandler);
        Runnable existing = head.closeAction;
        head.closeAction = existing == null ? closeHandler : Streams.composeWithExceptions(existing, closeHandler);
        return this;
    }

    public void close() {
        linkedOrConsumed = true;
        if (this == head) source = null;
        if (head.closeAction != null) {
            Runnable closeAction = head.closeAction;
            head.closeAction = null;
            closeAction.run();
        }
    }

    /**
     * A spliterator over a stage after the head: tryAdvance pushes the source's elements through the stages into a
     * buffer until one comes out; forEachRemaining runs them all into the action.
     */
    static final class Wrapping<T> implements Spliterator<T> {
        private final ReferencePipeline<T> pipeline;
        private Spliterator<?> spliterator;
        private Sink bufferSink;
        private ArrayList<Object> buffer;
        private int nextToConsume;
        private boolean finished;

        Wrapping(ReferencePipeline<T> pipeline) {
            this.pipeline = pipeline;
        }

        private void init() {
            if (spliterator == null) spliterator = pipeline.sourceSpliterator();
        }

        private boolean fillBuffer() {
            while (buffer.size() == 0) {
                if (bufferSink.cancellationRequested() || !spliterator.tryAdvance(bufferSink)) {
                    if (finished) return false;
                    bufferSink.end();
                    finished = true;
                }
            }
            return true;
        }

        private boolean doAdvance() {
            boolean hasNext;
            if (buffer == null) {
                if (finished) return false;
                init();
                buffer = new ArrayList<>();
                ArrayList<Object> b = buffer;
                bufferSink = pipeline.wrap(b::add);
                nextToConsume = 0;
                bufferSink.begin(spliterator.getExactSizeIfKnown());
                hasNext = fillBuffer();
            } else {
                ++nextToConsume;
                hasNext = nextToConsume < buffer.size();
                if (!hasNext) {
                    nextToConsume = 0;
                    buffer.clear();
                    hasNext = fillBuffer();
                }
            }
            return hasNext;
        }

        @SuppressWarnings("unchecked")
        public boolean tryAdvance(Consumer<? super T> consumer) {
            Objects.requireNonNull(consumer);
            boolean hasNext = doAdvance();
            if (hasNext) consumer.accept((T) buffer.get(nextToConsume));
            return hasNext;
        }

        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super T> consumer) {
            if (buffer == null && !finished) {
                Objects.requireNonNull(consumer);
                init();
                copyInto(pipeline.wrap(t -> consumer.accept((T) t)), spliterator, false);
                finished = true;
            } else {
                do {
                } while (tryAdvance(consumer));
            }
        }

        public Spliterator<T> trySplit() {
            return null;
        }

        public long estimateSize() {
            init();
            return spliterator.estimateSize();
        }

        public long getExactSizeIfKnown() {
            init();
            return (pipeline.flags & SIZED) != 0 ? spliterator.getExactSizeIfKnown() : -1;
        }

        public int characteristics() {
            init();
            int c = characteristicsOf(pipeline.flags);
            if ((c & Spliterator.SIZED) != 0) {
                c &= ~(Spliterator.SIZED | Spliterator.SUBSIZED);
                c |= spliterator.characteristics() & (Spliterator.SIZED | Spliterator.SUBSIZED);
            }
            return c;
        }

        public Comparator<? super T> getComparator() {
            if (!hasCharacteristics(Spliterator.SORTED)) throw new IllegalStateException();
            return null;
        }
    }
}
