package it.unimi.dsi.fastutil.chars;

/** The stand-in for fastutil's CharCollection: a collection of chars, its boxed methods unboxing them. */
public interface CharCollection extends java.util.Collection<Character>, CharIterable {
    @Override
    CharIterator iterator();

    boolean add(char key);

    boolean contains(char key);

    boolean rem(char key);

    @Override
    default boolean add(Character key) {
        return add(key.charValue());
    }

    @Override
    default boolean contains(Object key) {
        if (key == null) return false;
        return contains(((Character) key).charValue());
    }

    @Override
    default boolean remove(Object key) {
        if (key == null) return false;
        return rem(((Character) key).charValue());
    }
}
