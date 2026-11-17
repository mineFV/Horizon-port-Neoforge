package net.neoforged.neoforge.common.extensions;

/** The stand-in for NeoForge's IHolderExtension: a holder's key (observed, `standins --horizon-src`). */
public interface IHolderExtension<T> extends net.neoforged.neoforge.registries.datamaps.IWithData<T> {
    default net.minecraft.resources.ResourceKey<T> getKey() {
        return ((net.minecraft.core.Holder<T>) this).unwrapKey().orElse(null);
    }
}
