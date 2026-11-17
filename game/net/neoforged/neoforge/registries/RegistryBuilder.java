package net.neoforged.neoforge.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * The stand-in for NeoForge's RegistryBuilder: a registry of the mod's, its key and default (HorizonRegistry). Its
 * settings for the network and numbers change nothing here; its callbacks aren't modelled.
 */
public class RegistryBuilder<T> {
    private final ResourceKey<?> key;
    private ResourceLocation defaultKey;

    public RegistryBuilder(ResourceKey<? extends Registry<T>> key) {
        this.key = key;
    }

    public RegistryBuilder<T> defaultKey(ResourceLocation key) {
        this.defaultKey = key;
        return this;
    }

    public RegistryBuilder<T> defaultKey(ResourceKey<T> key) {
        this.defaultKey = key.location();
        return this;
    }

    public RegistryBuilder<T> withIntrusiveHolders() {
        return this;
    }

    public RegistryBuilder<T> maxId(int max) {
        return this;
    }

    public RegistryBuilder<T> sync(boolean sync) {
        return this;
    }

    public RegistryBuilder<T> disableRegistrationCheck() {
        return this;
    }

    public Registry<T> create() {
        return new net.minecraft.core.HorizonRegistry<>(key, defaultKey, true);
    }
}
