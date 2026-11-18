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
        boolean noCollission, noOcclusion, noLootTable, liquid, replaceable, dynamicShape, requiresCorrectToolForDrops;

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
    }
}
