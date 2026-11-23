package com.google.common.collect;

/** The stand-in for Guava's TransformedIterator: another iterator's elements, each transformed; removal its. */
abstract class TransformedIterator<F, T> implements java.util.Iterator<T> {
    final java.util.Iterator<? extends F> backingIterator;

    TransformedIterator(java.util.Iterator<? extends F> backingIterator) {
        if (backingIterator == null) throw new NullPointerException();
        this.backingIterator = backingIterator;
    }

    abstract T transform(F from);

    @Override
    public final boolean hasNext() {
        return backingIterator.hasNext();
    }

    @Override
    public final T next() {
        return transform(backingIterator.next());
    }

    @Override
    public final void remove() {
        backingIterator.remove();
    }
}
