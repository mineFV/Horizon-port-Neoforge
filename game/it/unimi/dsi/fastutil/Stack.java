package it.unimi.dsi.fastutil;

/** The stand-in for fastutil's Stack: push, pop, and peeking from the top (which a stack may refuse). */
public interface Stack<K> {
    void push(K o);

    K pop();

    boolean isEmpty();

    default K top() {
        return peek(0);
    }

    default K peek(int i) {
        throw new UnsupportedOperationException();
    }
}
