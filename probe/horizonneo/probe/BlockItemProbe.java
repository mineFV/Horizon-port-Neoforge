package horizonneo.probe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Blocks' items: a block of the probe's with a BlockItem, one without, and the game's (stone, a block's item; diamond,
 * not one), which the stand-ins are given as `dump` gives them (ItemProbe).
 */
final class BlockItemProbe {
    private BlockItemProbe() {}

    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("horizonprobe");
    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "horizonprobe");
    static final DeferredBlock<Block> WITH = BLOCKS.register("with_item", () -> new Block(BlockBehaviour.Properties.of()));
    static final DeferredBlock<Block> WITHOUT = BLOCKS.register("without_item", () -> new Block(BlockBehaviour.Properties.of()));
    static final DeferredBlock<Block> AIRY = BLOCKS.register("airy", () -> new Block(BlockBehaviour.Properties.of().air()));
    static final DeferredBlock<Block> FLUID = BLOCKS.register("fluid", () -> new Block(BlockBehaviour.Properties.of().liquid()));
    static final DeferredBlock<Block> CLEAR = BLOCKS.register("clear", () -> new Block(BlockBehaviour.Properties.of().noOcclusion()));
    static final DeferredBlock<Block> GHOST = BLOCKS.register("ghost", () -> new Block(BlockBehaviour.Properties.of().noCollission()));
    static final DeferredBlock<Block> GRASSY = BLOCKS.register("grassy", () -> new Block(BlockBehaviour.Properties.of().replaceable()));
    static final DeferredBlock<Block> HARD = BLOCKS.register("hard", () -> new Block(BlockBehaviour.Properties.of().strength(3.5F, 4)));
    static final DeferredBlock<Block> WET = BLOCKS.register("wet", () -> new Block(BlockBehaviour.Properties.of()) {
        @Override
        protected net.minecraft.world.level.material.FluidState getFluidState(net.minecraft.world.level.block.state.BlockState state) {
            return net.minecraft.world.level.material.Fluids.WATER.getSource(false);
        }
    });
    static final DeferredHolder<Item, BlockItem> ITEM = ITEMS.register("with_item", () -> new BlockItem(WITH.get(), new Item.Properties()));

    static void run() {
        java.util.List<DeferredBlock<Block>> blocks = java.util.List.of(WITH, AIRY, FLUID, CLEAR, GHOST, GRASSY, HARD, WET, CapabilityProbe.BLOCK);
        net.minecraft.world.level.material.Fluid[] fluids = {net.minecraft.world.level.material.Fluids.EMPTY, net.minecraft.world.level.material.Fluids.WATER, net.minecraft.world.level.material.Fluids.LAVA};
        for (net.minecraft.world.level.material.Fluid f : fluids) {
            p("fluid state " + f, () -> {
                net.minecraft.world.level.material.FluidState s = f.defaultFluidState();
                return s + " " + s.isEmpty() + " " + s.isSource() + " " + s.getAmount() + " " + s.getType() + " " + (s == f.defaultFluidState()) + " " + s.getValues().size()
                        + " " + s.is(f) + " " + s.is(net.minecraft.world.level.material.Fluids.WATER) + " " + f.isSame(net.minecraft.world.level.material.Fluids.WATER) + " " + f.getStateDefinition();
            });
        }
        p("fluid sources", () -> {
            net.minecraft.world.level.material.FlowingFluid w = net.minecraft.world.level.material.Fluids.WATER, l = net.minecraft.world.level.material.Fluids.FLOWING_LAVA;
            return w.getSource(false) + " " + w.getSource(true) + " " + (w.getSource(false) == w.defaultFluidState()) + " " + w.getSource(true).isSource() + " " + w.getSource(true).getAmount()
                    + " " + l.getSource(true) + " " + w.getSource() + " " + w.getFlowing() + " " + l.getSource() + " " + l.getFlowing()
                    + " " + net.minecraft.world.level.material.Fluids.FLOWING_WATER.getSource();
        });
        for (DeferredBlock<Block> b : blocks) {
            p("block state " + b.getId(), () -> {
                net.minecraft.world.level.block.state.BlockState s = b.get().defaultBlockState();
                return s.isAir() + " " + s.liquid() + " " + s.canOcclude() + " " + s.canBeReplaced() + " " + s.getDestroySpeed(null, null) + " " + s.hasBlockEntity()
                        + " " + s.getBlockHolder() + " " + (s.getBlockHolder() == b.get().builtInRegistryHolder()) + " " + s.getTags().map(Object::toString).sorted().toList()
                        + " " + s.getFluidState() + " " + (s.getFluidState() == s.getFluidState());
            });
        }
        for (String id : java.util.List.of("air", "cave_air", "void_air")) {
            p("block state game " + id, () -> {
                net.minecraft.world.level.block.state.BlockState s = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(id)).defaultBlockState();
                return s.isAir() + " " + s.liquid() + " " + s.canOcclude() + " " + s.canBeReplaced() + " " + s.getDestroySpeed(null, null) + " " + s.hasBlockEntity() + " " + s.getFluidState();
            });
        }
        p("block item", () -> WITH.get().asItem() + " " + (WITH.get().asItem() == ITEM.get()) + " " + ITEM.get().getBlock() + " " + ITEM.get().getDescriptionId()
                + " " + ITEM.get().getDescription() + " " + WITH.asItem() + " " + WITHOUT.get().asItem() + " " + Item.byBlock(WITHOUT.get()) + " " + Block.byItem(ITEM.get())
                + " " + Block.byItem(Items.DIAMOND) + " " + Item.BY_BLOCK.get(WITH.get()) + " " + Item.BY_BLOCK.get(WITHOUT.get()));
        p("block item game", () -> {
            Block stone = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace("stone"));
            Block air = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace("air"));
            return (Items.STONE instanceof BlockItem) + " " + ((BlockItem) Items.STONE).getBlock() + " " + (((BlockItem) Items.STONE).getBlock() == stone) + " "
                    + Items.STONE.getDescriptionId() + " " + (Items.DIAMOND instanceof BlockItem) + " " + (Items.AIR instanceof BlockItem) + " " + stone.asItem()
                    + " " + (stone.asItem() == Items.STONE) + " " + air.asItem() + " " + (Block.byItem(Items.STONE) == stone) + " " + Block.byItem(Items.AIR) + " " + (Block.byItem(Items.AIR) == air);
        });
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
