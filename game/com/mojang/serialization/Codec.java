package com.mojang.serialization;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.UnboundedMapCodec;

/**
 * The stand-in for DataFixerUpper's Codec. Codecs are built as the game builds them (fields, lists, maps, records of
 * their getters and constructor: HorizonCodecs keeps what each was made of); encoding and decoding aren't modelled yet,
 * so neither is a call to them. Their texts are the game's where observed.
 */
public interface Codec<A> extends Encoder<A>, Decoder<A> {
    PrimitiveCodec<Boolean> BOOL = new HorizonCodecs.Primitive<>("Bool");
    PrimitiveCodec<Byte> BYTE = new HorizonCodecs.Primitive<>("Byte");
    PrimitiveCodec<Short> SHORT = new HorizonCodecs.Primitive<>("Short");
    PrimitiveCodec<Integer> INT = new HorizonCodecs.Primitive<>("Int");
    PrimitiveCodec<Long> LONG = new HorizonCodecs.Primitive<>("Long");
    PrimitiveCodec<Float> FLOAT = new HorizonCodecs.Primitive<>("Float");
    PrimitiveCodec<Double> DOUBLE = new HorizonCodecs.Primitive<>("Double");
    PrimitiveCodec<String> STRING = new HorizonCodecs.Primitive<>("String");

    default Codec<A> withLifecycle(Lifecycle lifecycle) {
        return new HorizonCodecs.Derived<>(this, "withLifecycle", this.toString());
    }

    default Codec<A> stable() {
        return withLifecycle(Lifecycle.stable());
    }

    default Codec<A> deprecated(int since) {
        return withLifecycle(Lifecycle.deprecated(since));
    }

    static <T> Codec<T> withAlternative(Codec<T> primary, Codec<? extends T> alternative) {
        return new HorizonCodecs.Derived<>(primary, "withAlternative", "EitherCodec[first=" + primary + ", second=" + alternative + "][xmapped]");
    }

    static <T, U> Codec<T> withAlternative(Codec<T> primary, Codec<U> alternative, Function<U, T> converter) {
        return new HorizonCodecs.Derived<>(primary, "withAlternative", "EitherCodec[first=" + primary + ", second=" + alternative + "][xmapped]");
    }

    static <E> Codec<List<E>> list(Codec<E> element) {
        return new HorizonCodecs.Derived<>(element, "list", "ListCodec[" + element + "]");
    }

    static <E> Codec<List<E>> list(Codec<E> element, int min, int max) {
        return new HorizonCodecs.Derived<>(element, "list", "ListCodec[" + element + "]");
    }

    static <K, V> UnboundedMapCodec<K, V> unboundedMap(Codec<K> key, Codec<V> element) {
        return new UnboundedMapCodec<>(key, element);
    }

    static <E> Codec<E> stringResolver(Function<E, String> toString, Function<String, E> fromString) {
        return new HorizonCodecs.Derived<>(STRING, "stringResolver", STRING + "[flatXmapped]");
    }

    static <A> Codec<A> unit(A value) {
        return MapCodec.unit(value).codec();
    }

    static <A> Codec<A> unit(Supplier<A> value) {
        return MapCodec.unit(value).codec();
    }

    static <A> Codec<A> lazyInitialized(Supplier<Codec<A>> maker) {
        return new HorizonCodecs.Lazy<>(maker);
    }

    default Codec<List<A>> listOf() {
        return list(this);
    }

    default Codec<List<A>> listOf(int min, int max) {
        return list(this, min, max);
    }

    default <S> Codec<S> xmap(Function<? super A, ? extends S> to, Function<? super S, ? extends A> from) {
        return new HorizonCodecs.Derived<>(this, "xmap", this + "[xmapped]");
    }

    default <S> Codec<S> comapFlatMap(Function<? super A, ? extends DataResult<? extends S>> to, Function<? super S, ? extends A> from) {
        return new HorizonCodecs.Derived<>(this, "comapFlatMap", this + "[comapFlatMapped]");
    }

    default <S> Codec<S> flatComapMap(Function<? super A, ? extends S> to, Function<? super S, ? extends DataResult<? extends A>> from) {
        return new HorizonCodecs.Derived<>(this, "flatComapMap", this + "[flatComapMapped]");
    }

    default <S> Codec<S> flatXmap(Function<? super A, ? extends DataResult<? extends S>> to, Function<? super S, ? extends DataResult<? extends A>> from) {
        return new HorizonCodecs.Derived<>(this, "flatXmap", this + "[flatXmapped]");
    }

    default MapCodec<A> fieldOf(String name) {
        return new HorizonCodecs.Field<>(name, this);
    }

    default MapCodec<Optional<A>> optionalFieldOf(String name) {
        return new HorizonCodecs.OptionalField<>(name, this);
    }

    default MapCodec<A> optionalFieldOf(String name, A fallback) {
        return new HorizonCodecs.OptionalField<A>(name, this).<A>xmap(o -> o.orElse(fallback), a -> Optional.of(a));
    }

    default MapCodec<A> optionalFieldOf(String name, A fallback, Lifecycle lifecycle) {
        return optionalFieldOf(name, fallback);
    }

    default MapCodec<Optional<A>> lenientOptionalFieldOf(String name) {
        return new HorizonCodecs.OptionalField<>(name, this);
    }

    default MapCodec<A> lenientOptionalFieldOf(String name, A fallback) {
        return optionalFieldOf(name, fallback);
    }

    default Codec<A> orElse(A value) {
        return new HorizonCodecs.Derived<>(this, "orElse", this + "[mapResult OrElse[" + value + "]]");
    }

    default Codec<A> orElse(Consumer<String> onError, A value) {
        return orElse(value);
    }

    default Codec<A> orElse(UnaryOperator<String> onError, A value) {
        return orElse(value);
    }

    default Codec<A> orElseGet(Supplier<? extends A> value) {
        return new HorizonCodecs.Derived<>(this, "orElseGet", this + "[mapResult OrElseGet[" + value + "]]");
    }

    default Codec<A> promotePartial(Consumer<String> onError) {
        return new HorizonCodecs.Derived<>(this, "promotePartial", "Codec[" + this + " " + this + "[promotePartial]]");
    }

    default <E> Codec<E> dispatch(Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec) {
        return dispatch("type", type, codec);
    }

    default <E> Codec<E> dispatch(String typeKey, Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec) {
        return dispatchMap(typeKey, type, codec).codec();
    }

    default <E> Codec<E> dispatchStable(Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec) {
        return dispatch(type, codec);
    }

    default <E> MapCodec<E> dispatchMap(Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec) {
        return dispatchMap("type", type, codec);
    }

    default <E> MapCodec<E> dispatchMap(String typeKey, Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec) {
        return new HorizonCodecs.Dispatch<>(typeKey, this, type, codec);
    }

    default Codec<A> validate(Function<A, DataResult<A>> check) {
        return new HorizonCodecs.Derived<>(this, "validate", this + "[flatXmapped]");
    }

    /** Observed: a value from min to max (by compareTo) is a success, any other an error naming the range. */
    static <N extends Number & Comparable<N>> Function<N, DataResult<N>> checkRange(N min, N max) {
        return value -> value.compareTo(min) >= 0 && value.compareTo(max) <= 0 ? DataResult.success(value)
                : DataResult.error(() -> "Value " + value + " outside of range [" + min + ":" + max + "]");
    }

    static Codec<Integer> intRange(int min, int max) {
        return INT.validate(checkRange(min, max));
    }

    static Codec<Float> floatRange(float min, float max) {
        return FLOAT.validate(checkRange(min, max));
    }

    static Codec<Double> doubleRange(double min, double max) {
        return DOUBLE.validate(checkRange(min, max));
    }
}
