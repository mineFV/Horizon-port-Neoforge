package com.google.common.collect;

/**
 * The stand-in for Guava's AbstractTable: a table's views made once (cellSet, values), which follow it; its equality
 * (the same cells), hash (its cells') and text (its rows'). Observed against Guava 20 (`standins`,
 * probe/GuavaProbe.java).
 */
abstract class AbstractTable<R, C, V> implements Table<R, C, V> {
    private transient java.util.Set<Table.Cell<R, C, V>> cellSet;
    private transient java.util.Collection<V> values;

    AbstractTable() {}

    abstract java.util.Iterator<Table.Cell<R, C, V>> cellIterator();

    /** Its rows, as rowMap shows them. */
    abstract java.util.Map<R, java.util.Map<C, V>> horizonRows();

    @Override
    public java.util.Set<Table.Cell<R, C, V>> cellSet() {
        java.util.Set<Table.Cell<R, C, V>> result = cellSet;
        return result == null ? cellSet = new CellSet() : result;
    }

    @Override
    public java.util.Collection<V> values() {
        java.util.Collection<V> result = values;
        return result == null ? values = new Values() : result;
    }

    java.util.Iterator<V> valuesIterator() {
        return new TransformedIterator<Table.Cell<R, C, V>, V>(cellSet().iterator()) {
            @Override
            V transform(Table.Cell<R, C, V> cell) {
                return cell.getValue();
            }
        };
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof Table<?, ?, ?> that && cellSet().equals(that.cellSet()));
    }

    @Override
    public int hashCode() {
        return cellSet().hashCode();
    }

    @Override
    public String toString() {
        return horizonRows().toString();
    }

    /** The cells: one is in it where the table has its row and column with its value; removing it removes the cell. */
    class CellSet extends java.util.AbstractSet<Table.Cell<R, C, V>> {
        CellSet() {}

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Table.Cell<?, ?, ?> cell)) return false;
            java.util.Map<C, V> row = cell.getRowKey() == null ? null : horizonRows().get(cell.getRowKey());
            return row != null && cell.getColumnKey() != null && row.containsKey(cell.getColumnKey())
                    && java.util.Objects.equals(row.get(cell.getColumnKey()), cell.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!contains(o)) return false;
            Table.Cell<?, ?, ?> cell = (Table.Cell<?, ?, ?>) o;
            AbstractTable.this.remove(cell.getRowKey(), cell.getColumnKey());
            return true;
        }

        @Override
        public void clear() {
            AbstractTable.this.clear();
        }

        @Override
        public java.util.Iterator<Table.Cell<R, C, V>> iterator() {
            return cellIterator();
        }

        @Override
        public int size() {
            return AbstractTable.this.size();
        }
    }

    /** The values, as the cells come. */
    class Values extends java.util.AbstractCollection<V> {
        Values() {}

        @Override
        public java.util.Iterator<V> iterator() {
            return valuesIterator();
        }

        @Override
        public boolean contains(Object o) {
            return containsValue(o);
        }

        @Override
        public void clear() {
            AbstractTable.this.clear();
        }

        @Override
        public int size() {
            return AbstractTable.this.size();
        }
    }
}
