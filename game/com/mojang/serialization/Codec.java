package com.mojang.serialization;

/**
 * The stand-in for DataFixerUpper's Codec: its type alone. A component type keeps the codec it is given; what a codec
 * does isn't modelled, and none of its constants or builders is here yet.
 */
public interface Codec<A> extends Encoder<A>, Decoder<A> {
}
