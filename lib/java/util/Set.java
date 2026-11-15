package java.util;

/**
 * The model of java.util.Set. The sets Set.of and Set.copyOf give iterate in the order of their elements as given;
 * the JDK's order changes from one run to the next (it is salted at random), so no program can count on it.
 */
public interface Set<E> extends Collection<E> {
    int size();

    boolean isEmpty();

    boolean contains(Object o);

    Iterator<E> iterator();

    Object[] toArray();

    <T> T[] toArray(T[] a);

    boolean add(E e);

    boolean remove(Object o);

    boolean containsAll(Collection<?> c);

    boolean addAll(Collection<? extends E> c);

    boolean retainAll(Collection<?> c);

    boolean removeAll(Collection<?> c);

    void clear();

    boolean equals(Object o);

    int hashCode();

    @SuppressWarnings("unchecked")
    static <E> Set<E> of() {
        return (Set<E>) ImmutableCollections.EMPTY_SET;
    }

    static <E> Set<E> of(E e1) {
        return ImmutableCollections.setOf(new Object[] {e1});
    }

    static <E> Set<E> of(E e1, E e2) {
        return ImmutableCollections.setOf(new Object[] {e1, e2});
    }

    static <E> Set<E> of(E e1, E e2, E e3) {
        return ImmutableCollections.setOf(new Object[] {e1, e2, e3});
    }

    static <E> Set<E> of(E e1, E e2, E e3, E e4) {
        return ImmutableCollections.setOf(new Object[] {e1, e2, e3, e4});
    }

    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5) {
        return ImmutableCollections.setOf(new Object[] {e1, e2, e3, e4, e5});
    }

    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        return ImmutableCollections.setOf(new Object[] {e1, e2, e3, e4, e5, e6});
    }

    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return ImmutableCollections.setOf(new Object[] {e1, e2, e3, e4, e5, e6, e7});
    }

    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return ImmutableCollections.setOf(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8});
    }

    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return ImmutableCollections.setOf(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8, e9});
    }

    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return ImmutableCollections.setOf(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8, e9, e10});
    }

    @SafeVarargs
    static <E> Set<E> of(E... elements) {
        return ImmutableCollections.setOf(elements);
    }

    @SuppressWarnings("unchecked")
    static <E> Set<E> copyOf(Collection<? extends E> coll) {
        if (coll instanceof ImmutableCollections.AbstractImmutableSet) return (Set<E>) coll;
        return ImmutableCollections.setOf(new HashSet<>(coll).toArray());
    }

    @Override
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT);
    }
}
