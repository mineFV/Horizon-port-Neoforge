package net.minecraft.world.level.block.state;

import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.level.block.Block;

/**
 * The stand-in for the game's BlockBehaviour: a block's properties, kept as given. What they mean in the world is
 * Horizon's, from the facts `dump` read in NeoForge (the content mod), not from these.
 */
public abstract class BlockBehaviour implements FeatureElement {
    protected final Properties properties;

    protected BlockBehaviour(Properties properties) {
        this.properties = properties;
    }

    /**
     * What a block does when a neighbour changes: nothing, unless the block says otherwise (observed,
     * probe/BlockItemProbe.java, neighbours: a state's handleNeighborChanged runs it and returns, given no level).
     */
    protected void neighborChanged(BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.Block neighborBlock, net.minecraft.core.BlockPos neighborPos, boolean movedByPiston) {}

    /** Its states' fluid: none, unless the block says otherwise (observed). */
    protected net.minecraft.world.level.material.FluidState getFluidState(BlockState state) {
        return net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();
    }

    /**
     * The stand-in for BlockBehaviour.Properties: a builder; each setter gives it back (observed). What they set is kept,
     * not read: what a block does in the world is Horizon's, from `dump`'s facts.
     */
    public static class Properties {
        float destroyTime;
        float explosionResistance;
        java.util.function.Function<BlockState, net.minecraft.world.level.material.MapColor> mapColor;

        Properties() {}

        public Properties mapColor(net.minecraft.world.level.material.MapColor color) {
            mapColor = state -> color;
            return this;
        }

        public Properties mapColor(net.minecraft.world.item.DyeColor color) {
            return mapColor(color.getMapColor());
        }

        StateArgumentPredicate<net.minecraft.world.entity.EntityType<?>> isValidSpawn;
        StatePredicate isSuffocating;
        StatePredicate isViewBlocking;
        java.util.function.ToIntFunction<BlockState> lightEmission;
        net.minecraft.world.level.block.state.properties.NoteBlockInstrument instrument;
        net.minecraft.world.level.material.PushReaction pushReaction;
        boolean noCollission, noOcclusion, noLootTable, liquid, replaceable, dynamicShape, requiresCorrectToolForDrops, isAir;
        net.minecraft.world.level.block.SoundType soundType;
        /** The block of the game's whose properties these copy, which aren't known: its states refuse what they'd tell. */
        String horizonUnknown;

        public Properties sound(net.minecraft.world.level.block.SoundType soundType) {
            this.soundType = soundType;
            return this;
        }

        /**
         * Observed (probe/BlockItemProbe.java, legacy): a copy of a block's properties, which its states answer as the
         * block's do. A block of the game's (but air) has properties that aren't known: so has the copy.
         */
        public static Properties ofLegacyCopy(BlockBehaviour block) {
            Properties from = block.properties;
            if (block instanceof net.minecraft.world.level.block.Block b && b.horizonOther()) {
                if (!BlockStateBase.AIR.contains(String.valueOf(b.horizonId()))) {
                    Properties unknown = new Properties();
                    unknown.horizonUnknown = String.valueOf(b);
                    return unknown;
                }
                from = BlockStateBase.AIR_PROPERTIES;
            }
            Properties copy = new Properties();
            copy.destroyTime = from.destroyTime;
            copy.explosionResistance = from.explosionResistance;
            copy.mapColor = from.mapColor;
            copy.isValidSpawn = from.isValidSpawn;
            copy.isSuffocating = from.isSuffocating;
            copy.isViewBlocking = from.isViewBlocking;
            copy.lightEmission = from.lightEmission;
            copy.instrument = from.instrument;
            copy.pushReaction = from.pushReaction;
            copy.noCollission = from.noCollission;
            copy.noOcclusion = from.noOcclusion;
            copy.noLootTable = from.noLootTable;
            copy.liquid = from.liquid;
            copy.replaceable = from.replaceable;
            copy.dynamicShape = from.dynamicShape;
            copy.requiresCorrectToolForDrops = from.requiresCorrectToolForDrops;
            copy.isAir = from.isAir;
            copy.soundType = from.soundType;
            copy.horizonUnknown = from.horizonUnknown;
            return copy;
        }

        public Properties air() {
            isAir = true;
            return this;
        }

        public Properties isSuffocating(StatePredicate predicate) {
            isSuffocating = predicate;
            return this;
        }

        public Properties isViewBlocking(StatePredicate predicate) {
            isViewBlocking = predicate;
            return this;
        }

        public Properties lightLevel(java.util.function.ToIntFunction<BlockState> light) {
            lightEmission = light;
            return this;
        }

        public Properties instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument instrument) {
            this.instrument = instrument;
            return this;
        }

        public Properties pushReaction(net.minecraft.world.level.material.PushReaction reaction) {
            pushReaction = reaction;
            return this;
        }

        public Properties noCollission() {
            noCollission = true;
            return this;
        }

        public Properties noOcclusion() {
            noOcclusion = true;
            return this;
        }

        public Properties noLootTable() {
            noLootTable = true;
            return this;
        }

        public Properties liquid() {
            liquid = true;
            return this;
        }

        public Properties replaceable() {
            replaceable = true;
            return this;
        }

        public Properties dynamicShape() {
            dynamicShape = true;
            return this;
        }

        public Properties requiresCorrectToolForDrops() {
            requiresCorrectToolForDrops = true;
            return this;
        }

        /** Kept, not asked (spawning is Horizon's). */
        public Properties isValidSpawn(StateArgumentPredicate<net.minecraft.world.entity.EntityType<?>> predicate) {
            isValidSpawn = predicate;
            return this;
        }

        public Properties mapColor(java.util.function.Function<BlockState, net.minecraft.world.level.material.MapColor> color) {
            mapColor = color;
            return this;
        }

        public static Properties of() {
            return new Properties();
        }

        public Properties strength(float destroyTime, float explosionResistance) {
            this.destroyTime = destroyTime;
            this.explosionResistance = Math.max(0.0F, explosionResistance);
            return this;
        }

        public Properties strength(float strength) {
            return strength(strength, strength);
        }
    }

    /** The stand-in for BlockBehaviour.StatePredicate: a test of a state where it is. */
    @FunctionalInterface
    public interface StatePredicate {
        boolean test(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos);
    }

    /** The stand-in for BlockBehaviour.StateArgumentPredicate: a test of a state where it is, with an argument. */
    @FunctionalInterface
    public interface StateArgumentPredicate<A> {
        boolean test(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, A argument);
    }

    /** The stand-in for BlockBehaviour.BlockStateBase: a state of a block. */
    public abstract static class BlockStateBase extends StateHolder<Block, BlockState> {
        protected BlockStateBase(Block owner, java.util.Map<net.minecraft.world.level.block.state.properties.Property<?>, Comparable<?>> values) {
            super(owner, values);
        }

        public Block getBlock() {
            return owner;
        }

        public boolean is(net.minecraft.tags.TagKey<Block> tag) {
            return getBlock().builtInRegistryHolder().is(tag);
        }

        public boolean is(Block block) {
            return owner == block;
        }

        /** Observed (probe/BlockItemProbe.java, neighbours): its block's neighborChanged, given this state and the rest as they come. */
        public void handleNeighborChanged(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, Block neighborBlock, net.minecraft.core.BlockPos neighborPos, boolean movedByPiston) {
            getBlock().neighborChanged((BlockState) this, level, pos, neighborBlock, neighborPos, movedByPiston);
        }

        public net.minecraft.core.Holder<Block> getBlockHolder() {
            return owner.builtInRegistryHolder();
        }

        public java.util.stream.Stream<net.minecraft.tags.TagKey<Block>> getTags() {
            return owner.builtInRegistryHolder().tags();
        }

        /**
         * Its block's properties: the mod's own blocks' as it set them; of the game's blocks only air's are known (the
         * game's air blocks, observed in a server: air, cave_air, void_air), the others' refused.
         */
        private Properties own(String what) {
            if (owner.horizonOther()) {
                if (AIR.contains(String.valueOf(owner.horizonId()))) return AIR_PROPERTIES;
                throw new UnsupportedOperationException(what + " of " + owner + " isn't known: a block of the game's or of another mod's (horizon-port-neoforge)");
            }
            if (owner.properties.horizonUnknown != null) {
                throw new UnsupportedOperationException(what + " of " + owner + " isn't known: its properties copy those of " + owner.properties.horizonUnknown
                        + ", a block of the game's (horizon-port-neoforge)");
            }
            return owner.properties;
        }

        static final java.util.Set<String> AIR = java.util.Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air");
        static final Properties AIR_PROPERTIES = Properties.of().noCollission().noLootTable().air().replaceable();

        /** Observed: its block's say; a block of the game's other than air isn't known, refused. */
        public net.minecraft.world.level.material.FluidState getFluidState() {
            own("its fluid");
            return owner.getFluidState((BlockState) this);
        }

        /** Observed (`standins --horizon-src`): Properties.air(). */
        public boolean isAir() {
            return own("whether it is air").isAir;
        }

        /** Observed: whether its block makes block entities. */
        public boolean hasBlockEntity() {
            own("whether it has a block entity");
            return owner instanceof net.minecraft.world.level.block.EntityBlock;
        }

        /** Observed: its block's destroy time, wherever it is. */
        public float getDestroySpeed(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
            return own("its destroy speed").destroyTime;
        }

        /** Observed: Properties.liquid(). */
        public boolean liquid() {
            return own("whether it is a liquid").liquid;
        }

        /** Observed: unless Properties.noOcclusion() or noCollission(). */
        public boolean canOcclude() {
            Properties p = own("whether it occludes");
            return !p.noOcclusion && !p.noCollission;
        }

        /** Observed: Properties.replaceable(). */
        public boolean canBeReplaced() {
            return own("whether it can be replaced").replaceable;
        }

        /** What depends on the world around it is Horizon's: asking the stand-in says so. */
        private static UnsupportedOperationException world(String what) {
            return new UnsupportedOperationException(what + " in the world isn't modelled (horizon-port-neoforge)");
        }

        public boolean isSolidRender(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
            throw world("whether a block state renders solid");
        }

        public boolean isRedstoneConductor(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
            throw world("whether a block state conducts redstone");
        }

        public boolean isFaceSturdy(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face) {
            throw world("whether a block state's face is sturdy");
        }

        public int getSignal(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction direction) {
            throw world("a block state's signal");
        }

        public boolean canSurvive(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos) {
            throw world("whether a block state survives");
        }

        /** Shapes are a block's in the world, Horizon's (from `dump`'s facts): asking the stand-in says so. */
        private static UnsupportedOperationException shapes() {
            return new UnsupportedOperationException("a block state's shapes aren't modelled (horizon-port-neoforge)");
        }

        public net.minecraft.world.phys.shapes.VoxelShape getBlockSupportShape(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
            throw shapes();
        }

        public net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
            throw shapes();
        }

        public net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
            throw shapes();
        }

        public net.minecraft.world.phys.shapes.VoxelShape getInteractionShape(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
            throw shapes();
        }

        public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
            throw shapes();
        }

        public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
                net.minecraft.world.phys.shapes.CollisionContext context) {
            throw shapes();
        }

        public net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
                net.minecraft.world.phys.shapes.CollisionContext context) {
            throw shapes();
        }

        public net.minecraft.world.phys.shapes.VoxelShape getVisualShape(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
                net.minecraft.world.phys.shapes.CollisionContext context) {
            throw shapes();
        }

    }
}
