package net.minecraft.network.codec;

/** The stand-in for the game's StreamMemberEncoder: its type (the network isn't modelled). */
@FunctionalInterface
public interface StreamMemberEncoder<O, T> {
    void encode(T value, O output);
}
