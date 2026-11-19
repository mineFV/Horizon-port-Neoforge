package net.minecraft.network;

/**
 * The stand-in for the game's FriendlyByteBuf: the type of the network's buffers, with what the mod reads and writes
 * of them. The mod's payloads to clients are dropped before they are encoded (PacketDistributor): reading or writing a
 * buffer isn't modelled, and refused.
 */
public class FriendlyByteBuf extends io.netty.buffer.ByteBuf implements net.neoforged.neoforge.common.extensions.IFriendlyByteBufExtension {
    public FriendlyByteBuf(io.netty.buffer.ByteBuf source) {}

    static UnsupportedOperationException refused() {
        return new UnsupportedOperationException("network buffers aren't modelled (horizon-port-neoforge)");
    }

    @Override
    public FriendlyByteBuf writeBoolean(boolean value) {
        throw refused();
    }

    @Override
    public boolean readBoolean() {
        throw refused();
    }

    @Override
    public FriendlyByteBuf writeByte(int value) {
        throw refused();
    }

    @Override
    public byte readByte() {
        throw refused();
    }

    @Override
    public FriendlyByteBuf writeDouble(double value) {
        throw refused();
    }

    @Override
    public double readDouble() {
        throw refused();
    }

    public int readVarInt() {
        throw refused();
    }

    public FriendlyByteBuf writeVarInt(int value) {
        throw refused();
    }

    public long readVarLong() {
        throw refused();
    }

    public FriendlyByteBuf writeVarLong(long value) {
        throw refused();
    }

    public net.minecraft.core.BlockPos readBlockPos() {
        throw refused();
    }

    public FriendlyByteBuf writeBlockPos(net.minecraft.core.BlockPos pos) {
        throw refused();
    }

    public java.util.UUID readUUID() {
        throw refused();
    }

    public FriendlyByteBuf writeUUID(java.util.UUID uuid) {
        throw refused();
    }

    public <T extends Enum<T>> T readEnum(Class<T> type) {
        throw refused();
    }

    public FriendlyByteBuf writeEnum(Enum<?> value) {
        throw refused();
    }

    public <T> net.minecraft.resources.ResourceKey<T> readResourceKey(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> registry) {
        throw refused();
    }

    public void writeResourceKey(net.minecraft.resources.ResourceKey<?> key) {
        throw refused();
    }

    public net.minecraft.world.phys.BlockHitResult readBlockHitResult() {
        throw refused();
    }

    public void writeBlockHitResult(net.minecraft.world.phys.BlockHitResult hit) {
        throw refused();
    }

    public <T> java.util.List<T> readList(net.minecraft.network.codec.StreamDecoder<? super FriendlyByteBuf, T> decoder) {
        throw refused();
    }

    public <T, C extends java.util.Collection<T>> C readCollection(java.util.function.IntFunction<C> maker, net.minecraft.network.codec.StreamDecoder<? super FriendlyByteBuf, T> decoder) {
        throw refused();
    }

    public <T> void writeCollection(java.util.Collection<T> values, net.minecraft.network.codec.StreamEncoder<? super FriendlyByteBuf, T> encoder) {
        throw refused();
    }

    public <T> T readNullable(net.minecraft.network.codec.StreamDecoder<? super FriendlyByteBuf, T> decoder) {
        throw refused();
    }

    public <T> void writeNullable(T value, net.minecraft.network.codec.StreamEncoder<? super FriendlyByteBuf, T> encoder) {
        throw refused();
    }

    @Override
    public boolean release() {
        throw refused();
    }

    @Override
    public boolean equals(Object obj) {
        throw refused();
    }

    @Override
    public int hashCode() {
        throw refused();
    }

    @Override
    public String toString() {
        throw refused();
    }

    @Override
    public int compareTo(io.netty.buffer.ByteBuf buffer) {
        throw refused();
    }
}
