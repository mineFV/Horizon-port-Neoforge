package java.util;

/**
 * The model of java.util.SequencedCollection (Java 21). reversed() isn't modelled, nor getLast and removeLast, whose
 * defaults here go through it (List has defaults of its own for them).
 */
public interface SequencedCollection<E> extends Collection<E> {
    default void addFirst(E e) {
        throw new UnsupportedOperationException();
    }

    default void addLast(E e) {
        throw new UnsupportedOperationException();
    }

    default E getFirst() {
        return this.iterator().next();
    }

    default E removeFirst() {
        Iterator<E> it = this.iterator();
        E e = it.next();
        it.remove();
        return e;
    }
}
