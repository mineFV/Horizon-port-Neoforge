package com.mojang.datafixers.kinds;

/** The stand-in for DataFixerUpper's Applicative: a value made a point of the constructor. */
public interface Applicative<F extends K1, Mu extends Applicative.Mu> extends Functor<F, Mu> {
    <A> App<F, A> point(A a);

    interface Mu extends Functor.Mu {
    }
}
