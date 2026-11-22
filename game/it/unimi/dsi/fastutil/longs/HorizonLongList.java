package it.unimi.dsi.fastutil.longs;

/** Not fastutil's: a growing list of longs, the keys a removal behind a set's iterator moves before it (LongOpenHashSet). */
final class HorizonLongList {
    private long[] items = new long[2];
    private int size;

    void add(long c) {
        if (size == items.length) items = java.util.Arrays.copyOf(items, size * 2);
        items[size++] = c;
    }

    long get(int i) {
        return items[i];
    }
}
