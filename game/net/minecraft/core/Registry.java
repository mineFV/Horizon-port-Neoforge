package net.minecraft.core;

import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * The stand-in for the game's Registry: a value's id, an id's value, their holders (HorizonRegistry). What needs the
 * whole of a registry (its keys, its entries, its size, numbers) isn't modelled.
 */
public interface Registry<T> extends com.mojang.serialization.Keyable, IdMap<T>, net.neoforged.neoforge.registries.IRegistryExtension<T> {
    ResourceKey<?> key();

    default com.mojang.serialization.Codec<T> byNameCodec() {
        return com.mojang.serialization.HorizonCodecs.game("Registry.byNameCodec[" + key().location() + "]");
    }

    default com.mojang.serialization.Codec<Holder<T>> holderByNameCodec() {
        return com.mojang.serialization.HorizonCodecs.game("Registry.holderByNameCodec[" + key().location() + "]");
    }

    ResourceLocation getKey(T value);

    Optional<ResourceKey<T>> getResourceKey(T value);

    T get(ResourceKey<T> key);

    T get(ResourceLocation id);

    default Optional<T> getOptional(ResourceLocation id) {
        return Optional.ofNullable(get(id));
    }

    default Optional<T> getOptional(ResourceKey<T> key) {
        return Optional.ofNullable(get(key));
    }

    /** Observed: the value, else IllegalStateException naming the key and the registry. */
    default T getOrThrow(ResourceKey<T> key) {
        T value = get(key);
        if (value == null) throw new IllegalStateException("Missing key in " + key() + ": " + key);
        return value;
    }

    java.util.Set<ResourceLocation> keySet();

    java.util.Set<java.util.Map.Entry<ResourceKey<T>, T>> entrySet();

    default java.util.stream.Stream<T> stream() {
        return java.util.stream.StreamSupport.stream(spliterator(), false);
    }

    boolean containsKey(ResourceLocation id);

    boolean containsKey(ResourceKey<T> key);

    Optional<Holder.Reference<T>> getHolder(ResourceLocation id);

    Optional<Holder.Reference<T>> getHolder(ResourceKey<T> key);

    Holder<T> wrapAsHolder(T value);

    /** Observed: the holder, else IllegalStateException naming the key. */
    default Holder.Reference<T> getHolderOrThrow(ResourceKey<T> key) {
        return getHolder(key).orElseThrow(() -> new IllegalStateException("Missing key in " + key() + ": " + key));
    }

    java.util.stream.Stream<Holder.Reference<T>> holders();
}
