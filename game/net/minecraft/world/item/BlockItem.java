package net.minecraft.world.item;

import net.minecraft.world.level.block.Block;

/**
 * The stand-in for the game's BlockItem: an item that stands for a block, which a block then gives as its item
 * (Item.BY_BLOCK, filled when the item is registered: HorizonRegistries). Placing it in a world is Horizon's.
 */
public class BlockItem extends Item {
    private final Block block;

    public BlockItem(Block block, Item.Properties properties) {
        super(properties);
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    /** Observed (`standins --horizon-src`): its block's, unless it is the game's and its own was given. */
    @Override
    public String getDescriptionId() {
        String given = horizonGivenName();
        return given != null ? given : block.getDescriptionId();
    }

    /** Observed: its block to it. */
    public void registerBlocks(java.util.Map<Block, Item> blockToItemMap, Item item) {
        blockToItemMap.put(block, item);
    }

    public void removeFromBlockToItemMap(java.util.Map<Block, Item> blockToItemMap, Item item) {
        blockToItemMap.remove(block);
    }
}
