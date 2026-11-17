package net.minecraft.network.codec;

/** The stand-in for the game's StreamDecoder: its type (the network isn't modelled). */
public interface StreamDecoder<I, T> {
    T decode(I input);
}
