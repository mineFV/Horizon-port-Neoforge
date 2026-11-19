package net.minecraft.world.item;

/**
 * The stand-in for the game's ArmorMaterial: its defence by piece, enchantment value, equip sound, repair ingredient
 * (not modelled: Ingredient), layers, toughness and knockback resistance (the game's, observed in a server).
 */
public record ArmorMaterial(java.util.Map<ArmorItem.Type, Integer> defense, int enchantmentValue, net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> equipSound,
        java.util.function.Supplier<net.minecraft.world.item.crafting.Ingredient> repairIngredient, java.util.List<Layer> layers, float toughness, float knockbackResistance) {
    /** Observed: its value for that piece, 0 for one it doesn't list. */
    public int getDefense(ArmorItem.Type type) {
        return defense.getOrDefault(type, 0);
    }

    /** The stand-in for ArmorMaterial.Layer: a layer's texture's name, and whether it can be dyed (its textures are the client's). */
    public static final class Layer {
        private final net.minecraft.resources.ResourceLocation assetName;
        private final String suffix;
        private final boolean dyeable;

        public Layer(net.minecraft.resources.ResourceLocation assetName, String suffix, boolean dyeable) {
            this.assetName = assetName;
            this.suffix = suffix;
            this.dyeable = dyeable;
        }

        public Layer(net.minecraft.resources.ResourceLocation assetName) {
            this(assetName, "", false);
        }

        public boolean dyeable() {
            return dyeable;
        }
    }
}
