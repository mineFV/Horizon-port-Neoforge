package it.unimi.dsi.fastutil;

/**
 * The stand-in for fastutil's HashCommon: how its open hash tables mix hashes and size themselves (observed through
 * the tables' orders, `standins`).
 */
public class HashCommon {
    private static final int INT_PHI = 0x9E3779B9;
    private static final long LONG_PHI = 0x9E3779B97F4A7C15L;

    protected HashCommon() {}

    public static int mix(int x) {
        int h = x * INT_PHI;
        return h ^ (h >>> 16);
    }

    public static long mix(long x) {
        long h = x * LONG_PHI;
        h ^= h >>> 32;
        return h ^ (h >>> 16);
    }

    public static int nextPowerOfTwo(int x) {
        return 1 << (32 - Integer.numberOfLeadingZeros(x - 1));
    }

    public static long nextPowerOfTwo(long x) {
        return 1L << (64 - Long.numberOfLeadingZeros(x - 1));
    }

    public static int maxFill(int n, float f) {
        return Math.min((int) Math.ceil(n * f), n - 1);
    }

    public static int arraySize(int expected, float f) {
        long s = Math.max(2, nextPowerOfTwo((long) Math.ceil(expected / f)));
        if (s > (1 << 30)) throw new IllegalArgumentException("Too large (" + expected + " expected elements with load factor " + f + ")");
        return (int) s;
    }
}
