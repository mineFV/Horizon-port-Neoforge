package com.google.common.collect;

/**
 * The stand-in for Guava's StandardTable: a map of rows, each a map of its columns (HashBasedTable's linked: in the
 * order they came), a row gone once it has no cell left; null keys and values refused, asked of with null answering
 * nothing. cellSet and values (AbstractTable's views) remove from it through their iterators.
 * Observed against Guava 20 (`standins`, probe/GuavaProbe.java). row's view isn't here: it refuses, saying so.
 */
class StandardTable<R, C, V> extends AbstractTable<R, C, V> implements java.io.Serializable {
    final java.util.Map<R, java.util.Map<C, V>> backingMap;
    private final java.util.function.Supplier<? extends java.util.Map<C, V>> rows;

    StandardTable(java.util.Map<R, java.util.Map<C, V>> backingMap, java.util.function.Supplier<? extends java.util.Map<C, V>> rows) {
        this.backingMap = backingMap;
        this.rows = rows;
    }

    @Override
    java.util.Map<R, java.util.Map<C, V>> horizonRows() {
        return backingMap;
    }

    @Override
    public boolean contains(Object rowKey, Object columnKey) {
        if (rowKey == null || columnKey == null) return false;
        java.util.Map<C, V> row = backingMap.get(rowKey);
        return row != null && row.containsKey(columnKey);
    }

    @Override
    public boolean containsColumn(Object columnKey) {
        if (columnKey == null) return false;
        for (java.util.Map<C, V> row : backingMap.values()) {
            if (row.containsKey(columnKey)) return true;
        }
        return false;
    }

    @Override
    public boolean containsRow(Object rowKey) {
        return rowKey != null && backingMap.containsKey(rowKey);
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) return false;
        for (java.util.Map<C, V> row : backingMap.values()) {
            if (row.containsValue(value)) return true;
        }
        return false;
    }

    @Override
    public V get(Object rowKey, Object columnKey) {
        if (rowKey == null || columnKey == null) return null;
        java.util.Map<C, V> row = backingMap.get(rowKey);
        return row == null ? null : row.get(columnKey);
    }

    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    @Override
    public int size() {
        int size = 0;
        for (java.util.Map<C, V> row : backingMap.values()) size += row.size();
        return size;
    }

    @Override
    public void clear() {
        backingMap.clear();
    }

    @Override
    public V put(R rowKey, C columnKey, V value) {
        if (rowKey == null || columnKey == null || value == null) throw new NullPointerException();
        java.util.Map<C, V> row = backingMap.get(rowKey);
        if (row == null) {
            row = rows.get();
            backingMap.put(rowKey, row);
        }
        return row.put(columnKey, value);
    }

    @Override
    public V remove(Object rowKey, Object columnKey) {
        if (rowKey == null || columnKey == null) return null;
        java.util.Map<C, V> row = backingMap.get(rowKey);
        if (row == null) return null;
        V value = row.remove(columnKey);
        if (row.isEmpty()) backingMap.remove(rowKey);
        return value;
    }

    /** Refused: a row's live view isn't modelled. */
    @Override
    public java.util.Map<C, V> row(R rowKey) {
        throw new UnsupportedOperationException("a Guava table's row view isn't modelled (horizon-port-neoforge)");
    }

    @Override
    public java.util.Set<Table.Cell<R, C, V>> cellSet() {
        return super.cellSet();
    }

    @Override
    public java.util.Collection<V> values() {
        return super.values();
    }

    @Override
    java.util.Iterator<Table.Cell<R, C, V>> cellIterator() {
        return new CellIterator();
    }

    /** The cells, row after row, each row's in its order; removing one removes its row once it is empty. */
    private class CellIterator implements java.util.Iterator<Table.Cell<R, C, V>> {
        final java.util.Iterator<java.util.Map.Entry<R, java.util.Map<C, V>>> rowIterator = backingMap.entrySet().iterator();
        java.util.Map.Entry<R, java.util.Map<C, V>> rowEntry;
        java.util.Iterator<java.util.Map.Entry<C, V>> columnIterator = java.util.Collections.emptyIterator();

        @Override
        public boolean hasNext() {
            return rowIterator.hasNext() || columnIterator.hasNext();
        }

        @Override
        public Table.Cell<R, C, V> next() {
            if (!columnIterator.hasNext()) {
                rowEntry = rowIterator.next();
                columnIterator = rowEntry.getValue().entrySet().iterator();
            }
            java.util.Map.Entry<C, V> columnEntry = columnIterator.next();
            return Tables.immutableCell(rowEntry.getKey(), columnEntry.getKey(), columnEntry.getValue());
        }

        @Override
        public void remove() {
            columnIterator.remove();
            if (rowEntry.getValue().isEmpty()) rowIterator.remove();
        }
    }
}
