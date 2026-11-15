package java.util;

import java.util.function.UnaryOperator;

/**
 * The model of java.util.List. listIterator, reversed and spliterator aren't modelled. replaceAll and sort go by index
 * where the JDK's defaults go through a list iterator: the same result, for a list whose get and set agree.
 */
public interface List<E> extends SequencedCollection<E> {
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

    boolean addAll(int index, Collection<? extends E> c);

    boolean removeAll(Collection<?> c);

    boolean retainAll(Collection<?> c);

    default void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        for (int i = 0, n = size(); i < n; i++) set(i, operator.apply(get(i)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default void sort(Comparator<? super E> c) {
        Object[] a = this.toArray();
        Arrays.sort(a, (Comparator) c);
        for (int i = 0; i < a.length; i++) set(i, (E) a[i]);
    }

    void clear();

    boolean equals(Object o);

    int hashCode();

    E get(int index);

    E set(int index, E element);

    void add(int index, E element);

    E remove(int index);

    int indexOf(Object o);

    int lastIndexOf(Object o);

    List<E> subList(int fromIndex, int toIndex);

    default void addFirst(E e) {
        this.add(0, e);
    }

    default void addLast(E e) {
        this.add(e);
    }

    default E getFirst() {
        if (this.isEmpty()) throw new NoSuchElementException();
        return this.get(0);
    }

    default E getLast() {
        if (this.isEmpty()) throw new NoSuchElementException();
        return this.get(this.size() - 1);
    }

    default E removeFirst() {
        if (this.isEmpty()) throw new NoSuchElementException();
        return this.remove(0);
    }

    default E removeLast() {
        if (this.isEmpty()) throw new NoSuchElementException();
        return this.remove(this.size() - 1);
    }

    @SuppressWarnings("unchecked")
    static <E> List<E> of() {
        return (List<E>) ImmutableCollections.EMPTY_LIST;
    }

    static <E> List<E> of(E e1) {
        return ImmutableCollections.listOf(new Object[] {e1});
    }

    static <E> List<E> of(E e1, E e2) {
        return ImmutableCollections.listOf(new Object[] {e1, e2});
    }

    static <E> List<E> of(E e1, E e2, E e3) {
        return ImmutableCollections.listOf(new Object[] {e1, e2, e3});
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4) {
        return ImmutableCollections.listOf(new Object[] {e1, e2, e3, e4});
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5) {
        return ImmutableCollections.listOf(new Object[] {e1, e2, e3, e4, e5});
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        return ImmutableCollections.listOf(new Object[] {e1, e2, e3, e4, e5, e6});
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return ImmutableCollections.listOf(new Object[] {e1, e2, e3, e4, e5, e6, e7});
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return ImmutableCollections.listOf(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8});
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return ImmutableCollections.listOf(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8, e9});
    }

    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return ImmutableCollections.listOf(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8, e9, e10});
    }

    @SafeVarargs
    static <E> List<E> of(E... elements) {
        return ImmutableCollections.listOf(elements);
    }

    static <E> List<E> copyOf(Collection<? extends E> coll) {
        return ImmutableCollections.listCopy(coll);
    }
}
