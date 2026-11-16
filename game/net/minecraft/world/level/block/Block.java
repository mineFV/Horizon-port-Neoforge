package net.minecraft.world.level.block;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockExtension;

/** The stand-in for the game's Block: its default state (one, as a block without properties has). */
public class Block extends BlockBehaviour implements ItemLike, IBlockExtension {
    private BlockState defaultState;
    private net.minecraft.resources.ResourceLocation horizonId;

    public Block(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public final BlockState defaultBlockState() {
        if (defaultState == null) defaultState = new BlockState(this);
        return defaultState;
    }

    /**
     * Observed in a NeoForge server (`dump`'s exporter, `texts`): "Block{" and its id "}". Its id is the one it was
     * registered under, which HorizonRegistries gives it when it is made.
     */
    @Override
    public String toString() {
        return "Block{" + horizonId + "}";
    }

    /** HorizonRegistries': the id this block was registered under. Not the game's API. */
    public void horizonRegistered(net.minecraft.resources.ResourceLocation id) {
        this.horizonId = id;
    }
}
