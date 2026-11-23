package net.minecraft.world.item;

/**
 * The stand-in for the game's ProjectileWeaponItem, an item that shoots what it is loaded with (a bow, a crossbow):
 * made as an item is, from its properties, its constructor adding nothing to them (observed with a bow). What it does
 * in a world (which projectiles it takes, drawing, shooting) isn't modelled, so not declared.
 */
public abstract class ProjectileWeaponItem extends Item {
    public ProjectileWeaponItem(Item.Properties properties) {
        super(properties);
    }
}
