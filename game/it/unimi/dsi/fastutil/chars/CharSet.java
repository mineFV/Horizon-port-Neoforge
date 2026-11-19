package it.unimi.dsi.fastutil.chars;

/** The stand-in for fastutil's CharSet. */
public interface CharSet extends CharCollection, java.util.Set<Character> {
    @Override
    CharIterator iterator();

    default boolean remove(char k) {
        return rem(k);
    }

    @Override
    default boolean remove(Object o) {
        return CharCollection.super.remove(o);
    }

    @Override
    default boolean add(Character o) {
        return CharCollection.super.add(o);
    }

    @Override
    default boolean contains(Object o) {
        return CharCollection.super.contains(o);
    }
}
