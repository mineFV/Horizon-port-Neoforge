package com.google.common.collect;

/**
 * The stand-in for Guava's ImmutableList: a list without null that can't be changed, equal to any list of the same
 * elements (observed against Guava, `standins`). One class here for every size (Guava's picks others for 0 and 1).
 */
public abstract class ImmutableList<E> extends ImmutableCollection<E> implements java.util.List<E>, java.util.RandomAccess {
    ImmutableList() {}

    @SuppressWarnings("unchecked")
    static <E> ImmutableList<E> copyOfArray(Object[] elements) {
        for (int i = 0; i < elements.length; i++) if (elements[i] == null) throw new NullPointerException("at index " + i);
        return new RegularImmutableList<>(elements.clone());
    }

    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    @Override
    public UnmodifiableIterator<E> iterator() {
        return listIterator();
    }

    public UnmodifiableListIterator<E> listIterator() {
        return listIterator(0);
    }

    public UnmodifiableListIterator<E> listIterator(int index) {
        return new AbstractIndexedListIterator<E>(size(), index) {
            @Override
            protected E get(int index) {
                return ImmutableList.this.get(index);
            }
        };
    }

    @Override
    public int indexOf(Object object) {
        if (object == null) return -1;
        for (int i = 0; i < size(); i++) if (object.equals(get(i))) return i;
        return -1;
    }

    @Override
    public int lastIndexOf(Object object) {
        if (object == null) return -1;
        for (int i = size() - 1; i >= 0; i--) if (object.equals(get(i))) return i;
        return -1;
    }

    @Override
    public boolean contains(Object object) {
        return indexOf(object) >= 0;
    }

    @Override
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
        // Observed: Guava's checks, the start's first, then the end's, then their order.
        int size = size();
        if (fromIndex < 0 || fromIndex > size) throw new IndexOutOfBoundsException(position("start index", fromIndex, size));
        if (toIndex < 0 || toIndex > size) throw new IndexOutOfBoundsException(position("end index", toIndex, size));
        if (toIndex < fromIndex) throw new IndexOutOfBoundsException("end index (" + toIndex + ") must not be less than start index (" + fromIndex + ")");
        Object[] part = new Object[toIndex - fromIndex];
        for (int i = fromIndex; i < toIndex; i++) part[i - fromIndex] = get(i);
        return new RegularImmutableList<>(part);
    }

    private static String position(String what, int index, int size) {
        return index < 0 ? what + " (" + index + ") must not be negative" : what + " (" + index + ") must not be greater than size (" + size + ")";
    }

    @Override
    public final boolean addAll(int index, java.util.Collection<? extends E> newElements) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final E remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final ImmutableList<E> asList() {
        return this;
    }

    /** Observed: equal to any list of the same elements in the same order. */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof java.util.List<?> other)) return false;
        int size = size();
        if (size != other.size()) return false;
        java.util.Iterator<?> i = other.iterator();
        for (int k = 0; k < size; k++) if (!get(k).equals(i.next())) return false;
        return true;
    }

    /** Observed: a list's hash. */
    @Override
    public int hashCode() {
        int hashCode = 1;
        for (int k = 0; k < size(); k++) hashCode = 31 * hashCode + get(k).hashCode();
        return hashCode;
    }

    /** The stand-in for ImmutableList.Builder: its elements in order, null refused. */
    public static final class Builder<E> extends ImmutableCollection.ArrayBasedBuilder<E> {
        public Builder() {
            super(4);
        }

        @Override
        public Builder<E> add(E element) {
            super.add(element);
            return this;
        }

        @Override
        public Builder<E> addAll(Iterable<? extends E> elements) {
            super.addAll(elements);
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Builder<E> add(E... elements) {
            super.add(elements);
            return this;
        }

        @Override
        public Builder<E> addAll(java.util.Iterator<? extends E> elements) {
            super.addAll(elements);
            return this;
        }

        @Override
        public ImmutableList<E> build() {
            return new RegularImmutableList<>(java.util.Arrays.copyOf(contents, size));
        }
    }
}
