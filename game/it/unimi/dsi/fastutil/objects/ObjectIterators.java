package it.unimi.dsi.fastutil.objects;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

/** The stand-in for fastutil's ObjectIterators: the empty iterator alone. */
public final class ObjectIterators {
    private ObjectIterators() {}

    /** An iterator of nothing, either way: one for all (observed against fastutil, `standins`). */
    public static class EmptyIterator<K> implements ObjectListIterator<K>, java.io.Serializable, Cloneable {
        protected EmptyIterator() {}

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public K next() {
            throw new NoSuchElementException();
        }

        @Override
        public K previous() {
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return 0;
        }

        @Override
        public int previousIndex() {
            return -1;
        }

        @Override
        public int skip(int n) {
            return 0;
        }

        @Override
        public int back(int n) {
            return 0;
        }

        @Override
        public void forEachRemaining(Consumer<? super K> action) {}

        @Override
        public Object clone() {
            return EMPTY_ITERATOR;
        }
    }

    @SuppressWarnings("rawtypes")
    public static final EmptyIterator EMPTY_ITERATOR = new EmptyIterator();

    @SuppressWarnings("unchecked")
    public static <K> ObjectIterator<K> emptyIterator() {
        return EMPTY_ITERATOR;
    }
}
