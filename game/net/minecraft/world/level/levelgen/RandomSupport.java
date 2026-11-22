package net.minecraft.world.level.levelgen;

/**
 * The stand-in for the game's RandomSupport: generateUniqueSeed after Horizon's
 * `horizon-client-render/src/random_source.rs` (a counter shared by every caller, stepped by multiplying, mixed with
 * the clock: its numbers change from run to run, as the game's do) and mixStafford13 after `horizon_core::random`,
 * observed (`standins`, probe/RandomProbe.java). Not here: the 128-bit seeds (upgradeSeedTo128bit, seedFromHashOf).
 */
public final class RandomSupport {
    public static final long GOLDEN_RATIO_64 = -7046029254386353131L;
    public static final long SILVER_RATIO_64 = 7640891576956012809L;

    private static long uniquifier = 8682522807148012L;

    public RandomSupport() {}

    public static long mixStafford13(long value) {
        value = (value ^ value >>> 30) * -4658895280553007687L;
        value = (value ^ value >>> 27) * -7723592293110705685L;
        return value ^ value >>> 31;
    }

    public static long generateUniqueSeed() {
        return nextUniquifier() ^ System.nanoTime();
    }

    /** The counter's next value, one caller at a time (Horizon's compare-and-swap loop). */
    private static synchronized long nextUniquifier() {
        uniquifier *= 1181783497276652981L;
        return uniquifier;
    }
}
