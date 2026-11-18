package net.minecraft.world.level;

/** The stand-in for the game's ItemLike: what has an item (Item itself; Block's isn't modelled yet). */
public interface ItemLike {
    net.minecraft.world.item.Item asItem();
}
