package net.minecraft.world.item.enchantment;

import net.minecraft.world.item.component.TooltipProvider;

/**
 * The stand-in for the game's ItemEnchantments: EMPTY, which every item's components have. Enchantments aren't
 * modelled, so no other can be made.
 */
public class ItemEnchantments implements TooltipProvider {
    public static final ItemEnchantments EMPTY = new ItemEnchantments();

    private ItemEnchantments() {}

    /** Observed: 0, EMPTY having none (asked with null, which the game's map takes). */
    public int getLevel(net.minecraft.core.Holder<?> enchantment) {
        return 0;
    }

    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return true;
    }

    /** EMPTY is the only one: equal to itself alone. */
    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    /** Observed (EMPTY's). */
    @Override
    public int hashCode() {
        return 1;
    }

    /** Observed (EMPTY's). */
    @Override
    public String toString() {
        return "ItemEnchantments{enchantments={}, showInTooltip=true}";
    }
}
