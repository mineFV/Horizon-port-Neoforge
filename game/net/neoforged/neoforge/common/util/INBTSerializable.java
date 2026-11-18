package net.neoforged.neoforge.common.util;

/** The stand-in for NeoForge's INBTSerializable: what writes itself to a tag and reads itself back. */
public interface INBTSerializable<T extends net.minecraft.nbt.Tag> {
    T serializeNBT(net.minecraft.core.HolderLookup.Provider provider);

    void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, T nbt);
}
