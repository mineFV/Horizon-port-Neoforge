package net.minecraft.world.entity.ai.attributes;

/** The stand-in for the game's Attributes: the game's attributes a mod names, with their ids and values (observed in a server, `standins --horizon-src`). */
public class Attributes {
    public static final net.minecraft.core.Holder<Attribute> ATTACK_DAMAGE = game("generic.attack_damage", new RangedAttribute("attribute.name.generic.attack_damage", 2.0, 0.0, 2048.0));
    public static final net.minecraft.core.Holder<Attribute> ATTACK_SPEED = game("generic.attack_speed", new RangedAttribute("attribute.name.generic.attack_speed", 4.0, 0.0, 1024.0).setSyncable(true));
    public static final net.minecraft.core.Holder<Attribute> FALL_DAMAGE_MULTIPLIER = game("generic.fall_damage_multiplier", new RangedAttribute("attribute.name.generic.fall_damage_multiplier", 1.0, 0.0, 100.0).setSyncable(true).setSentiment(Attribute.Sentiment.NEGATIVE));
    public static final net.minecraft.core.Holder<Attribute> MAX_HEALTH = game("generic.max_health", new RangedAttribute("attribute.name.generic.max_health", 20.0, 1.0, 1024.0).setSyncable(true));
    public static final net.minecraft.core.Holder<Attribute> MOVEMENT_EFFICIENCY = game("generic.movement_efficiency", new RangedAttribute("attribute.name.generic.movement_efficiency", 0.0, 0.0, 1.0).setSyncable(true));
    public static final net.minecraft.core.Holder<Attribute> MOVEMENT_SPEED = game("generic.movement_speed", new net.neoforged.neoforge.common.PercentageAttribute("attribute.name.generic.movement_speed", 0.7, 0.0, 1024.0).setSyncable(true));
    public static final net.minecraft.core.Holder<Attribute> SAFE_FALL_DISTANCE = game("generic.safe_fall_distance", new RangedAttribute("attribute.name.generic.safe_fall_distance", 3.0, -1024.0, 1024.0).setSyncable(true));
    public static final net.minecraft.core.Holder<Attribute> SNEAKING_SPEED = game("player.sneaking_speed", new RangedAttribute("attribute.name.player.sneaking_speed", 0.3, 0.0, 1.0).setSyncable(true));
    public static final net.minecraft.core.Holder<Attribute> STEP_HEIGHT = game("generic.step_height", new RangedAttribute("attribute.name.generic.step_height", 0.6, 0.0, 10.0).setSyncable(true));
    public static final net.minecraft.core.Holder<Attribute> SUBMERGED_MINING_SPEED = game("player.submerged_mining_speed", new RangedAttribute("attribute.name.player.submerged_mining_speed", 0.2, 0.0, 20.0).setSyncable(true));
    public static final net.minecraft.core.Holder<Attribute> WATER_MOVEMENT_EFFICIENCY = game("generic.water_movement_efficiency", new RangedAttribute("attribute.name.generic.water_movement_efficiency", 0.0, 0.0, 1.0).setSyncable(true));

    public Attributes() {}

    @SuppressWarnings("unchecked")
    private static net.minecraft.core.Holder<Attribute> game(String name, Attribute attribute) {
        return (net.minecraft.core.Holder<Attribute>) (Object) net.neoforged.neoforge.registries.HorizonRegistries.bind(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.ATTRIBUTE, net.minecraft.resources.ResourceLocation.withDefaultNamespace(name)), (Object) attribute);
    }
}
