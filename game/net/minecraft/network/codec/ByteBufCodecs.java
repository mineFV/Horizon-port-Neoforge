package net.minecraft.network.codec;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/** The stand-in for the game's ByteBufCodecs: the network's codecs (HorizonStreamCodecs: they don't run). */
public interface ByteBufCodecs {
    StreamCodec<Object, Boolean> BOOL = new HorizonStreamCodecs.Opaque<>("BOOL");
    StreamCodec<Object, Byte> BYTE = new HorizonStreamCodecs.Opaque<>("BYTE");
    StreamCodec<Object, Short> SHORT = new HorizonStreamCodecs.Opaque<>("SHORT");
    StreamCodec<Object, Integer> INT = new HorizonStreamCodecs.Opaque<>("INT");
    StreamCodec<Object, Integer> VAR_INT = new HorizonStreamCodecs.Opaque<>("VAR_INT");
    StreamCodec<Object, Long> VAR_LONG = new HorizonStreamCodecs.Opaque<>("VAR_LONG");
    StreamCodec<Object, Float> FLOAT = new HorizonStreamCodecs.Opaque<>("FLOAT");
    StreamCodec<Object, Double> DOUBLE = new HorizonStreamCodecs.Opaque<>("DOUBLE");
    StreamCodec<Object, String> STRING_UTF8 = new HorizonStreamCodecs.Opaque<>("STRING_UTF8");
    StreamCodec<Object, net.minecraft.nbt.CompoundTag> TRUSTED_COMPOUND_TAG = new HorizonStreamCodecs.Opaque<>("TRUSTED_COMPOUND_TAG");

    static StreamCodec<Object, String> stringUtf8(int max) {
        return new HorizonStreamCodecs.Opaque<>("stringUtf8");
    }

    static <B, V> StreamCodec<B, Optional<V>> optional(StreamCodec<B, V> codec) {
        return new HorizonStreamCodecs.Opaque<>("optional");
    }

    static <B, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> maker, StreamCodec<? super B, V> codec) {
        return new HorizonStreamCodecs.Opaque<>("collection");
    }

    static <B, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> maker, StreamCodec<? super B, V> codec, int max) {
        return new HorizonStreamCodecs.Opaque<>("collection");
    }

    static <B, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(IntFunction<C> maker) {
        return codec -> new HorizonStreamCodecs.Opaque<>("collection");
    }

    static <B, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
        return codec -> new HorizonStreamCodecs.Opaque<>("list");
    }

    static <B, V> StreamCodec.CodecOperation<B, V, List<V>> list(int max) {
        return codec -> new HorizonStreamCodecs.Opaque<>("list");
    }

    static <B, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> maker, StreamCodec<? super B, K> key, StreamCodec<? super B, V> value) {
        return new HorizonStreamCodecs.Opaque<>("map");
    }

    static <B, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> maker, StreamCodec<? super B, K> key, StreamCodec<? super B, V> value, int max) {
        return new HorizonStreamCodecs.Opaque<>("map");
    }

    static <T> StreamCodec<Object, T> idMapper(IntFunction<T> byId, ToIntFunction<T> toId) {
        return new HorizonStreamCodecs.Opaque<>("idMapper");
    }

    static <T> StreamCodec<Object, T> registry(net.minecraft.resources.ResourceKey<?> registry) {
        return new HorizonStreamCodecs.Opaque<>("registry");
    }

    static <T> StreamCodec<Object, net.minecraft.core.Holder<T>> holderRegistry(net.minecraft.resources.ResourceKey<?> registry) {
        return new HorizonStreamCodecs.Opaque<>("holderRegistry");
    }
}
