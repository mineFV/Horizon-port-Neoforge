package net.minecraft.network.protocol.common.custom;

/**
 * The stand-in for the game's CustomPacketPayload: a payload and its type, an id (observed, `standins`). Sending and
 * receiving payloads isn't modelled.
 */
public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    /** A codec of a payload, made as the game makes it: it doesn't run (the network isn't modelled). */
    static <B extends io.netty.buffer.ByteBuf, T extends CustomPacketPayload> net.minecraft.network.codec.StreamCodec<B, T> codec(net.minecraft.network.codec.StreamMemberEncoder<B, T> encoder,
            net.minecraft.network.codec.StreamDecoder<B, T> decoder) {
        return net.minecraft.network.codec.StreamCodec.ofMember(encoder, decoder);
    }

    /** Observed: a type of the game's namespace. */
    static <T extends CustomPacketPayload> Type<T> createType(String id) {
        return new Type<>(net.minecraft.resources.ResourceLocation.withDefaultNamespace(id));
    }

    /** The stand-in for CustomPacketPayload.Type: a payload type, its id. */
    record Type<T extends CustomPacketPayload>(net.minecraft.resources.ResourceLocation id) {
    }
}
