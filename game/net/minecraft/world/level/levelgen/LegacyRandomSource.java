package net.minecraft.world.level.levelgen;

import net.minecraft.util.RandomSource;

/**
 * The stand-in for the game's LegacyRandomSource, after Horizon's `LegacyRandom` (`horizon_core::random`): Java's
 * 48-bit linear congruential generator, seeded as java.util.Random is, and forked from its next long. Observed
 * (`standins`, probe/RandomProbe.java). Not here: forkPositional (see RandomSource); draws from several threads at
 * once.
 */
public class LegacyRandomSource implements BitRandomSource {
    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long INCREMENT = 11L;
    private static final long MASK = (1L << 48) - 1;

    private long seed;

    public LegacyRandomSource(long seed) {
        setSeed(seed);
    }

    @Override
    public RandomSource fork() {
        return new LegacyRandomSource(nextLong());
    }

    @Override
    public void setSeed(long seed) {
        this.seed = (seed ^ MULTIPLIER) & MASK;
    }

    /**
     * Refused: Horizon draws it with Marsaglia's polar method over Java's Math.log (`horizon_core::random`,
     * `marsaglia_polar`), whose last bit the model has no exact counterpart for yet.
     */
    @Override
    public double nextGaussian() {
        throw new UnsupportedOperationException("RandomSource.nextGaussian isn't modelled (horizon-port-neoforge): it needs Java's Math.log to the last bit");
    }

    @Override
    public int next(int bits) {
        seed = seed * MULTIPLIER + INCREMENT & MASK;
        return (int) (seed >> 48 - bits);
    }
}
