package io.netty.buffer;

/**
 * The stand-in for Netty's ByteBuf: the type of the network's buffers, with what the mod reads and writes of them.
 * The mod's payloads to clients are dropped before they are encoded (PacketDistributor), so no buffer is written or
 * read here: a subclass's methods refuse.
 */
public abstract class ByteBuf implements io.netty.util.ReferenceCounted, Comparable<ByteBuf>, ByteBufConvertible {
    protected ByteBuf() {}

    public abstract ByteBuf writeBoolean(boolean value);

    public abstract boolean readBoolean();

    public abstract ByteBuf writeByte(int value);

    public abstract byte readByte();

    public abstract ByteBuf writeDouble(double value);

    public abstract double readDouble();

    @Override
    public ByteBuf asByteBuf() {
        return this;
    }

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    @Override
    public abstract int compareTo(ByteBuf buffer);
}
