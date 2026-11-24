package it.unimi.dsi.fastutil.objects;

import java.util.Collection;
import java.util.function.Consumer;

/** The stand-in for fastutil's ObjectCollections: the empty collection alone. */
public final class ObjectCollections {
    private ObjectCollections() {}

    /**
     * A collection of nothing: equal to any empty collection, of hash 0, changing it refused (but clear, and removeIf
     * finding nothing). Its spliterator is Java's default for it (characteristics aren't fastutil's).
     */
    public abstract static class EmptyCollection<K> extends AbstractObjectCollection<K> {
        protected EmptyCollection() {}

        @Override
        public boolean contains(Object k) {
            return false;
        }

        @Override
        public Object[] toArray() {
            return ObjectArrays.EMPTY_ARRAY;
        }

        @Override
        public <T> T[] toArray(T[] array) {
            if (array.length > 0) array[0] = null;
            return array;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ObjectBidirectionalIterator<K> iterator() {
            return ObjectIterators.EMPTY_ITERATOR;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void clear() {}

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Collection<?> c)) return false;
            return c.isEmpty();
        }

        @Override
        public void forEach(Consumer<? super K> action) {}

        @Override
        public boolean containsAll(Collection<?> c) {
            return c.isEmpty();
        }

        @Override
        public boolean addAll(Collection<? extends K> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeIf(java.util.function.Predicate<? super K> filter) {
            java.util.Objects.requireNonNull(filter);
            return false;
        }
    }
}
