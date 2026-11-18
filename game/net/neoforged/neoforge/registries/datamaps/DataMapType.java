package net.neoforged.neoforge.registries.datamaps;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** The stand-in for NeoForge's DataMapType: its id, registry and codec (a NeoForge map's codec isn't modelled: null here, refused). */
public class DataMapType<R, T> {
    private final ResourceLocation id;
    private final ResourceKey<?> registryKey;
    private final Codec<T> codec;

    DataMapType(ResourceLocation id, ResourceKey<?> registryKey, Codec<T> codec) {
        this.id = id;
        this.registryKey = registryKey;
        this.codec = codec;
    }

    public static <T, R> Builder<T, R> builder(ResourceLocation id, ResourceKey<?> registryKey, Codec<T> codec) {
        return new Builder<>(id, registryKey, codec);
    }

    public ResourceKey<?> registryKey() {
        return registryKey;
    }

    public ResourceLocation id() {
        return id;
    }

    public Codec<T> codec() {
        if (codec == null) throw new UnsupportedOperationException("the codec of the data map " + id + " isn't modelled (horizon-port-neoforge)");
        return codec;
    }

    /** The stand-in for DataMapType.Builder: what makes the type (its other settings aren't modelled). */
    public static class Builder<T, R> {
        private final ResourceLocation id;
        private final ResourceKey<?> registryKey;
        private final Codec<T> codec;

        Builder(ResourceLocation id, ResourceKey<?> registryKey, Codec<T> codec) {
            this.id = id;
            this.registryKey = registryKey;
            this.codec = codec;
        }

        /** Its codec over the network and whether a client must have the map: kept, not used (the network isn't modelled). */
        protected Codec<T> networkCodec;
        protected boolean mandatorySync;

        public Builder<T, R> synced(Codec<T> networkCodec, boolean mandatory) {
            this.networkCodec = networkCodec;
            this.mandatorySync = mandatory;
            return this;
        }

        public DataMapType<R, T> build() {
            return new DataMapType<>(id, registryKey, codec);
        }
    }
}
