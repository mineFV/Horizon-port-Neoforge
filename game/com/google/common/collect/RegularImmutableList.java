package com.google.common.collect;

/** The stand-in for Guava's RegularImmutableList: an ImmutableList's elements in an array. */
class RegularImmutableList<E> extends ImmutableList<E> {
    private final Object[] array;

    RegularImmutableList(Object[] array) {
        this.array = array;
    }

    @Override
    public int size() {
        return array.length;
    }

    /**
     * Observed: an index outside it is the array's error, but for one element, which Guava's SingletonImmutableList
     * checks itself.
     */
    @Override
    @SuppressWarnings("unchecked")
    public E get(int index) {
        if (array.length == 1) {
            if (index < 0) throw new IndexOutOfBoundsException("index (" + index + ") must not be negative");
            if (index >= 1) throw new IndexOutOfBoundsException("index (" + index + ") must be less than size (1)");
        }
        return (E) array[index];
    }

    @Override
    public UnmodifiableListIterator<E> listIterator(int index) {
        return super.listIterator(index);
    }
}
