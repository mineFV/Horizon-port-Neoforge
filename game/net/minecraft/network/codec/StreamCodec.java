package net.minecraft.network.codec;

/** The stand-in for the game's StreamCodec: its type alone; a component type keeps the one it is given. */
public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {
}
