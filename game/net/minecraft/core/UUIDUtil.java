package net.minecraft.core;

/** The stand-in for the game's UUIDUtil: its codecs, built as objects that don't run yet (HorizonCodecs). */
public final class UUIDUtil {
    private UUIDUtil() {}

    public static final com.mojang.serialization.Codec<java.util.UUID> CODEC = com.mojang.serialization.HorizonCodecs.game("UUIDUtil.CODEC");
    public static final com.mojang.serialization.Codec<java.util.UUID> STRING_CODEC = com.mojang.serialization.HorizonCodecs.game("UUIDUtil.STRING_CODEC");
    public static final com.mojang.serialization.Codec<java.util.UUID> AUTHLIB_CODEC = com.mojang.serialization.HorizonCodecs.game("UUIDUtil.AUTHLIB_CODEC");
    public static final com.mojang.serialization.Codec<java.util.UUID> LENIENT_CODEC = com.mojang.serialization.HorizonCodecs.game("UUIDUtil.LENIENT_CODEC");
    public static final net.minecraft.network.codec.StreamCodec<Object, java.util.UUID> STREAM_CODEC = new net.minecraft.network.codec.HorizonStreamCodecs.Opaque<>("UUIDUtil.STREAM_CODEC");
}
