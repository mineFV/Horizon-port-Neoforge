package net.neoforged.neoforge.registries.datamaps;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** The stand-in for NeoForge's DataMapType: its id, registry and codec (data maps' values aren't modelled: IWithData). */
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

        public DataMapType<R, T> build() {
            return new DataMapType<>(id, registryKey, codec);
        }
    }
}
