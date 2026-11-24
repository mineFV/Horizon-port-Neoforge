package net.minecraft.core.registries;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** The stand-in for the game's Registries: the keys of the registries a mod registers in. */
public class Registries {
    public static final ResourceLocation ROOT_REGISTRY_NAME = ResourceLocation.withDefaultNamespace("root");
    public static final ResourceKey<?> BLOCK = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("block"));
    public static final ResourceKey<?> FLUID = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("fluid"));
    public static final ResourceKey<?> DATA_COMPONENT_TYPE = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("data_component_type"));
    public static final ResourceKey<?> ITEM = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("item"));
    public static final ResourceKey<?> ARMOR_MATERIAL = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("armor_material"));
    public static final ResourceKey<?> SOUND_EVENT = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("sound_event"));
    public static final ResourceKey<?> ATTRIBUTE = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("attribute"));
    public static final ResourceKey<?> ENTITY_TYPE = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("entity_type"));
    public static final ResourceKey<?> MENU = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("menu"));
    public static final ResourceKey<?> RECIPE_TYPE = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("recipe_type"));
    public static final ResourceKey<?> BLOCK_ENTITY_TYPE = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("block_entity_type"));
    public static final ResourceKey<?> ENCHANTMENT = ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("enchantment"));

    private Registries() {}
}
