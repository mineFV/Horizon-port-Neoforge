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

    /** Its item (Item.byBlock), else air. */
    @Override
    public net.minecraft.world.item.Item asItem() {
        return net.minecraft.world.item.Item.byBlock(this);
    }

    /** Observed (probe/ShapeProbe.java): Shapes.box in sixteenths of a block. */
    public static net.minecraft.world.phys.shapes.VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return net.minecraft.world.phys.shapes.Shapes.box(x1 / 16.0, y1 / 16.0, z1 / 16.0, x2 / 16.0, y2 / 16.0, z2 / 16.0);
    }

    /** Observed (probe/BlockItemProbe.java, clones): a new stack of its item, one of it (empty for air). */
    public net.minecraft.world.item.ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos, BlockState state) {
        return new net.minecraft.world.item.ItemStack(this);
    }

    /** Observed: a BlockItem's block, else air. */
    public static Block byItem(net.minecraft.world.item.Item item) {
        return item instanceof net.minecraft.world.item.BlockItem blockItem ? blockItem.getBlock() : net.neoforged.neoforge.registries.HorizonRegistries.block("minecraft:air");
    }

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
    private net.minecraft.core.Holder.Reference<Block> holder;

    /** Its holder: one for each block, its key the block registry's and its id. */
    public net.minecraft.core.Holder.Reference<Block> builtInRegistryHolder() {
        if (holder == null) holder = new net.minecraft.core.Holder.Reference<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, horizonId), this);
        return holder;
    }

    private String descriptionId;

    /** Observed: "block." then its id, its path's slashes as dots. */
    public String getDescriptionId() {
        if (descriptionId == null) descriptionId = "block." + horizonId.getNamespace() + "." + horizonId.getPath().replace('/', '.');
        return descriptionId;
    }

    public net.minecraft.network.chat.MutableComponent getName() {
        return net.minecraft.network.chat.Component.translatable(getDescriptionId());
    }

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

    private boolean horizonOther;

    /** HorizonRegistries': this block only stands for one of the game's or another mod's, whose properties aren't known. Not the game's API. */
    public void horizonOther(boolean other) {
        this.horizonOther = other;
    }

    public boolean horizonOther() {
        return horizonOther;
    }

    /** HorizonRegistries': the id this block was registered under. Not the game's API. */
    public void horizonRegistered(net.minecraft.resources.ResourceLocation id) {
        this.horizonId = id;
    }
}
