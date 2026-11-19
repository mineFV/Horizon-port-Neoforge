package com.mojang.serialization;

/** The stand-in for DataFixerUpper's Encoder: encoding isn't modelled yet, and refused. */
public interface Encoder<A> {
    default <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        throw new UnsupportedOperationException("encoding isn't modelled (horizon-port-neoforge)");
    }

    default <T> DataResult<T> encodeStart(DynamicOps<T> ops, A input) {
        throw new UnsupportedOperationException("encoding isn't modelled (horizon-port-neoforge)");
    }
}
