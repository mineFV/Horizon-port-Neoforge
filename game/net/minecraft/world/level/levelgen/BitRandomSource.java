package net.minecraft.world.level.levelgen;

import net.minecraft.util.RandomSource;

/**
 * The stand-in for the game's BitRandomSource: the draws made of next(bits), after Horizon's `LegacyRandom`
 * (`horizon_core::random`), which are java.util.Random's. Observed (`standins`, probe/RandomProbe.java) on seeds and
 * bounds chosen to pin them down (powers of two, bounds whose rejection loop runs, the ints' extremes).
 */
public interface BitRandomSource extends RandomSource {
    float FLOAT_MULTIPLIER = 5.9604645E-8F;
    double DOUBLE_MULTIPLIER = 1.1102230246251565E-16;

    int next(int bits);

    @Override
    default int nextInt() {
        return next(32);
    }

    /** Horizon's next_int_bound: a bound not positive draws nothing; the message is observed. */
    @Override
    default int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("Bound must be positive");
        if ((bound & bound - 1) == 0) return (int) ((long) bound * (long) next(31) >> 31);
        int bits;
        int value;
        do {
            bits = next(31);
            value = bits % bound;
        } while (bits - value + (bound - 1) < 0);
        return value;
    }

    /** The low int is sign-extended before it is added, as Horizon notes. */
    @Override
    default long nextLong() {
        int high = next(32);
        int low = next(32);
        return ((long) high << 32) + (long) low;
    }

    @Override
    default boolean nextBoolean() {
        return next(1) != 0;
    }

    @Override
    default float nextFloat() {
        return (float) next(24) * FLOAT_MULTIPLIER;
    }

    @Override
    default double nextDouble() {
        int high = next(26);
        int low = next(27);
        return (double) (((long) high << 27) + (long) low) * DOUBLE_MULTIPLIER;
    }
}
