package net.minecraft.world.item;

/**
 * The stand-in for the game's BowItem: made as an item is, from its properties, its constructor adding nothing to them
 * (observed in a server: a bow and a plain item made with the same properties have the same components). Using it
 * (drawing, releasing, shooting arrows) isn't modelled, so not declared.
 */
public class BowItem extends ProjectileWeaponItem {
    public BowItem(Item.Properties properties) {
        super(properties);
    }
}
