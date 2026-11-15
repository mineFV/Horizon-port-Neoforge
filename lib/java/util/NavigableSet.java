package java.util;

/** The model of java.util.NavigableSet: its views (descendingSet, the sub-sets) aren't modelled. */
public interface NavigableSet<E> extends SortedSet<E> {
    E lower(E e);

    E floor(E e);

    E ceiling(E e);

    E higher(E e);

    E pollFirst();

    E pollLast();
}
