package net.minecraft.util;

import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/** The stand-in for the game's ByIdMap: values by their ids, observed (`standins`, probe/IdProbe.java). */
public class ByIdMap {
    public ByIdMap() {}

    /**
     * Observed: the values sorted by id, which must be 0 to n - 1, each once (else an IllegalArgumentException, at the
     * first value out of range or seen twice, in the values' order); out of range, ZERO gives the first, WRAP the id
     * modulo n (never negative), CLAMP the nearest end.
     */
    public static <T> IntFunction<T> continuous(ToIntFunction<T> key, T[] values, OutOfBoundsStrategy strategy) {
        T[] sorted = sorted(key, values);
        int n = sorted.length;
        return switch (strategy) {
            case ZERO -> {
                T first = sorted[0];
                yield i -> i >= 0 && i < n ? sorted[i] : first;
            }
            case WRAP -> i -> sorted[Math.floorMod(i, n)];
            case CLAMP -> i -> sorted[i < 0 ? 0 : Math.min(i, n - 1)];
        };
    }

    private static <T> T[] sorted(ToIntFunction<T> key, T[] values) {
        int n = values.length;
        if (n == 0) throw new IllegalArgumentException("Empty value list");
        T[] out = values.clone();
        for (int i = 0; i < n; i++) out[i] = null;
        for (T v : values) {
            int id = key.applyAsInt(v);
            if (id < 0 || id >= n) throw new IllegalArgumentException("Values are not continous, found index " + id + " for value " + v);
            T previous = out[id];
            if (previous != null) throw new IllegalArgumentException("Duplicate entry on id " + id + ": current=" + v + ", previous=" + previous);
            out[id] = v;
        }
        return out;
    }

    /** The stand-in for ByIdMap.OutOfBoundsStrategy. */
    public enum OutOfBoundsStrategy {
        ZERO,
        WRAP,
        CLAMP
    }
}
