package it.unimi.dsi.fastutil.ints;

import java.util.Comparator;

/** Not fastutil's: a stable sort of ints for the lists' sort, and an index-based list iterator for AbstractIntList. */
final class HorizonInts {
    private HorizonInts() {}

    /**
     * a[from, to) sorted: ascending when the comparator is null, else stably by it (a merge sort of a copy, written back
     * once sorted). A stable sort's result is the same for any consistent comparator, which fastutil's (a merge sort too)
     * gives; the pairs compared aren't the same.
     */
    static void sort(int[] a, int from, int to, Comparator<? super Integer> c) {
        if (c == null) {
            java.util.Arrays.sort(a, from, to);
            return;
        }
        int[] t = java.util.Arrays.copyOfRange(a, from, to);
        mergeSort(t, 0, t.length, c, new int[t.length]);
        System.arraycopy(t, 0, a, from, t.length);
    }

    private static void mergeSort(int[] a, int from, int to, Comparator<? super Integer> c, int[] tmp) {
        if (to - from < 2) return;
        int mid = (from + to) >>> 1;
        mergeSort(a, from, mid, c, tmp);
        mergeSort(a, mid, to, c, tmp);
        System.arraycopy(a, from, tmp, from, to - from);
        int i = from, j = mid, k = from;
        while (i < mid && j < to) a[k++] = c.compare(tmp[j], tmp[i]) < 0 ? tmp[j++] : tmp[i++];
        while (i < mid) a[k++] = tmp[i++];
        while (j < to) a[k++] = tmp[j++];
    }

    /** A list iterator of an IntList by its indices, as fastutil's index-based one goes (its class isn't fastutil's). */
    static final class ListIterator implements IntListIterator {
        private final IntList list;
        private int pos, last = -1;

        ListIterator(IntList list, int index) {
            this.list = list;
            this.pos = index;
        }

        @Override
        public boolean hasNext() {
            return pos < list.size();
        }

        @Override
        public boolean hasPrevious() {
            return pos > 0;
        }

        @Override
        public int nextInt() {
            if (!hasNext()) throw new java.util.NoSuchElementException();
            return list.getInt(last = pos++);
        }

        @Override
        public int previousInt() {
            if (!hasPrevious()) throw new java.util.NoSuchElementException();
            return list.getInt(last = --pos);
        }

        @Override
        public int nextIndex() {
            return pos;
        }

        @Override
        public int previousIndex() {
            return pos - 1;
        }

        @Override
        public void add(int k) {
            list.add(pos++, k);
            last = -1;
        }

        @Override
        public void set(int k) {
            if (last == -1) throw new IllegalStateException();
            list.set(last, k);
        }

        @Override
        public void remove() {
            if (last == -1) throw new IllegalStateException();
            list.removeInt(last);
            if (last < pos) pos--;
            last = -1;
        }
    }
}
