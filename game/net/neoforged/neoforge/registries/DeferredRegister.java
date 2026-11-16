package net.neoforged.neoforge.registries;

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

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> supplier) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, name);
        DeferredHolder<T, I> holder = createHolder(registryKey, id);
        HorizonRegistries.add(holder.getKey(), supplier);
        return holder;
    }

    protected <I extends T> DeferredHolder<T, I> createHolder(ResourceKey<?> registryKey, ResourceLocation key) {
        return DeferredHolder.create(registryKey, key);
    }

    public void register(IEventBus bus) {}

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
        public <B extends Block> DeferredBlock<B> register(String name, Supplier<? extends B> supplier) {
            return (DeferredBlock<B>) super.register(name, supplier);
        }

        @Override
        protected <I extends Block> DeferredBlock<I> createHolder(ResourceKey<?> registryKey, ResourceLocation key) {
            return DeferredBlock.createBlock(ResourceKey.create(registryKey, key));
        }
    }
}
