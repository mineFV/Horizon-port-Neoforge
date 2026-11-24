package net.minecraft.world.item;

/**
 * The stand-in for the game's BucketItem: an item holding a fluid (`content`, public as NeoForge makes it), made from
 * its properties, its constructor adding nothing to them (observed in a server, probe/BlockItemProbe.java). Using it,
 * emptying it or filling it in the world isn't here.
 */
public class BucketItem extends Item implements DispensibleContainerItem {
    public final net.minecraft.world.level.material.Fluid content;

    public BucketItem(net.minecraft.world.level.material.Fluid content, Item.Properties properties) {
        super(properties);
        this.content = content;
    }
}
