package it.unimi.dsi.fastutil.ints;

import java.util.Comparator;
import java.util.List;

/**
 * The stand-in for fastutil's IntList: a list of ints, whose boxed methods take and give Integers (an index of an
 * Object is a ClassCastException unless it is an Integer, a NullPointerException for null). Sorting is stable, as
 * fastutil's; the elements given to a comparator, and how often, aren't fastutil's, nor what is left of the list when one
 * throws (here, the list as it was). Not here: its elements in bulk (getElements, setElements...), replaceAll and
 * unstableSort of its own, IntComparator, static factories.
 */
public interface IntList extends List<Integer>, Comparable<List<? extends Integer>>, IntCollection {
    @Override
    IntListIterator iterator();

    IntListIterator listIterator();

    IntListIterator listIterator(int index);

    @Override
    IntList subList(int from, int to);

    void size(int size);

    @Override
    boolean add(int key);

    void add(int index, int key);

    @Override
    default void add(int index, Integer key) {
        add(index, key.intValue());
    }

    boolean addAll(int index, IntCollection c);

    int set(int index, int k);

    int getInt(int index);

    int indexOf(int k);

    int lastIndexOf(int k);

    @Override
    default boolean contains(Object key) {
        return IntCollection.super.contains(key);
    }

    @Override
    default Integer get(int index) {
        return Integer.valueOf(getInt(index));
    }

    @Override
    default int indexOf(Object o) {
        return indexOf(((Integer) o).intValue());
    }

    @Override
    default int lastIndexOf(Object o) {
        return lastIndexOf(((Integer) o).intValue());
    }

    @Override
    default boolean add(Integer k) {
        return add(k.intValue());
    }

    int removeInt(int index);

    @Override
    default boolean remove(Object key) {
        return IntCollection.super.remove(key);
    }

    @Override
    default Integer remove(int index) {
        return Integer.valueOf(removeInt(index));
    }

    @Override
    default Integer set(int index, Integer k) {
        return Integer.valueOf(set(index, k.intValue()));
    }

    /** Sorted by the comparator, stably (null: ascending), then set back element by element. */
    @Override
    default void sort(Comparator<? super Integer> comparator) {
        int[] elements = toIntArray();
        HorizonInts.sort(elements, 0, elements.length, comparator);
        for (int i = 0; i < elements.length; i++) set(i, elements[i]);
    }
}
