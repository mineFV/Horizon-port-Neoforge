package net.minecraft.core;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * The stand-in for the game's NonNullList: a list that refuses null where it is given a value (a
 * NullPointerException, "The validated object is null"), on a list of its own: empty or with a capacity (an
 * ArrayList), an array of a size filled with a default, the elements given (their array itself), or a copy that
 * can't be changed. clear
 * puts the default back in every place where there is one; without one, it empties the list where its list lets it.
 * Observed (the probe's ListProbe).
 */
public class NonNullList<E> extends AbstractList<E> {
    private final List<E> list;
    private final E defaultValue;

    protected NonNullList(List<E> list, E defaultValue) {
        this.list = list;
        this.defaultValue = defaultValue;
    }

    public static <E> NonNullList<E> create() {
        return new NonNullList<>(new ArrayList<>(), null);
    }

    /** Observed: a negative capacity is Guava's error. */
    public static <E> NonNullList<E> createWithCapacity(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("initialArraySize cannot be negative but was: " + capacity);
        return new NonNullList<>(new ArrayList<>(capacity), null);
    }

    @SuppressWarnings("unchecked")
    public static <E> NonNullList<E> withSize(int size, E defaultValue) {
        notNull(defaultValue);
        Object[] values = new Object[size];
        Arrays.fill(values, defaultValue);
        return new NonNullList<>((List<E>) Arrays.asList(values), defaultValue);
    }

    @SafeVarargs
    public static <E> NonNullList<E> of(E defaultValue, E... elements) {
        return new NonNullList<>(Arrays.asList(elements), defaultValue);
    }

    /** Observed: on a list that can't be changed (List.copyOf's: no null in it either). */
    public static <E> NonNullList<E> copyOf(Collection<? extends E> collection) {
        return new NonNullList<>(List.copyOf(collection), null);
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @Override
    public E set(int index, E value) {
        notNull(value);
        return list.set(index, value);
    }

    @Override
    public void add(int index, E value) {
        notNull(value);
        list.add(index, value);
    }

    @Override
    public E remove(int index) {
        return list.remove(index);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public void clear() {
        if (defaultValue == null) {
            super.clear();
            return;
        }
        for (int i = 0; i < size(); i++) set(i, defaultValue);
    }

    private static void notNull(Object value) {
        if (value == null) throw new NullPointerException("The validated object is null");
    }
}
