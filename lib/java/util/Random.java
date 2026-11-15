package java.util;

/**
 * The model of java.util.Random: the linear congruential generator its specification gives (the 48-bit seed, next,
 * nextInt with a bound, nextLong, nextDouble...), so that a seeded Random gives the JDK's numbers. new Random()'s
 * seed comes from nanoTime (the JDK's too, mixed with a counter of its own). Its methods hold the object's monitor:
 * the JDK's are safe from several threads. Not modelled: nextGaussian (it needs StrictMath.log), the streams, the
 * bounded ranges of RandomGenerator (nextInt(origin, bound)...).
 */
public class Random implements java.io.Serializable {
    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long ADDEND = 0xBL;
    private static final long MASK = (1L << 48) - 1;

    private static long seedUniquifier = 8682522807148012L;

    private long seed;

    public Random() {
        this(nextUniquifier() ^ System.nanoTime());
    }

    private static synchronized long nextUniquifier() {
        seedUniquifier *= 1181783497276652981L;
        return seedUniquifier;
    }

    public Random(long seed) {
        this.seed = initialScramble(seed);
    }

    private static long initialScramble(long seed) {
        return (seed ^ MULTIPLIER) & MASK;
    }

    public synchronized void setSeed(long seed) {
        this.seed = initialScramble(seed);
    }

    protected synchronized int next(int bits) {
        seed = (seed * MULTIPLIER + ADDEND) & MASK;
        return (int) (seed >>> (48 - bits));
    }

    public void nextBytes(byte[] bytes) {
        for (int i = 0, len = bytes.length; i < len; ) {
            for (int rnd = nextInt(), n = Math.min(len - i, 4); n-- > 0; rnd >>= 8) bytes[i++] = (byte) rnd;
        }
    }

    public int nextInt() {
        return next(32);
    }

    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        int r = next(31);
        int m = bound - 1;
        if ((bound & m) == 0) {
            r = (int) ((bound * (long) r) >> 31);
        } else {
            for (int u = r; u - (r = u % bound) + m < 0; u = next(31)) {
            }
        }
        return r;
    }

    public long nextLong() {
        return ((long) (next(32)) << 32) + next(32);
    }

    public boolean nextBoolean() {
        return next(1) != 0;
    }

    public float nextFloat() {
        return next(24) / ((float) (1 << 24));
    }

    public double nextDouble() {
        return (((long) (next(26)) << 27) + next(27)) * 0x1.0p-53;
    }
}
