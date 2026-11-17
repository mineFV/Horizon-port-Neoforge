package net.neoforged.neoforge.registries;

/** The stand-in for NeoForge's IRegistryExtension: whether a value is registered, and its id or null. */
public interface IRegistryExtension<T> {
    boolean containsValue(T value);

    /** Observed (`standins --horizon-src`): its id when it is registered, else null (where getKey gives a defaulted registry's default). */
    default net.minecraft.resources.ResourceLocation getKeyOrNull(T value) {
        return containsValue(value) ? ((net.minecraft.core.Registry<T>) this).getKey(value) : null;
    }
}
