package it.unimi.dsi.fastutil.ints;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * The stand-in for fastutil's IntCollection: a collection of ints, whose boxed methods take an Integer (null is in no
 * such collection; another object is a ClassCastException).
 */
public interface IntCollection extends Collection<Integer>, IntIterable {
    @Override
    IntIterator iterator();

    boolean add(int key);

    boolean contains(int key);

    boolean rem(int key);

    @Override
    default boolean add(Integer key) {
        return add(key.intValue());
    }

    @Override
    default boolean contains(Object key) {
        if (key == null) return false;
        return contains(((Integer) key).intValue());
    }

    @Override
    default boolean remove(Object key) {
        if (key == null) return false;
        return rem(((Integer) key).intValue());
    }

    int[] toIntArray();

    default int[] toIntArray(int[] a) {
        return toArray(a);
    }

    int[] toArray(int[] a);

    boolean addAll(IntCollection c);

    boolean containsAll(IntCollection c);

    boolean removeAll(IntCollection c);

    @Override
    default boolean removeIf(Predicate<? super Integer> filter) {
        return removeIf(filter instanceof java.util.function.IntPredicate ? (java.util.function.IntPredicate) filter : (java.util.function.IntPredicate) key -> filter.test(Integer.valueOf(key)));
    }

    default boolean removeIf(java.util.function.IntPredicate filter) {
        Objects.requireNonNull(filter);
        boolean removed = false;
        IntIterator each = iterator();
        while (each.hasNext()) {
            if (filter.test(each.nextInt())) {
                each.remove();
                removed = true;
            }
        }
        return removed;
    }

    boolean retainAll(IntCollection c);
}
