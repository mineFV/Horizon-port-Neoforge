package com.google.common.collect;

/** The stand-in for Guava's UnmodifiableListIterator: a list iterator whose changes refuse. */
public abstract class UnmodifiableListIterator<E> extends UnmodifiableIterator<E> implements java.util.ListIterator<E> {
    protected UnmodifiableListIterator() {}

    @Override
    public final void add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void set(E e) {
        throw new UnsupportedOperationException();
    }
}
