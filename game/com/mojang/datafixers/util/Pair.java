package com.mojang.datafixers.util;

/** The stand-in for DataFixerUpper's Pair: two values (observed, `standins`). */
public class Pair<F, S> implements com.mojang.datafixers.kinds.App<Pair.Mu<S>, F> {
    public static final class Mu<S> implements com.mojang.datafixers.kinds.K1 {}

    private final F first;
    private final S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public static <F, S> Pair<F, S> of(F first, S second) {
        return new Pair<>(first, second);
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }

    public Pair<S, F> swap() {
        return of(second, first);
    }

    public <F2> Pair<F2, S> mapFirst(java.util.function.Function<? super F, ? extends F2> function) {
        return of(function.apply(first), second);
    }

    public <S2> Pair<F, S2> mapSecond(java.util.function.Function<? super S, ? extends S2> function) {
        return of(first, function.apply(second));
    }

    /** Observed: "(" first ", " second ")". */
    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    /** Observed: both values equal (Objects.equals). */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Pair<?, ?> other)) return false;
        return java.util.Objects.equals(first, other.first) && java.util.Objects.equals(second, other.second);
    }

    /** Observed: the two values' hashes as Arrays.hashCode folds them. */
    @Override
    public int hashCode() {
        return 31 * (31 + java.util.Objects.hashCode(first)) + java.util.Objects.hashCode(second);
    }
}
