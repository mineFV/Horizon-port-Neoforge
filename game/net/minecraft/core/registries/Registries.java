package net.minecraft.core.registries;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** The stand-in for the game's Registries: the keys of the registries a mod registers in. */
public class Registries {
    public static final ResourceLocation ROOT_REGISTRY_NAME = ResourceLocation.withDefaultNamespace("root");
    public static final ResourceKey<?> BLOCK = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("block"));
    public static final ResourceKey<?> FLUID = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("fluid"));
    public static final ResourceKey<?> ITEM = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("item"));
    public static final ResourceKey<?> BLOCK_ENTITY_TYPE = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("block_entity_type"));

    private Registries() {}
}
