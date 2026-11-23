package it.unimi.dsi.fastutil.ints;

/** The stand-in for fastutil's IntArrays: its two empty arrays alone (one marks a list never given a capacity). */
public final class IntArrays {
    private IntArrays() {}

    public static final int[] EMPTY_ARRAY = {};

    public static final int[] DEFAULT_EMPTY_ARRAY = {};
}
