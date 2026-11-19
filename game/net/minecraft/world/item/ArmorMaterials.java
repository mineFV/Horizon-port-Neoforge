package net.minecraft.world.item;

/** The stand-in for the game's ArmorMaterials: the game's materials a mod names, their values observed in a server. */
public class ArmorMaterials {
    public static final net.minecraft.core.Holder<ArmorMaterial> NETHERITE = game("netherite", defense(3, 6, 8, 3, 11), 15, "item.armor.equip_netherite", 3.0F, 0.1F);

    public ArmorMaterials() {}

    /**
     * The defence of boots, leggings, chestplate, helmet and body, in the game's order: its map is an EnumMap, this one
     * a map in the same order (its class differs), which can't be changed.
     */
    private static java.util.Map<ArmorItem.Type, Integer> defense(int boots, int leggings, int chestplate, int helmet, int body) {
        java.util.Map<ArmorItem.Type, Integer> m = new java.util.LinkedHashMap<>();
        m.put(ArmorItem.Type.HELMET, helmet);
        m.put(ArmorItem.Type.CHESTPLATE, chestplate);
        m.put(ArmorItem.Type.LEGGINGS, leggings);
        m.put(ArmorItem.Type.BOOTS, boots);
        m.put(ArmorItem.Type.BODY, body);
        return java.util.Collections.unmodifiableMap(m);
    }

    @SuppressWarnings("unchecked")
    private static net.minecraft.core.Holder<ArmorMaterial> game(String name, java.util.Map<ArmorItem.Type, Integer> defense, int enchantmentValue, String sound, float toughness,
            float knockbackResistance) {
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.withDefaultNamespace(name);
        net.minecraft.resources.ResourceLocation soundId = net.minecraft.resources.ResourceLocation.withDefaultNamespace(sound);
        net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> soundHolder = (net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent>) (Object) net.neoforged.neoforge.registries.HorizonRegistries
                .bind(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.SOUND_EVENT, soundId), (Object) net.minecraft.sounds.SoundEvent.createVariableRangeEvent(soundId));
        ArmorMaterial material = new ArmorMaterial(defense, enchantmentValue, soundHolder, () -> {
            throw new UnsupportedOperationException("ingredients aren't modelled (horizon-port-neoforge)");
        }, java.util.List.of(new ArmorMaterial.Layer(id)), toughness, knockbackResistance);
        return (net.minecraft.core.Holder<ArmorMaterial>) (Object) net.neoforged.neoforge.registries.HorizonRegistries.bind(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ARMOR_MATERIAL, id), (Object) material);
    }
}
