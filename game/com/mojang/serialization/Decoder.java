package com.mojang.serialization;

/** The stand-in for DataFixerUpper's Decoder: decoding isn't modelled yet, and refused. */
public interface Decoder<A> {
    default <T> DataResult<com.mojang.datafixers.util.Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        throw new UnsupportedOperationException("decoding isn't modelled (horizon-port-neoforge)");
    }

    default <T> DataResult<A> parse(DynamicOps<T> ops, T input) {
        throw new UnsupportedOperationException("decoding isn't modelled (horizon-port-neoforge)");
    }
}
