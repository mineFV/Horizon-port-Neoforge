package com.mojang.serialization;

/** The stand-in for DataFixerUpper's Keyable: a map codec's keys aren't modelled yet, and refused. */
public interface Keyable {
    default <T> java.util.stream.Stream<T> keys(DynamicOps<T> ops) {
        throw new UnsupportedOperationException("a map codec's keys aren't modelled (horizon-port-neoforge)");
    }
}
