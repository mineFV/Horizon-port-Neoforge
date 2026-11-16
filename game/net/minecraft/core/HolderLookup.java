package net.minecraft.core;

import net.neoforged.neoforge.common.extensions.IHolderLookupProviderExtension;

/** The stand-in for the game's HolderLookup: its supertypes, and Provider, which a block entity's load and save take. */
public interface HolderLookup<T> extends HolderGetter<T> {
    /** Its supertypes alone: what a block entity is given, which these stand-ins don't read. */
    interface Provider extends IHolderLookupProviderExtension {
    }
}
