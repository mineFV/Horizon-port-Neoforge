package net.minecraft.world.item.component;

import java.util.List;

/**
 * The stand-in for the game's ItemAttributeModifiers, a record of modifiers: EMPTY, which every item's components
 * have. Attributes aren't modelled, so no modifier can be made.
 */
public record ItemAttributeModifiers(List<Object> modifiers, boolean showInTooltip) {
    public static final ItemAttributeModifiers EMPTY = new ItemAttributeModifiers(List.of(), true);
}
