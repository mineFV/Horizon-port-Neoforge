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

    /** Old ids for the items' registry (NeoForge's aliases): one, again to another, to itself, through another, to nothing. */
    static {
        ITEMS.addAlias(ResourceLocation.parse("horizonprobe:old_item"), ResourceLocation.parse("horizonprobe:with_item"));
        ITEMS.addAlias(ResourceLocation.parse("horizonprobe:old_item"), ResourceLocation.parse("horizonprobe:other_item"));
        ITEMS.addAlias(ResourceLocation.parse("horizonprobe:same_item"), ResourceLocation.parse("horizonprobe:same_item"));
        ITEMS.addAlias(ResourceLocation.parse("horizonprobe:older_item"), ResourceLocation.parse("horizonprobe:old_item"));
        ITEMS.addAlias(ResourceLocation.parse("horizonprobe:lost_item"), ResourceLocation.parse("horizonprobe:missing_item"));
    }
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
    /** What CLONE's own getCloneItemStack was given, each time it ran. */
    static final java.util.List<String> CLONED = new java.util.ArrayList<>();
    static final DeferredBlock<Block> CLONE = BLOCKS.register("clone", () -> new Block(BlockBehaviour.Properties.of()) {
        @Override
        public net.minecraft.world.item.ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            CLONED.add(level + " " + pos + " " + (state == defaultBlockState()));
            return new net.minecraft.world.item.ItemStack(Items.DIAMOND, 3);
        }
    });
    /** What PROGRESS's own getDestroyProgress was given, each time it ran. */
    static final java.util.List<String> PROGRESSED = new java.util.ArrayList<>();
    static final DeferredBlock<Block> PROGRESS = BLOCKS.register("progress", () -> new Block(BlockBehaviour.Properties.of()) {
        @Override
        protected float getDestroyProgress(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.entity.player.Player player,
                net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
            PROGRESSED.add(player + " " + level + " " + pos + " " + (state == defaultBlockState()));
            return 0.25F;
        }
    });
    /** What NEIGHBOR's own neighborChanged was given, each time it ran. */
    static final java.util.List<String> NEIGHBORED = new java.util.ArrayList<>();
    static final DeferredBlock<Block> NEIGHBOR = BLOCKS.register("neighbor", () -> new Block(BlockBehaviour.Properties.of()) {
        @Override
        protected void neighborChanged(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                Block neighborBlock, net.minecraft.core.BlockPos neighborPos, boolean movedByPiston) {
            NEIGHBORED.add(level + " " + pos + " " + (state == defaultBlockState()) + " " + neighborBlock + " " + neighborPos + " " + movedByPiston);
        }

        @Override
        public void onNeighborChange(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                net.minecraft.core.BlockPos neighbor) {
            NEIGHBORED.add("change " + level + " " + pos + " " + (state == defaultBlockState()) + " " + neighbor);
        }
    });
    /** Blocks made with a copy of another's properties (ofLegacyCopy): their states answer as the other's. */
    static final DeferredBlock<Block> LEGACY_HARD = BLOCKS.register("legacy_hard", () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(HARD.get())));
    static final DeferredBlock<Block> LEGACY_CLEAR = BLOCKS.register("legacy_clear", () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(CLEAR.get())));
    static final DeferredBlock<Block> LEGACY_GRASSY = BLOCKS.register("legacy_grassy", () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(GRASSY.get())));
    static final DeferredBlock<Block> LEGACY_FLUID = BLOCKS.register("legacy_fluid", () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(FLUID.get())));
    static final DeferredBlock<Block> LEGACY_AIRY = BLOCKS.register("legacy_airy", () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(AIRY.get())));
    static final DeferredBlock<Block> LEGACY_GHOST = BLOCKS.register("legacy_ghost", () -> new Block(BlockBehaviour.Properties.ofLegacyCopy(GHOST.get())
            .sound(net.minecraft.world.level.block.SoundType.SAND)));
    static final DeferredHolder<Item, BlockItem> ITEM = ITEMS.register("with_item", () -> new BlockItem(WITH.get(), new Item.Properties()));

    static void run() {
        java.util.List<DeferredBlock<Block>> blocks = java.util.List.of(WITH, AIRY, FLUID, CLEAR, GHOST, GRASSY, HARD, WET, CapabilityProbe.BLOCK,
                LEGACY_HARD, LEGACY_CLEAR, LEGACY_GRASSY, LEGACY_FLUID, LEGACY_AIRY, LEGACY_GHOST);
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
        clones();
        neighbours();
        progress();
        aliases();
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

    /** Looking items up by their old ids (aliases): what the registry finds, says it holds, and the keys it gives. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void aliases() {
        net.minecraft.core.Registry items = BuiltInRegistries.ITEM;
        for (String old : new String[] {"old_item", "older_item", "same_item", "lost_item", "with_item"}) {
            ResourceLocation id = ResourceLocation.parse("horizonprobe:" + old);
            p("item alias " + old, () -> {
                Object found = items.get(id);
                return found + " " + (found == ITEM.get()) + " " + items.containsKey(id) + " " + items.getOptional(id).isPresent() + " "
                        + items.get(net.minecraft.resources.ResourceKey.create(Registries.ITEM, id)) + " " + items.containsKey(net.minecraft.resources.ResourceKey.create(Registries.ITEM, id))
                        + " " + items.getHolder(id).map(h -> ((net.minecraft.core.Holder.Reference) h).key()).orElse(null) + " " + items.getKey(found);
            });
        }
    }

    /** A state's getDestroyProgress: its block's, a block's own (the game's needs a player in a world: not asked). */
    static void progress() {
        p("block destroy progress", () -> {
            PROGRESSED.clear();
            float f = PROGRESS.get().defaultBlockState().getDestroyProgress(null, null, new net.minecraft.core.BlockPos(4, 5, 6));
            return f + " " + PROGRESSED;
        });
    }

    /** A state's handleNeighborChanged: its block's neighborChanged, a block's own or the game's (which does nothing). */
    static void neighbours() {
        p("block neighbor changed", () -> {
            NEIGHBORED.clear();
            net.minecraft.core.BlockPos at = new net.minecraft.core.BlockPos(1, 2, 3), from = new net.minecraft.core.BlockPos(1, 3, 3);
            NEIGHBOR.get().defaultBlockState().handleNeighborChanged(null, at, WITH.get(), from, true);
            NEIGHBOR.get().defaultBlockState().handleNeighborChanged(null, from, NEIGHBOR.get(), at, false);
            WITH.get().defaultBlockState().handleNeighborChanged(null, at, NEIGHBOR.get(), from, false);
            NEIGHBOR.get().defaultBlockState().onNeighborChange(null, at, from);
            WITH.get().defaultBlockState().onNeighborChange(null, from, at);
            return NEIGHBORED;
        });
    }

    /** A block's stack for a player's pick: its own (3 arguments), and NeoForge's (5), which runs a block's own. */
    static void clones() {
        net.minecraft.core.BlockPos at = new net.minecraft.core.BlockPos(1, 2, 3);
        for (DeferredBlock<Block> b : java.util.List.of(WITH, WITHOUT, CLONE)) {
            p("block clone " + b.getId(), () -> {
                CLONED.clear();
                net.minecraft.world.item.ItemStack s = b.get().getCloneItemStack(null, at, b.get().defaultBlockState());
                net.minecraft.world.item.ItemStack t = b.get().getCloneItemStack(b.get().defaultBlockState(), null, null, at, null);
                return s.getItem() + " " + s.getCount() + " " + s.isEmpty() + " " + (s == net.minecraft.world.item.ItemStack.EMPTY) + " "
                        + t.getItem() + " " + t.getCount() + " " + t.isEmpty() + " " + (t == net.minecraft.world.item.ItemStack.EMPTY) + " " + CLONED;
            });
        }
        p("block clone other state", () -> {
            CLONED.clear();
            net.minecraft.world.item.ItemStack s = WITH.get().getCloneItemStack(CLONE.get().defaultBlockState(), null, null, at, null);
            net.minecraft.world.item.ItemStack t = CLONE.get().getCloneItemStack(WITH.get().defaultBlockState(), null, null, at, null);
            return s.getItem() + " " + s.getCount() + " " + t.getItem() + " " + t.getCount() + " " + CLONED;
        });
        p("block clone game", () -> {
            Block stone = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace("stone"));
            Block air = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace("air"));
            net.minecraft.world.item.ItemStack s = stone.getCloneItemStack(null, at, stone.defaultBlockState());
            net.minecraft.world.item.ItemStack a = air.getCloneItemStack(air.defaultBlockState(), null, null, at, null);
            return s.getItem() + " " + s.getCount() + " " + a.getItem() + " " + a.isEmpty() + " " + (a == net.minecraft.world.item.ItemStack.EMPTY);
        });
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
