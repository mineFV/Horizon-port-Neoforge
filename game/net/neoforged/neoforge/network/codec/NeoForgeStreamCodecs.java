package net.neoforged.neoforge.network.codec;

import java.util.function.Function;
import net.minecraft.network.codec.HorizonStreamCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The stand-in for NeoForge's NeoForgeStreamCodecs: codecs of the network, made as NeoForge makes them; they don't run. */
public final class NeoForgeStreamCodecs {
    private NeoForgeStreamCodecs() {}

    public static <B, V> StreamCodec<B, V> lazy(java.util.function.Supplier<StreamCodec<B, V>> codec) {
        return new HorizonStreamCodecs.Opaque<>("lazy");
    }

    public static <B extends net.minecraft.network.FriendlyByteBuf, V extends Enum<V>> StreamCodec<B, V> enumCodec(Class<V> type) {
        return new HorizonStreamCodecs.Opaque<>("enumCodec");
    }

    public static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4, StreamCodec<? super B, T5> c5, Function<C, T5> g5,
            StreamCodec<? super B, T6> c6, Function<C, T6> g6, StreamCodec<? super B, T7> c7, Function<C, T7> g7, com.mojang.datafixers.util.Function7<T1, T2, T3, T4, T5, T6, T7, C> make) {
        return new HorizonStreamCodecs.Opaque<>("composite");
    }
}
