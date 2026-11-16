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
