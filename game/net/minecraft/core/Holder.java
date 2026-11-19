package net.minecraft.core;

import net.neoforged.neoforge.common.extensions.IHolderExtension;

/** The stand-in for the game's Holder: a value of a registry, as a DeferredHolder holds one. */
public interface Holder<T> extends IHolderExtension<T> {
    T value();

    boolean isBound();

    boolean is(net.minecraft.resources.ResourceLocation id);

    boolean is(net.minecraft.resources.ResourceKey<T> key);

    boolean is(java.util.function.Predicate<net.minecraft.resources.ResourceKey<T>> test);

    boolean is(Holder<T> other);

    java.util.Optional<net.minecraft.resources.ResourceKey<T>> unwrapKey();

    /** Observed (`standins --horizon-src`): its key's id, else "[unregistered]". */
    default String getRegisteredName() {
        return unwrapKey().map(k -> k.location().toString()).orElse("[unregistered]");
    }

    /** Whether its value is in a tag, as the tags were given (HorizonTags). */
    boolean is(net.minecraft.tags.TagKey<T> tag);

    /** The tags its value is in, as given (HorizonTags), by their ids' order: the game's order changes from run to run. */
    java.util.stream.Stream<net.minecraft.tags.TagKey<T>> tags();

    /** The stand-in for Holder.Reference: a registered value with its key. */
    class Reference<T> implements Holder<T> {
        private net.minecraft.resources.ResourceKey<T> key;
        private final T value;

        /** HorizonRegistries': the game's own constructor binds it to a registry. */
        public Reference(net.minecraft.resources.ResourceKey<T> key, T value) {
            this.key = key;
            this.value = value;
        }

        /** HorizonRegistries': an intrusive holder's key, once its value is registered. Not the game's API. */
        public void horizonBind(net.minecraft.resources.ResourceKey<T> key) {
            this.key = key;
        }

        /**
         * Observed: an intrusive holder whose value isn't registered yet has none, and says so (unwrapKey too). The
         * game's text then names the registry's holder owner, an object of its own whose text is its identity: here the
         * registry's.
         */
        public net.minecraft.resources.ResourceKey<T> key() {
            if (key == null) throw new IllegalStateException("Trying to access unbound value '" + value + "' from registry " + net.neoforged.neoforge.registries.HorizonRegistries.intrusiveOwner(value));
            return key;
        }

        @Override
        public T value() {
            return value;
        }

        @Override
        public boolean is(net.minecraft.resources.ResourceLocation id) {
            return key.location().equals(id);
        }

        @Override
        public boolean isBound() {
            return key != null && value != null;
        }

        @Override
        public boolean is(net.minecraft.resources.ResourceKey<T> other) {
            return key == other;
        }

        @Override
        public boolean is(java.util.function.Predicate<net.minecraft.resources.ResourceKey<T>> test) {
            return test.test(key);
        }

        /** Observed: whether it is that holder (a DeferredHolder of it is too: its key's). */
        @Override
        public boolean is(Holder<T> other) {
            return this == other || other instanceof net.neoforged.neoforge.registries.DeferredHolder<?, ?> d && d.getKey() == key;
        }

        @Override
        public java.util.Optional<net.minecraft.resources.ResourceKey<T>> unwrapKey() {
            return java.util.Optional.of(key());
        }

        @Override
        public boolean is(net.minecraft.tags.TagKey<T> tag) {
            return net.minecraft.tags.HorizonTags.is(key, tag);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public java.util.stream.Stream<net.minecraft.tags.TagKey<T>> tags() {
            return (java.util.stream.Stream) net.minecraft.tags.HorizonTags.tags(key).stream();
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
