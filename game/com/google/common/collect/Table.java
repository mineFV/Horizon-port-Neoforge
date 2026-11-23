package com.google.common.collect;

/** The stand-in for Guava's Table (20.0, shipped with the game): its methods; HashBasedTable is the one made. */
public interface Table<R, C, V> {
    boolean contains(Object rowKey, Object columnKey);

    boolean containsRow(Object rowKey);

    boolean containsColumn(Object columnKey);

    boolean containsValue(Object value);

    V get(Object rowKey, Object columnKey);

    boolean isEmpty();

    int size();

    boolean equals(Object obj);

    int hashCode();

    void clear();

    V put(R rowKey, C columnKey, V value);

    V remove(Object rowKey, Object columnKey);

    java.util.Map<C, V> row(R rowKey);

    java.util.Set<Cell<R, C, V>> cellSet();

    java.util.Collection<V> values();

    interface Cell<R, C, V> {
        R getRowKey();

        C getColumnKey();

        V getValue();

        boolean equals(Object obj);

        int hashCode();
    }
}
