package java.util.stream;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/** What the streams' factories share: the builder, concat's spliterator, ranges, arrays of ints and longs. */
final class Streams {
    private Streams() {}

    /** Stream.builder's: once built, it takes no more (an IllegalStateException). */
    static final class StreamBuilder<T> implements Stream.Builder<T> {
        private Object first;
        private ArrayList<Object> buffer;
        private int count;

        public void accept(T t) {
            if (count == 0) {
                first = t;
                count++;
            } else if (count > 0) {
                if (buffer == null) {
                    buffer = new ArrayList<>();
                    buffer.add(first);
                    count++;
                }
                buffer.add(t);
            } else {
                throw new IllegalStateException();
            }
        }

        public Stream.Builder<T> add(T t) {
            accept(t);
            return this;
        }

        public Stream<T> build() {
            int c = count;
            if (c >= 0) {
                count = -count - 1;
                Object[] elements = c == 0 ? new Object[0] : c == 1 ? new Object[] {first} : buffer.toArray();
                return StreamSupport.stream(Spliterators.spliterator(elements, Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
            }
            throw new IllegalStateException();
        }
    }

    /** concat's: a's elements, then b's; the sizes are asked for when it is made. */
    static final class ConcatSpliterator<T> implements Spliterator<T> {
        private final Spliterator<T> aSpliterator;
        private final Spliterator<T> bSpliterator;
        private boolean beforeSplit;
        private final boolean unsized;

        ConcatSpliterator(Spliterator<T> aSpliterator, Spliterator<T> bSpliterator) {
            this.aSpliterator = aSpliterator;
            this.bSpliterator = bSpliterator;
            beforeSplit = true;
            unsized = aSpliterator.estimateSize() + bSpliterator.estimateSize() < 0;
        }

        public Spliterator<T> trySplit() {
            return null;
        }

        public boolean tryAdvance(Consumer<? super T> consumer) {
            boolean hasNext;
            if (beforeSplit) {
                hasNext = aSpliterator.tryAdvance(consumer);
                if (!hasNext) {
                    beforeSplit = false;
                    hasNext = bSpliterator.tryAdvance(consumer);
                }
            } else {
                hasNext = bSpliterator.tryAdvance(consumer);
            }
            return hasNext;
        }

        public void forEachRemaining(Consumer<? super T> consumer) {
            if (beforeSplit) aSpliterator.forEachRemaining(consumer);
            bSpliterator.forEachRemaining(consumer);
        }

        public long estimateSize() {
            if (beforeSplit) {
                long size = aSpliterator.estimateSize() + bSpliterator.estimateSize();
                return (size >= 0) ? size : Long.MAX_VALUE;
            }
            return bSpliterator.estimateSize();
        }

        public int characteristics() {
            if (beforeSplit) {
                return aSpliterator.characteristics() & bSpliterator.characteristics() & ~(Spliterator.DISTINCT | Spliterator.SORTED | (unsized ? Spliterator.SIZED | Spliterator.SUBSIZED : 0));
            }
            return bSpliterator.characteristics();
        }
    }

    /** Runs a, then b even if a threw: what b throws is suppressed in what a threw. */
    static Runnable composeWithExceptions(Runnable a, Runnable b) {
        return () -> {
            try {
                a.run();
            } catch (Throwable e1) {
                try {
                    b.run();
                } catch (Throwable e2) {
                    try {
                        e1.addSuppressed(e2);
                    } catch (Throwable ignore) {
                    }
                }
                throw e1;
            }
            b.run();
        };
    }

    static Runnable composedClose(BaseStream<?, ?> a, BaseStream<?, ?> b) {
        return () -> {
            try {
                a.close();
            } catch (Throwable e1) {
                try {
                    b.close();
                } catch (Throwable e2) {
                    try {
                        e1.addSuppressed(e2);
                    } catch (Throwable ignore) {
                    }
                }
                throw e1;
            }
            b.close();
        };
    }

    /** IntStream.range's and LongStream.range's: from, up to the end (excluded); distinct and sorted. */
    static final class RangeSpliterator implements Spliterator<Object> {
        private long from;
        private final long upTo;
        private final boolean ints;

        RangeSpliterator(long from, long upTo, boolean ints) {
            this.from = from;
            this.upTo = upTo;
            this.ints = ints;
        }

        private Object box(long i) {
            return ints ? (Object) Integer.valueOf((int) i) : (Object) Long.valueOf(i);
        }

        public boolean tryAdvance(Consumer<? super Object> consumer) {
            Objects.requireNonNull(consumer);
            if (from < upTo) {
                long i = from++;
                consumer.accept(box(i));
                return true;
            }
            return false;
        }

        public void forEachRemaining(Consumer<? super Object> consumer) {
            Objects.requireNonNull(consumer);
            long i = from;
            long hUpTo = upTo;
            from = upTo;
            while (i < hUpTo) consumer.accept(box(i++));
        }

        public Spliterator<Object> trySplit() {
            return null;
        }

        public long estimateSize() {
            return upTo - from;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.DISTINCT | Spliterator.SORTED;
        }

        public java.util.Comparator<? super Object> getComparator() {
            return null;
        }
    }

    /** Over an int[] or a long[], read as each element is reached. */
    static final class PrimitiveArraySpliterator implements Spliterator<Object> {
        private final int[] ints;
        private final long[] longs;
        private int index;
        private final int fence;

        PrimitiveArraySpliterator(int[] ints, long[] longs, int origin, int fence) {
            this.ints = ints;
            this.longs = longs;
            this.index = origin;
            this.fence = fence;
        }

        private Object at(int i) {
            return ints != null ? (Object) Integer.valueOf(ints[i]) : (Object) Long.valueOf(longs[i]);
        }

        public boolean tryAdvance(Consumer<? super Object> action) {
            if (action == null) throw new NullPointerException();
            if (index >= 0 && index < fence) {
                action.accept(at(index++));
                return true;
            }
            return false;
        }

        public void forEachRemaining(Consumer<? super Object> action) {
            if (action == null) throw new NullPointerException();
            int length = ints != null ? ints.length : longs.length;
            int i, hi;
            if (length >= (hi = fence) && (i = index) >= 0 && i < (index = hi)) {
                do {
                    action.accept(at(i));
                } while (++i < hi);
            }
        }

        public Spliterator<Object> trySplit() {
            return null;
        }

        public long estimateSize() {
            return fence - index;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }
}
