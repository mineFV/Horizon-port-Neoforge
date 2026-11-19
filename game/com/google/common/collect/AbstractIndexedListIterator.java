package com.google.common.collect;

/** The stand-in for Guava's AbstractIndexedListIterator: a list iterator over indices, each element got by its index. */
abstract class AbstractIndexedListIterator<E> extends UnmodifiableListIterator<E> {
    private final int size;
    private int position;

    protected abstract E get(int index);

    protected AbstractIndexedListIterator(int size, int position) {
        if (position < 0) throw new IndexOutOfBoundsException("index (" + position + ") must not be negative");
        if (position > size) throw new IndexOutOfBoundsException("index (" + position + ") must not be greater than size (" + size + ")");
        this.size = size;
        this.position = position;
    }

    @Override
    public final boolean hasNext() {
        return position < size;
    }

    @Override
    public final E next() {
        if (!hasNext()) throw new java.util.NoSuchElementException();
        return get(position++);
    }

    @Override
    public final int nextIndex() {
        return position;
    }

    @Override
    public final boolean hasPrevious() {
        return position > 0;
    }

    @Override
    public final E previous() {
        if (!hasPrevious()) throw new java.util.NoSuchElementException();
        return get(--position);
    }

    @Override
    public final int previousIndex() {
        return position - 1;
    }
}
