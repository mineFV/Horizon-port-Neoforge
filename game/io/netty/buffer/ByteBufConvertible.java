package io.netty.buffer;

/** The stand-in for Netty's ByteBufConvertible: its type. */
public interface ByteBufConvertible {
    ByteBuf asByteBuf();
}
