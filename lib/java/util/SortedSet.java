package java.util;

/** The model of java.util.SortedSet: headSet, tailSet and subSet aren't modelled. */
public interface SortedSet<E> extends Set<E>, SequencedCollection<E> {
    Comparator<? super E> comparator();

    E first();

    E last();

    default void addFirst(E e) {
        throw new UnsupportedOperationException();
    }

    default void addLast(E e) {
        throw new UnsupportedOperationException();
    }

    default E getFirst() {
        return this.first();
    }

    default E getLast() {
        return this.last();
    }

    default E removeFirst() {
        E e = this.first();
        this.remove(e);
        return e;
    }

    default E removeLast() {
        E e = this.last();
        this.remove(e);
        return e;
    }
}
