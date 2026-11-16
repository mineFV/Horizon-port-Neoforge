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

    /** The stand-in for BlockBehaviour.Properties: a builder; each setter gives it back. */
    public static class Properties {
        float destroyTime;
        float explosionResistance;

        Properties() {}

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

    /** The stand-in for BlockBehaviour.BlockStateBase: a state of a block. */
    public abstract static class BlockStateBase extends StateHolder<Block, BlockState> {
        protected BlockStateBase(Block owner) {
            super(owner);
        }

        public Block getBlock() {
            return owner;
        }

        public boolean is(Block block) {
            return owner == block;
        }
    }
}
