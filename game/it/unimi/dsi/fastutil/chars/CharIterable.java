package it.unimi.dsi.fastutil.chars;

/** The stand-in for fastutil's CharIterable. */
public interface CharIterable extends Iterable<Character> {
    @Override
    CharIterator iterator();
}
