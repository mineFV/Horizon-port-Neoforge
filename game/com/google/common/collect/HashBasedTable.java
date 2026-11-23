package com.google.common.collect;

/**
 * The stand-in for Guava's HashBasedTable: a StandardTable whose rows and columns are linked hash maps, in the order
 * they came; observed against Guava 20 (`standins`, probe/GuavaProbe.java). Not here: create(int, int) and
 * create(Table).
 */
public class HashBasedTable<R, C, V> extends StandardTable<R, C, V> {
    private HashBasedTable() {
        super(new java.util.LinkedHashMap<>(), java.util.LinkedHashMap::new);
    }

    public static <R, C, V> HashBasedTable<R, C, V> create() {
        return new HashBasedTable<>();
    }

    @Override
    public boolean contains(Object rowKey, Object columnKey) {
        return super.contains(rowKey, columnKey);
    }

    @Override
    public boolean containsColumn(Object columnKey) {
        return super.containsColumn(columnKey);
    }

    @Override
    public boolean containsRow(Object rowKey) {
        return super.containsRow(rowKey);
    }

    @Override
    public boolean containsValue(Object value) {
        return super.containsValue(value);
    }

    @Override
    public V get(Object rowKey, Object columnKey) {
        return super.get(rowKey, columnKey);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public V remove(Object rowKey, Object columnKey) {
        return super.remove(rowKey, columnKey);
    }
}
