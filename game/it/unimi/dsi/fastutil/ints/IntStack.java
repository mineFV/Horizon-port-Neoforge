package it.unimi.dsi.fastutil.ints;

/** The stand-in for fastutil's IntStack: a stack of ints, boxed where Stack's methods are asked. */
public interface IntStack extends it.unimi.dsi.fastutil.Stack<Integer> {
    void push(int k);

    int popInt();

    int topInt();

    int peekInt(int i);

    @Override
    default void push(Integer o) {
        push(o.intValue());
    }

    @Override
    default Integer pop() {
        return Integer.valueOf(popInt());
    }

    @Override
    default Integer top() {
        return Integer.valueOf(topInt());
    }

    @Override
    default Integer peek(int i) {
        return Integer.valueOf(peekInt(i));
    }
}
