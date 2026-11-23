package net.neoforged.neoforge.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/**
 * The stand-in for NeoForge's Tags: the common tags ("c") a mod names, as keys, observed in a server (`standins
 * --horizon-src`, probe/TagProbe.java); what holds one is the game's tags' (HorizonTags). Only those Mekanism names
 * are here (but DamageTypes, whose type isn't modelled); not getTagTranslationKey.
 */
public class Tags {
    public Tags() {}

    public static class Items {
        public static final TagKey<net.minecraft.world.item.Item> ORES = tag("ores");
        public static final TagKey<net.minecraft.world.item.Item> ORES_COPPER = tag("ores/copper");
        public static final TagKey<net.minecraft.world.item.Item> ORES_GOLD = tag("ores/gold");
        public static final TagKey<net.minecraft.world.item.Item> ORES_IRON = tag("ores/iron");

        public Items() {}

        private static TagKey<net.minecraft.world.item.Item> tag(String name) {
            return net.minecraft.tags.ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", name));
        }
    }

    public static class Fluids {
        public static final TagKey<net.minecraft.world.level.material.Fluid> GASEOUS = tag("gaseous");
        public static final TagKey<net.minecraft.world.level.material.Fluid> HIDDEN_FROM_RECIPE_VIEWERS = tag("hidden_from_recipe_viewers");

        public Fluids() {}

        private static TagKey<net.minecraft.world.level.material.Fluid> tag(String name) {
            return net.minecraft.tags.FluidTags.create(ResourceLocation.fromNamespaceAndPath("c", name));
        }
    }

    public static class EntityTypes {
        public static final TagKey<net.minecraft.world.entity.EntityType<?>> TELEPORTING_NOT_SUPPORTED = tag("teleporting_not_supported");

        public EntityTypes() {}

        private static TagKey<net.minecraft.world.entity.EntityType<?>> tag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", name));
        }
    }
}
