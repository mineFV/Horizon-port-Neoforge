package it.unimi.dsi.fastutil.chars;

/** Not fastutil's: a growing list of chars, the keys a removal behind a set's iterator moves before it (CharOpenHashSet). */
final class HorizonCharList {
    private char[] items = new char[2];
    private int size;

    void add(char c) {
        if (size == items.length) items = java.util.Arrays.copyOf(items, size * 2);
        items[size++] = c;
    }

    char get(int i) {
        return items[i];
    }
}
