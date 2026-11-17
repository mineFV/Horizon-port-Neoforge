package net.minecraft.core;

import net.minecraft.resources.ResourceLocation;

/** The stand-in for the game's DefaultedRegistry: an unknown id gives its default value, an unknown value its default id. */
public interface DefaultedRegistry<T> extends Registry<T> {
    @Override
    ResourceLocation getKey(T value);

    @Override
    T get(ResourceLocation id);

    @Override
    T byId(int id);

    ResourceLocation getDefaultKey();
}
