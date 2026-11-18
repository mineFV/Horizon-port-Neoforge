package net.minecraft.tags;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** The stand-in for the game's TagKey: a registry and a tag's id, one object for each (observed, `standins --horizon-src`). */
public record TagKey<T>(ResourceKey<?> registry, ResourceLocation location) {
    private static final Map<TagKey<?>, TagKey<?>> MADE = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> TagKey<T> create(ResourceKey<?> registry, ResourceLocation location) {
        TagKey<T> key = new TagKey<>(registry, location);
        return (TagKey<T>) MADE.computeIfAbsent(key, k -> k);
    }

    /** A codec of a registry's tags by id (encoding isn't modelled yet: HorizonCodecs). */
    public static <T> com.mojang.serialization.Codec<TagKey<T>> codec(ResourceKey<?> registry) {
        return com.mojang.serialization.HorizonCodecs.game("TagKey.codec[" + registry.location() + "]");
    }

    /** A codec of a registry's tags by "#" and id (encoding isn't modelled yet). */
    public static <T> com.mojang.serialization.Codec<TagKey<T>> hashedCodec(ResourceKey<?> registry) {
        return com.mojang.serialization.HorizonCodecs.game("TagKey.hashedCodec[" + registry.location() + "]");
    }

    public boolean isFor(ResourceKey<?> registry) {
        return this.registry == registry;
    }

    @SuppressWarnings("unchecked")
    public <E> Optional<TagKey<E>> cast(ResourceKey<?> registry) {
        return isFor(registry) ? Optional.of((TagKey<E>) this) : Optional.empty();
    }

    @Override
    public String toString() {
        return "TagKey[" + registry.location() + " / " + location + "]";
    }
}
