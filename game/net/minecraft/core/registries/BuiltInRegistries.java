package net.minecraft.core.registries;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.HorizonRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

/** The stand-in for the game's BuiltInRegistries: those Mekanism uses, each a HorizonRegistry, their defaults observed. */
public class BuiltInRegistries {
    public static final DefaultedRegistry<net.minecraft.world.level.material.Fluid> FLUID = new HorizonRegistry<>(Registries.FLUID, ResourceLocation.withDefaultNamespace("empty"));
    public static final DefaultedRegistry<net.minecraft.world.level.block.Block> BLOCK = new HorizonRegistry<>(Registries.BLOCK, ResourceLocation.withDefaultNamespace("air"));
    public static final DefaultedRegistry<net.minecraft.world.entity.EntityType<?>> ENTITY_TYPE = new HorizonRegistry<>(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("pig"));
    public static final DefaultedRegistry<net.minecraft.world.item.Item> ITEM = new HorizonRegistry<>(Registries.ITEM, ResourceLocation.withDefaultNamespace("air"));
    public static final Registry<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITY_TYPE = new HorizonRegistry<>(Registries.BLOCK_ENTITY_TYPE, null);
    public static final Registry<Object> MENU = new HorizonRegistry<>(Registries.MENU, null);
    public static final Registry<Object> RECIPE_TYPE = new HorizonRegistry<>(Registries.RECIPE_TYPE, null);
    public static final Registry<net.minecraft.core.component.DataComponentType<?>> DATA_COMPONENT_TYPE = new HorizonRegistry<>(Registries.DATA_COMPONENT_TYPE, null);

    private BuiltInRegistries() {}
}
