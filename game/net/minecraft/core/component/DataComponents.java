package net.minecraft.core.component;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * The stand-in for the game's DataComponents: the types every item's components have (COMMON_ITEM_COMPONENTS) and an
 * item's durability, with their ids. Their codecs are kept as objects of no behaviour (what they do isn't modelled):
 * that they are persistent is all that can be asked of them here. The other types of the game aren't here.
 */
public class DataComponents {
    public static final DataComponentType<Integer> MAX_STACK_SIZE = register("max_stack_size");
    public static final DataComponentType<Integer> MAX_DAMAGE = register("max_damage");
    public static final DataComponentType<Integer> DAMAGE = register("damage");
    public static final DataComponentType<ItemLore> LORE = register("lore");
    public static final DataComponentType<ItemEnchantments> ENCHANTMENTS = register("enchantments");
    public static final DataComponentType<Integer> REPAIR_COST = register("repair_cost");
    public static final DataComponentType<ItemAttributeModifiers> ATTRIBUTE_MODIFIERS = register("attribute_modifiers");
    public static final DataComponentType<Rarity> RARITY = register("rarity");

    /** Observed in a server: their types and values, in this order. */
    public static final DataComponentMap COMMON_ITEM_COMPONENTS = DataComponentMap.builder()
            .set(MAX_STACK_SIZE, 64)
            .set(LORE, ItemLore.EMPTY)
            .set(ENCHANTMENTS, ItemEnchantments.EMPTY)
            .set(REPAIR_COST, 0)
            .set(ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
            .set(RARITY, Rarity.COMMON)
            .build();

    private static <T> DataComponentType<T> register(String name) {
        DataComponentType<T> type = DataComponentType.<T>builder().persistent(new HorizonCodec<T>()).build();
        DataComponentType.horizonRegistered(type, ResourceLocation.withDefaultNamespace(name));
        return type;
    }
}
