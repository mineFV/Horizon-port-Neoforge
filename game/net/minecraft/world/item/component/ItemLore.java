package net.minecraft.world.item.component;

import java.util.List;

/**
 * The stand-in for the game's ItemLore, a record of lines: EMPTY, which every item's components have. Texts
 * (Component) aren't modelled, so no line can be made.
 */
public record ItemLore(List<Object> lines, List<Object> styledLines) implements TooltipProvider {
    public static final ItemLore EMPTY = new ItemLore(List.of(), List.of());
}
