package it.unimi.dsi.fastutil.objects;

/** The stand-in for fastutil's ObjectListIterator: changing the list through it is refused unless the list's allows it. */
public interface ObjectListIterator<K> extends ObjectBidirectionalIterator<K>, java.util.ListIterator<K> {
    @Override
    default void set(K k) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void add(K k) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void remove() {
        throw new UnsupportedOperationException();
    }
}
