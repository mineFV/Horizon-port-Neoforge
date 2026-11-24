package net.minecraft.world.item.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * The stand-in for the game's Enchantments: the keys of the game's enchantments, each its field's name in lower case
 * in the game's namespace, observed (`standins`). Enchantments are the game's data since 1.21: what one does, and the
 * bootstrap that registers them, aren't here.
 */
public class Enchantments {
    public static final ResourceKey<?> PROTECTION = key("protection");
    public static final ResourceKey<?> FIRE_PROTECTION = key("fire_protection");
    public static final ResourceKey<?> FEATHER_FALLING = key("feather_falling");
    public static final ResourceKey<?> BLAST_PROTECTION = key("blast_protection");
    public static final ResourceKey<?> PROJECTILE_PROTECTION = key("projectile_protection");
    public static final ResourceKey<?> RESPIRATION = key("respiration");
    public static final ResourceKey<?> AQUA_AFFINITY = key("aqua_affinity");
    public static final ResourceKey<?> THORNS = key("thorns");
    public static final ResourceKey<?> DEPTH_STRIDER = key("depth_strider");
    public static final ResourceKey<?> FROST_WALKER = key("frost_walker");
    public static final ResourceKey<?> BINDING_CURSE = key("binding_curse");
    public static final ResourceKey<?> SOUL_SPEED = key("soul_speed");
    public static final ResourceKey<?> SWIFT_SNEAK = key("swift_sneak");
    public static final ResourceKey<?> SHARPNESS = key("sharpness");
    public static final ResourceKey<?> SMITE = key("smite");
    public static final ResourceKey<?> BANE_OF_ARTHROPODS = key("bane_of_arthropods");
    public static final ResourceKey<?> KNOCKBACK = key("knockback");
    public static final ResourceKey<?> FIRE_ASPECT = key("fire_aspect");
    public static final ResourceKey<?> LOOTING = key("looting");
    public static final ResourceKey<?> SWEEPING_EDGE = key("sweeping_edge");
    public static final ResourceKey<?> EFFICIENCY = key("efficiency");
    public static final ResourceKey<?> SILK_TOUCH = key("silk_touch");
    public static final ResourceKey<?> UNBREAKING = key("unbreaking");
    public static final ResourceKey<?> FORTUNE = key("fortune");
    public static final ResourceKey<?> POWER = key("power");
    public static final ResourceKey<?> PUNCH = key("punch");
    public static final ResourceKey<?> FLAME = key("flame");
    public static final ResourceKey<?> INFINITY = key("infinity");
    public static final ResourceKey<?> LUCK_OF_THE_SEA = key("luck_of_the_sea");
    public static final ResourceKey<?> LURE = key("lure");
    public static final ResourceKey<?> LOYALTY = key("loyalty");
    public static final ResourceKey<?> IMPALING = key("impaling");
    public static final ResourceKey<?> RIPTIDE = key("riptide");
    public static final ResourceKey<?> CHANNELING = key("channeling");
    public static final ResourceKey<?> MULTISHOT = key("multishot");
    public static final ResourceKey<?> QUICK_CHARGE = key("quick_charge");
    public static final ResourceKey<?> PIERCING = key("piercing");
    public static final ResourceKey<?> DENSITY = key("density");
    public static final ResourceKey<?> BREACH = key("breach");
    public static final ResourceKey<?> WIND_BURST = key("wind_burst");
    public static final ResourceKey<?> MENDING = key("mending");
    public static final ResourceKey<?> VANISHING_CURSE = key("vanishing_curse");

    public Enchantments() {}

    private static ResourceKey<?> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace(name));
    }
}
