package net.minecraft.world.level.block.state;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.extensions.IBlockStateExtension;

/** The stand-in for the game's BlockState: one state of a block (a block without properties has one). */
public class BlockState extends BlockBehaviour.BlockStateBase implements IBlockStateExtension {
    /** Block's: the game's own constructor takes the state's properties, which aren't modelled. */
    public BlockState(Block owner) {
        super(owner);
    }
}
