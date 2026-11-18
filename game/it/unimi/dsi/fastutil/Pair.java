package it.unimi.dsi.fastutil;

/** The stand-in for fastutil's Pair: a left and a right value. */
public interface Pair<L, R> {
    L left();

    R right();

    default Pair<L, R> left(L l) {
        throw new UnsupportedOperationException();
    }

    default Pair<L, R> right(R r) {
        throw new UnsupportedOperationException();
    }

    default L first() {
        return left();
    }

    default R second() {
        return right();
    }

    default Pair<L, R> first(L l) {
        return left(l);
    }

    default Pair<L, R> second(R r) {
        return right(r);
    }

    default L key() {
        return first();
    }

    default R value() {
        return second();
    }

    default Pair<L, R> key(L l) {
        return left(l);
    }

    default Pair<L, R> value(R r) {
        return right(r);
    }
}
