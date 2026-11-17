package com.mojang.serialization;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Not the game's: the codecs this model makes, each keeping what it was made of. They don't encode or decode yet (no
 * method of theirs does); their texts are the game's where they were observed, else say what they are.
 */
public final class HorizonCodecs {
    private HorizonCodecs() {}

    /** A codec of the game's (ItemStack.CODEC...): its name. Not the game's API. */
    public static <A> Codec<A> game(String name) {
        return new Derived<>(null, "game", name);
    }

    /** A primitive codec: its text is its name (observed). */
    public static final class Primitive<A> implements PrimitiveCodec<A> {
        private final String name;

        Primitive(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** A codec made from another (a list, an xmap, a validation...): the operation and its text. */
    public static final class Derived<A> implements Codec<A> {
        final Object from;
        final String operation;
        private final String text;

        Derived(Object from, String operation, String text) {
            this.from = from;
            this.operation = operation;
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** A map codec made from another. */
    public static final class DerivedMap<A> extends MapCodec<A> {
        final Object from;
        final String operation;
        private final String text;

        DerivedMap(Object from, String operation, String text) {
            this.from = from;
            this.operation = operation;
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** A codec made later, by a supplier: its text is the made codec's. */
    public static final class Lazy<A> implements Codec<A> {
        private final Supplier<Codec<A>> maker;
        private Codec<A> made;

        Lazy(Supplier<Codec<A>> maker) {
            this.maker = maker;
        }

        synchronized Codec<A> get() {
            if (made == null) made = maker.get();
            return made;
        }

        /** Observed: "RecursiveCodec[" and its supplier's text "]". */
        @Override
        public String toString() {
            return "RecursiveCodec[" + maker + "]";
        }
    }

    /** A field of a map: its name and its value's codec. */
    public static final class Field<A> extends MapCodec<A> {
        final String name;
        final Codec<A> codec;

        Field(String name, Codec<A> codec) {
            this.name = name;
            this.codec = codec;
        }

        @Override
        public String toString() {
            return "Field[" + name + ": " + codec + "]";
        }
    }

    /** An optional field of a map. */
    public static final class OptionalField<A> extends MapCodec<Optional<A>> {
        final String name;
        final Codec<A> codec;

        OptionalField(String name, Codec<A> codec) {
            this.name = name;
            this.codec = codec;
        }

        @Override
        public String toString() {
            return "OptionalFieldCodec[" + name + ": " + codec + "]";
        }
    }

    /** A value with no field (MapCodec.unit). */
    public static final class Unit<A> extends MapCodec<A> {
        final A value;

        Unit(A value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "MapCodec[EmptyEncoder UnitDecoder[" + value + "]]";
        }
    }

    /** A map codec as a codec (MapCodec.codec). */
    public static final class OfMap<A> implements Codec<A> {
        final MapCodec<A> map;

        OfMap(MapCodec<A> map) {
            this.map = map;
        }

        @Override
        public String toString() {
            return map.toString();
        }
    }

    /** A dispatch: a key's codec gives the type, the type gives the map codec of the rest. */
    public static final class Dispatch<K, E> extends MapCodec<E> {
        final String typeKey;
        final Codec<K> keyCodec;
        final Function<? super E, ? extends K> type;
        final Function<? super K, ? extends MapCodec<? extends E>> codec;

        Dispatch(String typeKey, Codec<K> keyCodec, Function<? super E, ? extends K> type, Function<? super K, ? extends MapCodec<? extends E>> codec) {
            this.typeKey = typeKey;
            this.keyCodec = keyCodec;
            this.type = type;
            this.codec = codec;
        }

        @Override
        public String toString() {
            return "KeyDispatchCodec[" + keyCodec + " " + type + " " + codec + "]";
        }
    }

    /** A record: its fields (getters and map codecs) and the function that makes it from them. */
    public static final class Record<O> extends MapCodec<O> {
        final RecordCodecBuilder<O, O> builder;

        public Record(RecordCodecBuilder<O, O> builder) {
            this.builder = builder;
        }

        /** Observed: its function as a point ("UnitDecoder[" it "]"), then each field's map codec, after " * ". */
        @Override
        public String toString() {
            StringBuilder b = new StringBuilder("RecordCodec[UnitDecoder[").append(builder.horizonFunction()).append(']');
            for (RecordCodecBuilder<O, ?> part : builder.horizonParts()) {
                if (part.horizonCodec() != null) b.append(" * ").append(part.horizonCodec());
            }
            return b.append(']').toString();
        }
    }
}
