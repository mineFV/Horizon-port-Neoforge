package net.minecraft.world.level.block;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockExtension;

/** The stand-in for the game's Block: its default state (one, as a block without properties has). */
public class Block extends BlockBehaviour implements ItemLike, IBlockExtension {
    /** Its states: those of the properties it adds (createBlockStateDefinition), one when it adds none. */
    protected final net.minecraft.world.level.block.state.StateDefinition<Block, BlockState> stateDefinition;
    private BlockState defaultState;
    private net.minecraft.resources.ResourceLocation horizonId;

    public Block(BlockBehaviour.Properties properties) {
        super(properties);
        net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder = new net.minecraft.world.level.block.state.StateDefinition.Builder<>(this);
        createBlockStateDefinition(builder);
        this.stateDefinition = builder.horizonCreate(BlockState::new);
        registerDefaultState(stateDefinition.any());
    }

    /** The properties its states have: none here, a mod's block adds its own. */
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {}

    protected final void registerDefaultState(BlockState state) {
        this.defaultState = state;
    }

    public final BlockState defaultBlockState() {
        return defaultState;
    }

    public net.minecraft.world.level.block.state.StateDefinition<Block, BlockState> getStateDefinition() {
        return stateDefinition;
    }

    /** HorizonWorld's: the id this block was registered under. Not the game's API. */
    public net.minecraft.resources.ResourceLocation horizonId() {
        return horizonId;
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
