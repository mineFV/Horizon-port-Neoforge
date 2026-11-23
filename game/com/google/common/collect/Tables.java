package com.google.common.collect;

/**
 * The stand-in for Guava's Tables: a table's cells (immutableCell), equal when their row, column and value are, their
 * hash those three's and their text "(row,column)=value", observed against Guava 20 (`standins`,
 * probe/GuavaProbe.java). Not here: the other tables it makes (newCustomTable, transpose...).
 */
public final class Tables {
    private Tables() {}

    public static <R, C, V> Table.Cell<R, C, V> immutableCell(R rowKey, C columnKey, V value) {
        return new ImmutableCell<>(rowKey, columnKey, value);
    }

    abstract static class AbstractCell<R, C, V> implements Table.Cell<R, C, V> {
        AbstractCell() {}

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            return obj instanceof Table.Cell<?, ?, ?> other && java.util.Objects.equals(getRowKey(), other.getRowKey())
                    && java.util.Objects.equals(getColumnKey(), other.getColumnKey()) && java.util.Objects.equals(getValue(), other.getValue());
        }

        @Override
        public int hashCode() {
            return java.util.Arrays.hashCode(new Object[] {getRowKey(), getColumnKey(), getValue()});
        }

        @Override
        public String toString() {
            return "(" + getRowKey() + "," + getColumnKey() + ")=" + getValue();
        }
    }

    static final class ImmutableCell<R, C, V> extends AbstractCell<R, C, V> implements java.io.Serializable {
        private final R rowKey;
        private final C columnKey;
        private final V value;

        ImmutableCell(R rowKey, C columnKey, V value) {
            this.rowKey = rowKey;
            this.columnKey = columnKey;
            this.value = value;
        }

        @Override
        public R getRowKey() {
            return rowKey;
        }

        @Override
        public C getColumnKey() {
            return columnKey;
        }

        @Override
        public V getValue() {
            return value;
        }
    }
}
