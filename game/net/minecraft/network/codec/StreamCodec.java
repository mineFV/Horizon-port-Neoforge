package net.minecraft.network.codec;

import java.util.function.Function;

/**
 * The stand-in for the game's StreamCodec: a codec of the network, built as the game builds them; the network isn't
 * modelled, so none encodes or decodes (HorizonStreamCodecs says so if asked).
 */
public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {
    static <B, V> StreamCodec<B, V> of(StreamEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return new HorizonStreamCodecs.Opaque<>("of");
    }

    static <B, V> StreamCodec<B, V> ofMember(StreamMemberEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return new HorizonStreamCodecs.Opaque<>("ofMember");
    }

    static <B, V> StreamCodec<B, V> unit(V value) {
        return new HorizonStreamCodecs.Opaque<>("unit");
    }

    default <O> StreamCodec<B, O> apply(CodecOperation<B, V, O> operation) {
        return operation.apply(this);
    }

    default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> to, Function<? super O, ? extends V> from) {
        return new HorizonStreamCodecs.Opaque<>("map");
    }

    default <U> StreamCodec<B, U> dispatch(Function<? super U, ? extends V> type, Function<? super V, ? extends StreamCodec<? super B, ? extends U>> codec) {
        return new HorizonStreamCodecs.Opaque<>("dispatch");
    }

    @SuppressWarnings("unchecked")
    default <S extends B> StreamCodec<S, V> cast() {
        return (StreamCodec<S, V>) this;
    }

    static <B, C, T1> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, java.util.function.Function<T1, C> make) {
        return new HorizonStreamCodecs.Opaque<>("composite");
    }

    static <B, C, T1, T2> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, java.util.function.BiFunction<T1, T2, C> make) {
        return new HorizonStreamCodecs.Opaque<>("composite");
    }

    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3, com.mojang.datafixers.util.Function3<T1, T2, T3, C> make) {
        return new HorizonStreamCodecs.Opaque<>("composite");
    }

    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4, com.mojang.datafixers.util.Function4<T1, T2, T3, T4, C> make) {
        return new HorizonStreamCodecs.Opaque<>("composite");
    }

    static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4, StreamCodec<? super B, T5> c5, Function<C, T5> g5, com.mojang.datafixers.util.Function5<T1, T2, T3, T4, T5, C> make) {
        return new HorizonStreamCodecs.Opaque<>("composite");
    }

    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4, StreamCodec<? super B, T5> c5, Function<C, T5> g5, StreamCodec<? super B, T6> c6, Function<C, T6> g6, com.mojang.datafixers.util.Function6<T1, T2, T3, T4, T5, T6, C> make) {
        return new HorizonStreamCodecs.Opaque<>("composite");
    }

    @FunctionalInterface
    interface CodecOperation<B, S, T> {
        StreamCodec<B, T> apply(StreamCodec<B, S> codec);
    }
}
