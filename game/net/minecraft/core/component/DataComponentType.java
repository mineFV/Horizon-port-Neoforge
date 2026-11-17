package net.minecraft.core.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.StreamCodec;

/**
 * The stand-in for the game's DataComponentType: a type of an item's data, made by its builder, which keeps the codecs
 * it is given (what they do isn't modelled). Its text is its id once registered (observed, `standins --horizon-src`).
 */
public interface DataComponentType<T> {
    static <T> Builder<T> builder() {
        return new Builder<>();
    }

    Codec<T> codec();

    default Codec<T> codecOrThrow() {
        Codec<T> codec = codec();
        if (codec == null) throw new IllegalStateException(this + " is not a persistent component");
        return codec;
    }

    default boolean isTransient() {
        return codec() == null;
    }

    StreamCodec<?, T> streamCodec();

    class Builder<T> {
        private Codec<T> codec;
        private StreamCodec<?, T> streamCodec;
        private boolean cacheEncoding;

        public Builder() {}

        public Builder<T> persistent(Codec<T> codec) {
            this.codec = codec;
            return this;
        }

        public Builder<T> networkSynchronized(StreamCodec<?, T> streamCodec) {
            this.streamCodec = streamCodec;
            return this;
        }

        public Builder<T> cacheEncoding() {
            this.cacheEncoding = true;
            return this;
        }

        /** Observed: with neither codec, NullPointerException("Missing Codec for component"); the network's follows the codec. */
        public DataComponentType<T> build() {
            StreamCodec<?, T> network = streamCodec;
            if (network == null) {
                if (codec == null) throw new NullPointerException("Missing Codec for component");
                network = new HorizonStreamCodec<>();
            }
            return new SimpleType<>(codec, network);
        }

        static class SimpleType<T> implements DataComponentType<T> {
            private final Codec<T> codec;
            private final StreamCodec<?, T> streamCodec;
            private net.minecraft.resources.ResourceLocation horizonId;

            SimpleType(Codec<T> codec, StreamCodec<?, T> streamCodec) {
                this.codec = codec;
                this.streamCodec = streamCodec;
            }

            @Override
            public Codec<T> codec() {
                return codec;
            }

            @Override
            public StreamCodec<?, T> streamCodec() {
                return streamCodec;
            }

            /** Observed: its id when registered, else "[unregistered]". */
            @Override
            public String toString() {
                return horizonId == null ? "[unregistered]" : horizonId.toString();
            }
        }
    }

    /** HorizonRegistries' and DataComponents': the id this type was registered under. Not the game's API. */
    static void horizonRegistered(DataComponentType<?> type, net.minecraft.resources.ResourceLocation id) {
        if (type instanceof Builder.SimpleType<?> simple) simple.horizonId = id;
    }

    /** Its id, or null. Not the game's API. */
    static net.minecraft.resources.ResourceLocation horizonId(DataComponentType<?> type) {
        return type instanceof Builder.SimpleType<?> simple ? simple.horizonId : null;
    }
}
