package com.mojang.serialization.codecs;

import java.util.List;
import java.util.function.Function;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;

/**
 * The stand-in for DataFixerUpper's RecordCodecBuilder: a field of a record (its getter and its map codec), a point (a
 * value), or fields applied to a function of them (the record's constructor), as `group(...).apply(instance, ...)`
 * makes them. create and mapCodec run the mod's builder at once, as the game does (HorizonCodecs.Record).
 */
public final class RecordCodecBuilder<O, F> implements App<RecordCodecBuilder.Mu<O>, F> {
    final Function<O, F> getter;
    final MapCodec<F> codec;
    final Object point;
    final List<RecordCodecBuilder<O, ?>> parts;
    final Object function;

    private RecordCodecBuilder(Function<O, F> getter, MapCodec<F> codec, Object point, List<RecordCodecBuilder<O, ?>> parts, Object function) {
        this.getter = getter;
        this.codec = codec;
        this.point = point;
        this.parts = parts;
        this.function = function;
    }

    @SuppressWarnings("unchecked")
    public static <O, F> RecordCodecBuilder<O, F> unbox(App<Mu<O>, F> box) {
        return (RecordCodecBuilder<O, F>) box;
    }

    public static <O> Instance<O> instance() {
        return new Instance<>();
    }

    public static <O, F> RecordCodecBuilder<O, F> of(Function<O, F> getter, String name, Codec<F> codec) {
        return of(getter, codec.fieldOf(name));
    }

    public static <O, F> RecordCodecBuilder<O, F> of(Function<O, F> getter, MapCodec<F> codec) {
        return new RecordCodecBuilder<>(getter, codec, null, null, null);
    }

    public static <O, F> RecordCodecBuilder<O, F> point(F value) {
        return new RecordCodecBuilder<>(null, null, value, null, null);
    }

    public static <O, F> RecordCodecBuilder<O, F> stable(F value) {
        return point(value);
    }

    public static <O, F> RecordCodecBuilder<O, F> point(F value, Lifecycle lifecycle) {
        return point(value);
    }

    public static <O> Codec<O> create(Function<Instance<O>, ? extends App<Mu<O>, O>> builder) {
        return mapCodec(builder).codec();
    }

    public static <O> MapCodec<O> mapCodec(Function<Instance<O>, ? extends App<Mu<O>, O>> builder) {
        return build(builder.apply(instance()));
    }

    public static <O> MapCodec<O> build(App<Mu<O>, O> builder) {
        return new com.mojang.serialization.HorizonCodecs.Record<>(unbox(builder));
    }

    /** Products' apply: the fields applied to the function. Only RecordCodecBuilder's instance is modelled. Not the game's API. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <F extends K1, R> App<F, R> horizonApply(Applicative<F, ?> instance, Object function, App... fields) {
        if (!(instance instanceof Instance<?>)) throw new UnsupportedOperationException("an applicative other than RecordCodecBuilder's isn't modelled (horizon-port-neoforge)");
        List<RecordCodecBuilder<Object, ?>> parts = new java.util.ArrayList<>();
        for (App f : fields) parts.add((RecordCodecBuilder<Object, ?>) f);
        return (App<F, R>) new RecordCodecBuilder(null, null, null, List.copyOf(parts), function);
    }

    /** HorizonCodecs.Record's: the fields' getters and map codecs, in order (none for a point). Not the game's API. */
    public List<RecordCodecBuilder<O, ?>> horizonParts() {
        return parts == null ? (codec != null ? List.of(this) : List.of()) : parts;
    }

    /** The function its fields are applied to, or a point's value. Not the game's API. */
    public Object horizonFunction() {
        return function != null ? function : point;
    }

    /** The map codec of a field; null for a point or an application. Not the game's API. */
    public MapCodec<F> horizonCodec() {
        return codec;
    }

    public static final class Mu<O> implements K1 {
        public Mu() {}
    }

    public static final class Instance<O> implements Applicative<Mu<O>, Instance.Mu<O>> {
        public Instance() {}

        public <A> App<RecordCodecBuilder.Mu<O>, A> stable(A a) {
            return RecordCodecBuilder.point(a);
        }

        public <A> App<RecordCodecBuilder.Mu<O>, A> point(A a, Lifecycle lifecycle) {
            return RecordCodecBuilder.point(a);
        }

        @Override
        public <A> App<RecordCodecBuilder.Mu<O>, A> point(A a) {
            return RecordCodecBuilder.point(a);
        }

        static final class Mu<O> implements Applicative.Mu {
            private Mu() {}
        }
    }
}
