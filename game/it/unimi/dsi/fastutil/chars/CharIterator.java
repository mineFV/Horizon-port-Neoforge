package it.unimi.dsi.fastutil.chars;

/** The stand-in for fastutil's CharIterator: an iterator of chars, boxing them for next(). */
public interface CharIterator extends java.util.PrimitiveIterator<Character, CharConsumer> {
    char nextChar();

    @Override
    default Character next() {
        return nextChar();
    }

    @Override
    default void forEachRemaining(CharConsumer action) {
        java.util.Objects.requireNonNull(action);
        while (hasNext()) action.accept(nextChar());
    }
}
