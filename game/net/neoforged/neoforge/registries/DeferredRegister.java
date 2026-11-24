package net.neoforged.neoforge.registries;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;

/**
 * The stand-in for NeoForge's DeferredRegister: what a mod registers, under its namespace, in HorizonRegistries.
 * Registering on the mod's bus has nothing to do: values are made when first asked for.
 */
public class DeferredRegister<T> {
    private final ResourceKey<?> registryKey;
    private final String namespace;
    private final java.util.Set<DeferredHolder<T, ? extends T>> entries = new java.util.LinkedHashSet<>();
    private final java.util.Set<DeferredHolder<T, ? extends T>> entriesView = java.util.Collections.unmodifiableSet(entries);

    protected DeferredRegister(ResourceKey<?> registryKey, String namespace) {
        this.registryKey = registryKey;
        this.namespace = namespace;
    }

    public static <T> DeferredRegister<T> create(ResourceKey<?> registryKey, String namespace) {
        return new DeferredRegister<>(registryKey, namespace);
    }

    public static Blocks createBlocks(String namespace) {
        return new Blocks(namespace);
    }

    /**
     * Observed: the value is made by func, given its id, when first asked for (in the game, when the registry is
     * filled); the holder is made before a name registered twice is refused.
     */
    public <I extends T> DeferredHolder<T, I> register(String name, Function<ResourceLocation, ? extends I> func) {
        java.util.Objects.requireNonNull(name);
        java.util.Objects.requireNonNull(func);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, name);
        DeferredHolder<T, I> holder = createHolder(registryKey, id);
        if (!entries.add(holder)) throw new IllegalArgumentException("Duplicate registration " + name);
        HorizonRegistries.add(holder.getKey(), () -> func.apply(id));
        return holder;
    }

    /** Observed: hands on to register by a function, which a subclass overriding that one sees. */
    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> supplier) {
        return register(name, id -> supplier.get());
    }

    protected <I extends T> DeferredHolder<T, I> createHolder(ResourceKey<?> registryKey, ResourceLocation key) {
        return DeferredHolder.create(registryKey, key);
    }

    public void register(IEventBus bus) {}

    /**
     * An old id for a new one: observed in a server (probe/BlockItemProbe.java, aliases), the registry's lookups don't
     * follow it (the old id gives a defaulted registry's default, and nothing else); what NeoForge does with it (the
     * ids of saved data) is Horizon's, so it changes nothing here.
     */
    public void addAlias(ResourceLocation from, ResourceLocation to) {}

    /** Observed: what it registered, in that order, as a set that can't be changed: the same view each time. */
    public java.util.Collection<DeferredHolder<T, ? extends T>> getEntries() {
        return entriesView;
    }

    public ResourceKey<?> getRegistryKey() {
        return registryKey;
    }

    public String getNamespace() {
        return namespace;
    }

    /** The stand-in for DeferredRegister.Blocks. */
    public static class Blocks extends DeferredRegister<Block> {
        protected Blocks(String namespace) {
            super(Registries.BLOCK, namespace);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <B extends Block> DeferredBlock<B> register(String name, Function<ResourceLocation, ? extends B> func) {
            return (DeferredBlock<B>) super.register(name, func);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <B extends Block> DeferredBlock<B> register(String name, Supplier<? extends B> supplier) {
            return (DeferredBlock<B>) super.register(name, supplier);
        }

        @Override
        protected <I extends Block> DeferredBlock<I> createHolder(ResourceKey<?> registryKey, ResourceLocation key) {
            return DeferredBlock.createBlock(ResourceKey.create(registryKey, key));
        }
    }
}
