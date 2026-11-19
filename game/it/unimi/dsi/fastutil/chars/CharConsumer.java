package it.unimi.dsi.fastutil.chars;

/** The stand-in for fastutil's CharConsumer: a consumer of chars, and of boxed ones and ints as fastutil gives them. */
@FunctionalInterface
public interface CharConsumer extends java.util.function.Consumer<Character>, java.util.function.IntConsumer {
    void accept(char t);

    @Override
    default void accept(int t) {
        accept(HorizonChars.safeIntToChar(t));
    }

    @Override
    default void accept(Character t) {
        accept(t.charValue());
    }
}
