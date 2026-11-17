package com.mojang.datafixers.kinds;

/** The stand-in for DataFixerUpper's Functor: its type. */
public interface Functor<F extends K1, Mu extends Functor.Mu> extends Kind1<F, Mu> {
    interface Mu extends Kind1.Mu {
    }
}
