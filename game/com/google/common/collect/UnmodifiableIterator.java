package com.google.common.collect;

/** The stand-in for Guava's UnmodifiableIterator: an iterator whose remove refuses. */
public abstract class UnmodifiableIterator<E> implements java.util.Iterator<E> {
    protected UnmodifiableIterator() {}

    @Override
    public final void remove() {
        throw new UnsupportedOperationException();
    }
}
