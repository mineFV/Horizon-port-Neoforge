package net.minecraft.network.codec;

/** The stand-in for the game's StreamEncoder: its type (the network isn't modelled). */
public interface StreamEncoder<O, T> {
    void encode(O output, T value);
}
