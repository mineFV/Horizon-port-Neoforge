package net.minecraft.core;

import net.neoforged.neoforge.common.extensions.IHolderExtension;

/** The stand-in for the game's Holder: a value of a registry, as a DeferredHolder holds one. */
public interface Holder<T> extends IHolderExtension<T> {
    T value();

    /** The stand-in for Holder.Reference: a registered value with its key. */
    class Reference<T> implements Holder<T> {
        private final net.minecraft.resources.ResourceKey<T> key;
        private final T value;

        /** HorizonRegistries': the game's own constructor binds it to a registry. */
        public Reference(net.minecraft.resources.ResourceKey<T> key, T value) {
            this.key = key;
            this.value = value;
        }

        public net.minecraft.resources.ResourceKey<T> key() {
            return key;
        }

        @Override
        public T value() {
            return value;
        }

        public boolean is(net.minecraft.resources.ResourceLocation id) {
            return key.location().equals(id);
        }

        /** Observed in a server (`standins --horizon-src`): "Reference{" key "=" value "}". */
        @Override
        public String toString() {
            return "Reference{" + key + "=" + value + "}";
        }

        /**
         * Observed: true for itself, false for another value's. The stand-ins make one for each value (the game's
         * builtInRegistryHolder), so two of one key aren't made here, and identity answers all there is.
         */
        @Override
        public boolean equals(Object other) {
            return this == other;
        }

        /** Observed: its key's. */
        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }
}
