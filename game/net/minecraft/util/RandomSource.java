package net.minecraft.util;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;

/**
 * The stand-in for the game's RandomSource, after Horizon's `horizon_core::random` (`RandomSource`, its default
 * methods) and `horizon-client-render/src/random_source.rs` (`create`): an unseeded one is the Java generator
 * (LegacyRandomSource) with a seed from a shared counter and the clock, a seeded one the same from its seed. Which
 * class each gives, the numbers drawn and the errors are observed (`standins`, probe/RandomProbe.java). nextGaussian
 * refuses (LegacyRandomSource). Not here: forkPositional (the positional factories), the thread-safe and thread-local
 * ones (createThreadSafe, createNewThreadLocalInstance).
 */
public interface RandomSource {
    double GAUSSIAN_SPREAD_FACTOR = 2.297;

    static RandomSource create() {
        return create(RandomSupport.generateUniqueSeed());
    }

    static RandomSource create(long seed) {
        return new LegacyRandomSource(seed);
    }

    RandomSource fork();

    void setSeed(long seed);

    int nextInt();

    int nextInt(int bound);

    /** Horizon's next_int_between_inclusive: nextInt(max - min + 1) + min, wrapping as ints do. */
    default int nextIntBetweenInclusive(int min, int max) {
        return nextInt(max - min + 1) + min;
    }

    long nextLong();

    boolean nextBoolean();

    float nextFloat();

    double nextDouble();

    double nextGaussian();

    /** Horizon's triangle: the first draw less the second. */
    default double triangle(double mean, double deviation) {
        return mean + deviation * (nextDouble() - nextDouble());
    }

    default void consumeCount(int count) {
        for (int i = 0; i < count; i++) nextInt();
    }

    /** Horizon's next_int_between: an empty range draws nothing. The message is observed. */
    default int nextInt(int origin, int bound) {
        if (origin >= bound) throw new IllegalArgumentException("bound - origin is non positive");
        return origin + nextInt(bound - origin);
    }
}
