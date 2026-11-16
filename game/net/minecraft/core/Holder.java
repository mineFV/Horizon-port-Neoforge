package net.minecraft.core;

import net.neoforged.neoforge.common.extensions.IHolderExtension;

/** The stand-in for the game's Holder: a value of a registry, as a DeferredHolder holds one. */
public interface Holder<T> extends IHolderExtension {
    T value();
}
