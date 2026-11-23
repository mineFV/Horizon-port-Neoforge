package java.util;

/**
 * The model of java.util.ListIterator: the interface alone, for the lists of the game's libraries that give one
 * (fastutil's IntList); the model's own lists don't (List.listIterator isn't modelled).
 */
public interface ListIterator<E> extends Iterator<E> {
    @Override
    boolean hasNext();

    @Override
    E next();

    boolean hasPrevious();

    E previous();

    int nextIndex();

    int previousIndex();

    @Override
    void remove();

    void set(E e);

    void add(E e);
}
