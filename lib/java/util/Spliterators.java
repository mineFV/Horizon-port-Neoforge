package java.util;

import java.util.function.Consumer;

/**
 * The model of java.util.Spliterators: over an array, over a collection's iterator (made when the traversal starts,
 * with the collection's size then), over an iterator, none; and an iterator over a spliterator. None splits.
 */
public final class Spliterators {
    private Spliterators() {}

    public static <T> Spliterator<T> emptySpliterator() {
        return new EmptySpliterator<>();
    }

    public static <T> Spliterator<T> spliterator(Object[] array, int additionalCharacteristics) {
        return new ArraySpliterator<>(Objects.requireNonNull(array), 0, array.length, additionalCharacteristics);
    }

    public static <T> Spliterator<T> spliterator(Object[] array, int fromIndex, int toIndex, int additionalCharacteristics) {
        checkFromToBounds(Objects.requireNonNull(array).length, fromIndex, toIndex);
        return new ArraySpliterator<>(array, fromIndex, toIndex, additionalCharacteristics);
    }

    static void checkFromToBounds(int arrayLength, int origin, int fence) {
        if (origin > fence) {
            throw new ArrayIndexOutOfBoundsException("origin(".concat(Integer.toString(origin)).concat(") > fence(").concat(Integer.toString(fence)).concat(")"));
        }
        if (origin < 0) throw new ArrayIndexOutOfBoundsException(origin);
        if (fence > arrayLength) throw new ArrayIndexOutOfBoundsException(fence);
    }

    public static <T> Spliterator<T> spliterator(Collection<? extends T> c, int characteristics) {
        return new IteratorSpliterator<>(Objects.requireNonNull(c), characteristics);
    }

    public static <T> Spliterator<T> spliterator(Iterator<? extends T> iterator, long size, int characteristics) {
        return new IteratorSpliterator<>(Objects.requireNonNull(iterator), size, characteristics);
    }

    public static <T> Spliterator<T> spliteratorUnknownSize(Iterator<? extends T> iterator, int characteristics) {
        return new IteratorSpliterator<>(Objects.requireNonNull(iterator), characteristics);
    }

    public static <T> Iterator<T> iterator(Spliterator<? extends T> spliterator) {
        Objects.requireNonNull(spliterator);
        return new Adapter<>(spliterator);
    }

    private static final class Adapter<T> implements Iterator<T>, Consumer<T> {
        private final Spliterator<? extends T> spliterator;
        private boolean valueReady;
        private T nextElement;

        Adapter(Spliterator<? extends T> spliterator) {
            this.spliterator = spliterator;
        }

        public void accept(T t) {
            valueReady = true;
            nextElement = t;
        }

        public boolean hasNext() {
            if (!valueReady) spliterator.tryAdvance(this);
            return valueReady;
        }

        public T next() {
            if (!valueReady && !hasNext()) throw new NoSuchElementException();
            valueReady = false;
            T t = nextElement;
            nextElement = null;
            return t;
        }

        public void forEachRemaining(Consumer<? super T> action) {
            Objects.requireNonNull(action);
            if (valueReady) {
                valueReady = false;
                T t = nextElement;
                nextElement = null;
                action.accept(t);
            }
            spliterator.forEachRemaining(action);
        }
    }

    private static final class EmptySpliterator<T> implements Spliterator<T> {
        public Spliterator<T> trySplit() {
            return null;
        }

        public boolean tryAdvance(Consumer<? super T> consumer) {
            Objects.requireNonNull(consumer);
            return false;
        }

        public void forEachRemaining(Consumer<? super T> consumer) {
            Objects.requireNonNull(consumer);
        }

        public long estimateSize() {
            return 0;
        }

        public int characteristics() {
            return Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

    /** Reads the array as it is when each element is reached: the array isn't copied. */
    static final class ArraySpliterator<T> implements Spliterator<T> {
        private final Object[] array;
        private int index;
        private final int fence;
        private final int characteristics;

        ArraySpliterator(Object[] array, int origin, int fence, int additionalCharacteristics) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.characteristics = additionalCharacteristics | Spliterator.SIZED | Spliterator.SUBSIZED;
        }

        public Spliterator<T> trySplit() {
            return null;
        }

        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super T> action) {
            Object[] a;
            int i, hi;
            if (action == null) throw new NullPointerException();
            if ((a = array).length >= (hi = fence) && (i = index) >= 0 && i < (index = hi)) {
                do {
                    action.accept((T) a[i]);
                } while (++i < hi);
            }
        }

        @SuppressWarnings("unchecked")
        public boolean tryAdvance(Consumer<? super T> action) {
            if (action == null) throw new NullPointerException();
            if (index >= 0 && index < fence) {
                T e = (T) array[index++];
                action.accept(e);
                return true;
            }
            return false;
        }

        public long estimateSize() {
            return fence - index;
        }

        public int characteristics() {
            return characteristics;
        }
    }

    /** Over a collection, whose iterator and size are taken when the traversal starts (or the size is asked). */
    static class IteratorSpliterator<T> implements Spliterator<T> {
        private final Collection<? extends T> collection;
        private Iterator<? extends T> it;
        private final int characteristics;
        private long est;

        IteratorSpliterator(Collection<? extends T> collection, int characteristics) {
            this.collection = collection;
            this.it = null;
            this.characteristics = (characteristics & Spliterator.CONCURRENT) == 0 ? characteristics | Spliterator.SIZED | Spliterator.SUBSIZED : characteristics;
        }

        IteratorSpliterator(Iterator<? extends T> iterator, long size, int characteristics) {
            this.collection = null;
            this.it = iterator;
            this.est = size;
            this.characteristics = (characteristics & Spliterator.CONCURRENT) == 0 ? characteristics | Spliterator.SIZED | Spliterator.SUBSIZED : characteristics;
        }

        IteratorSpliterator(Iterator<? extends T> iterator, int characteristics) {
            this.collection = null;
            this.it = iterator;
            this.est = Long.MAX_VALUE;
            this.characteristics = characteristics & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
        }

        public Spliterator<T> trySplit() {
            return null;
        }

        public void forEachRemaining(Consumer<? super T> action) {
            if (action == null) throw new NullPointerException();
            Iterator<? extends T> i;
            if ((i = it) == null) {
                i = it = collection.iterator();
                est = collection.size();
            }
            i.forEachRemaining(action);
        }

        public boolean tryAdvance(Consumer<? super T> action) {
            if (action == null) throw new NullPointerException();
            if (it == null) {
                it = collection.iterator();
                est = collection.size();
            }
            if (it.hasNext()) {
                action.accept(it.next());
                return true;
            }
            return false;
        }

        public long estimateSize() {
            if (it == null) {
                it = collection.iterator();
                return est = collection.size();
            }
            return est;
        }

        public int characteristics() {
            return characteristics;
        }
    }
}
