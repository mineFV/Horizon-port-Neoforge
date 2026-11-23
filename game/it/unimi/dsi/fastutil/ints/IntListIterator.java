package it.unimi.dsi.fastutil.ints;

/** The stand-in for fastutil's IntListIterator: changing the list through it is refused unless the list's allows it. */
public interface IntListIterator extends IntBidirectionalIterator, java.util.ListIterator<Integer> {
    default void set(int k) {
        throw new UnsupportedOperationException();
    }

    default void add(int k) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    default void set(Integer k) {
        set(k.intValue());
    }

    @Override
    default void add(Integer k) {
        add(k.intValue());
    }

    @Override
    default Integer next() {
        return Integer.valueOf(nextInt());
    }

    @Override
    default Integer previous() {
        return Integer.valueOf(previousInt());
    }
}
