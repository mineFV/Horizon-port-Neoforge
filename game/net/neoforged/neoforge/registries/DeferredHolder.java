package net.neoforged.neoforge.registries;

import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** The stand-in for NeoForge's DeferredHolder: a registered value, by its key (HorizonRegistries makes it). */
public class DeferredHolder<R, T extends R> implements Holder<R>, Supplier<T> {
    protected final ResourceKey<R> key;

    protected DeferredHolder(ResourceKey<R> key) {
        this.key = key;
    }

    public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<?> registryKey, ResourceLocation valueName) {
        return new DeferredHolder<>(ResourceKey.create(registryKey, valueName));
    }

    @SuppressWarnings("unchecked")
    public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<R> key) {
        return new DeferredHolder<>(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        Object value = HorizonRegistries.get(key);
        if (value == null) throw new NullPointerException("Trying to access unbound value: " + key);
        return (T) value;
    }

    @Override
    public R value() {
        return get();
    }

    @Override
    public boolean isBound() {
        return HorizonRegistries.get(key) != null;
    }

    @Override
    public boolean is(ResourceLocation id) {
        return key.location().equals(id);
    }

    @Override
    public boolean is(ResourceKey<R> other) {
        return key == other;
    }

    @Override
    public boolean is(java.util.function.Predicate<ResourceKey<R>> test) {
        return test.test(key);
    }

    /** Observed: a holder of its key (its value's own, or another DeferredHolder). */
    @Override
    public boolean is(Holder<R> other) {
        return other instanceof Holder.Reference<R> r ? r.key() == key : other instanceof DeferredHolder<?, ?> d && d.key == key;
    }

    @Override
    public java.util.Optional<ResourceKey<R>> unwrapKey() {
        return java.util.Optional.of(key);
    }

    /** Its key's value's (the game's goes to the registry's holder of its key). */
    @Override
    public boolean is(net.minecraft.tags.TagKey<R> tag) {
        return net.minecraft.tags.HorizonTags.is(key, tag);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public java.util.stream.Stream<net.minecraft.tags.TagKey<R>> tags() {
        return (java.util.stream.Stream) net.minecraft.tags.HorizonTags.tags(key).stream();
    }

    public ResourceLocation getId() {
        return key.location();
    }

    public ResourceKey<R> getKey() {
        return key;
    }

    /**
     * Observed (`dump`'s exporter): equal to a holder of the same key (the game's also to its registry's own holders
     * of it, which aren't modelled), its hash the key's, its text "DeferredHolder{" and the key "}".
     */
    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof DeferredHolder<?, ?> other && other.key == key;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "DeferredHolder{" + key + "}";
    }
}
