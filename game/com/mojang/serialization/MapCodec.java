package com.mojang.serialization;

import java.util.function.Function;
import java.util.function.Supplier;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** The stand-in for DataFixerUpper's MapCodec: fields of a map, built as the game builds them (HorizonCodecs). */
public abstract class MapCodec<A> extends CompressorHolder implements MapDecoder<A>, MapEncoder<A> {
    public MapCodec() {}

    public final <O> RecordCodecBuilder<O, A> forGetter(Function<O, A> getter) {
        return RecordCodecBuilder.of(getter, this);
    }

    public MapCodec<A> fieldOf(String name) {
        return codec().fieldOf(name);
    }

    public MapCodec<A> withLifecycle(Lifecycle lifecycle) {
        return this;
    }

    public Codec<A> codec() {
        return new HorizonCodecs.OfMap<>(this);
    }

    public MapCodec<A> stable() {
        return withLifecycle(Lifecycle.stable());
    }

    public MapCodec<A> deprecated(int since) {
        return withLifecycle(Lifecycle.deprecated(since));
    }

    public <S> MapCodec<S> xmap(Function<? super A, ? extends S> to, Function<? super S, ? extends A> from) {
        return new HorizonCodecs.DerivedMap<>(this, "xmap", this + "[xmapped]");
    }

    public <S> MapCodec<S> flatXmap(Function<? super A, ? extends DataResult<? extends S>> to, Function<? super S, ? extends DataResult<? extends A>> from) {
        return new HorizonCodecs.DerivedMap<>(this, "flatXmap", this + "[flatXmapped]");
    }

    public MapCodec<A> validate(Function<A, DataResult<A>> check) {
        return new HorizonCodecs.DerivedMap<>(this, "validate", this + "[flatXmapped]");
    }

    public MapCodec<A> orElse(A value) {
        return new HorizonCodecs.DerivedMap<>(this, "orElse", this + "[mapResult OrElse[" + value + "]]");
    }

    public MapCodec<A> orElseGet(Supplier<? extends A> value) {
        return new HorizonCodecs.DerivedMap<>(this, "orElseGet", this + "[mapResult OrElseGet[" + value + "]]");
    }

    public MapCodec<A> setPartial(Supplier<A> value) {
        return new HorizonCodecs.DerivedMap<>(this, "setPartial", "SetPartial[" + value + "]");
    }

    public static <A> MapCodec<A> unit(A value) {
        return new HorizonCodecs.Unit<>(value);
    }

    public static <A> MapCodec<A> unit(Supplier<A> value) {
        return new HorizonCodecs.Unit<>(value.get());
    }
}
