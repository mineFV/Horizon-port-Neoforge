package com.google.common.collect;

/** The stand-in for Guava's ImmutableCollection: a collection without null that can't be changed (observed against Guava, `standins`). */
public abstract class ImmutableCollection<E> extends java.util.AbstractCollection<E> implements java.io.Serializable {
    ImmutableCollection() {}

    @Override
    public abstract UnmodifiableIterator<E> iterator();

    @Override
    public final Object[] toArray() {
        return super.toArray();
    }

    @Override
    public final <T> T[] toArray(T[] other) {
        return super.toArray(other);
    }

    @Override
    public abstract boolean contains(Object object);

    @Override
    public final boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean addAll(java.util.Collection<? extends E> newElements) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean removeAll(java.util.Collection<?> oldElements) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean retainAll(java.util.Collection<?> elementsToKeep) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void clear() {
        throw new UnsupportedOperationException();
    }

    public ImmutableList<E> asList() {
        return ImmutableList.copyOfArray(toArray());
    }

    /** The stand-in for ImmutableCollection.Builder. */
    public abstract static class Builder<E> {
        Builder() {}

        public abstract Builder<E> add(E element);

        @SuppressWarnings("unchecked")
        public Builder<E> add(E... elements) {
            for (E e : elements) add(e);
            return this;
        }

        public Builder<E> addAll(Iterable<? extends E> elements) {
            for (E e : elements) add(e);
            return this;
        }

        public Builder<E> addAll(java.util.Iterator<? extends E> elements) {
            while (elements.hasNext()) add(elements.next());
            return this;
        }

        public abstract ImmutableCollection<E> build();
    }
}
