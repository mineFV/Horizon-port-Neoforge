package horizonneo.probe;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.BitRandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;

/**
 * The game's random numbers, as GameProbe observes the rest (see there): seeded generators drawn from on seeds and
 * bounds chosen to pin them down (powers of two, bounds whose rejection loop runs, the ints' and longs' extremes,
 * bounds that wrap), the draw after an error (whether it took a number), forks and reseeding.
 */
final class RandomProbe {
    private RandomProbe() {}

    static final long[] SEEDS = {0L, 1L, -1L, 42L, 0x5DEECE66DL, 12345678901234L, Long.MIN_VALUE, Long.MAX_VALUE};
    static final int[] BOUNDS = {1, 2, 3, 7, 10, 16, 100, 1 << 30, (1 << 30) + 1, 1500000000, Integer.MAX_VALUE, 0, -1,
        Integer.MIN_VALUE};
    static final int MAX = Integer.MAX_VALUE, MIN = Integer.MIN_VALUE;
    static final int[][] RANGES = {{0, 0}, {5, 5}, {5, 4}, {6, 5}, {-10, 10}, {0, MAX}, {-1, MAX}, {MIN, MAX}, {MIN, 0},
        {MIN, -1}, {MAX, MIN}, {1, MIN}, {-5, 1 << 30}};

    static void run() {
        p("random constants", () -> Double.doubleToLongBits(RandomSource.GAUSSIAN_SPREAD_FACTOR) + " "
                + Float.floatToIntBits(BitRandomSource.FLOAT_MULTIPLIER) + " " + Double.doubleToLongBits(BitRandomSource.DOUBLE_MULTIPLIER)
                + " " + RandomSupport.GOLDEN_RATIO_64 + " " + RandomSupport.SILVER_RATIO_64);
        p("random create class", () -> RandomSource.create().getClass().getName());
        p("random create new", () -> {
            RandomSource a = RandomSource.create();
            RandomSource b = RandomSource.create();
            return (a != b) + " " + (a.nextLong() != b.nextLong() || a.nextLong() != b.nextLong());
        });
        p("random unique seeds", () -> RandomSupport.generateUniqueSeed() != RandomSupport.generateUniqueSeed());
        for (long v : new long[] {0L, 1L, -1L, 42L, Long.MIN_VALUE, Long.MAX_VALUE, RandomSupport.GOLDEN_RATIO_64, 0x123456789ABCDEFL}) {
            p("random stafford " + v, () -> RandomSupport.mixStafford13(v));
        }
        for (long seed : SEEDS) {
            String k = "random " + seed;
            p(k + " class", () -> RandomSource.create(seed).getClass().getName());
            p(k + " ints", () -> {
                RandomSource r = RandomSource.create(seed);
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < 6; i++) s.append(r.nextInt()).append(' ');
                return s;
            });
            p(k + " others", () -> {
                RandomSource r = RandomSource.create(seed);
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < 4; i++) {
                    s.append(r.nextLong()).append(' ').append(r.nextBoolean()).append(' ')
                            .append(Float.floatToIntBits(r.nextFloat())).append(' ')
                            .append(Double.doubleToLongBits(r.nextDouble())).append(' ');
                }
                return s;
            });
            for (int bound : BOUNDS) {
                p(k + " bound " + bound, () -> {
                    RandomSource r = RandomSource.create(seed);
                    StringBuilder s = new StringBuilder();
                    for (int i = 0; i < 8; i++) s.append(r.nextInt(bound)).append(' ');
                    return s;
                });
            }
            p(k + " bound error then", () -> after(seed, r -> r.nextInt(0)));
            for (int[] range : RANGES) {
                int a = range[0], b = range[1];
                p(k + " between " + a + " " + b, () -> {
                    RandomSource r = RandomSource.create(seed);
                    StringBuilder s = new StringBuilder();
                    for (int i = 0; i < 5; i++) s.append(r.nextIntBetweenInclusive(a, b)).append(' ');
                    return s;
                });
                p(k + " range " + a + " " + b, () -> {
                    RandomSource r = RandomSource.create(seed);
                    StringBuilder s = new StringBuilder();
                    for (int i = 0; i < 5; i++) s.append(r.nextInt(a, b)).append(' ');
                    return s;
                });
                p(k + " range error then " + a + " " + b, () -> after(seed, r -> r.nextInt(a, b)));
                p(k + " between error then " + a + " " + b, () -> after(seed, r -> r.nextIntBetweenInclusive(a, b)));
            }
            p(k + " triangle", () -> {
                RandomSource r = RandomSource.create(seed);
                return Double.doubleToLongBits(r.triangle(0.0, 1.0)) + " " + Double.doubleToLongBits(r.triangle(5.5, -2.25))
                        + " " + Double.doubleToLongBits(r.triangle(-1e300, 1e300));
            });
            for (int count : new int[] {0, 1, 3, -1, MIN}) {
                p(k + " consume " + count, () -> {
                    RandomSource r = RandomSource.create(seed);
                    r.consumeCount(count);
                    return r.nextInt();
                });
            }
            p(k + " fork", () -> {
                RandomSource r = RandomSource.create(seed);
                RandomSource f = r.fork();
                return f.getClass().getName() + " " + (f != r) + " " + f.nextInt() + " " + r.nextInt() + " " + f.nextLong();
            });
            p(k + " reseed", () -> {
                RandomSource r = RandomSource.create(7L);
                r.nextInt();
                r.nextDouble();
                r.setSeed(seed);
                return r.nextInt() + " " + r.nextLong();
            });
            p(k + " bits", () -> {
                LegacyRandomSource r = new LegacyRandomSource(seed);
                StringBuilder s = new StringBuilder();
                for (int bits : new int[] {0, 1, 2, 8, 16, 24, 26, 27, 31, 32, 33, 40, 47, 48, 49, 63, 64, -1, MIN}) {
                    s.append(r.next(bits)).append(' ');
                }
                return s;
            });
        }
        p("random direct", () -> {
            LegacyRandomSource r = new LegacyRandomSource(99L);
            return r.nextInt() + " " + r.nextInt(17) + " " + (r instanceof BitRandomSource);
        });
    }

    /** What a call gives (or throws), then the next int: the same as a fresh generator's first if it drew nothing. */
    static String after(long seed, java.util.function.ToIntFunction<RandomSource> call) {
        RandomSource r = RandomSource.create(seed);
        String first;
        try {
            first = String.valueOf(call.applyAsInt(r));
        } catch (RuntimeException e) {
            first = "throws " + e.getClass().getName() + ": " + e.getMessage();
        }
        return first + " / " + r.nextInt() + " (fresh " + RandomSource.create(seed).nextInt() + ")";
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
