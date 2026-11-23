package net.minecraft.world.level.dimension;

/**
 * The stand-in for the game's DimensionType: its height limits alone, observed (`standins --horizon-src`,
 * probe/ChunkProbe.java). Not here: a dimension's own type, its codecs, the moon's brightness.
 */
public record DimensionType() {
    public static final int BITS_FOR_Y = 12;
    public static final int MIN_HEIGHT = 16;
    public static final int Y_SIZE = 4064;
    public static final int MAX_Y = 2031;
    public static final int MIN_Y = -2032;
    public static final int WAY_ABOVE_MAX_Y = 32496;
    public static final int WAY_BELOW_MIN_Y = -32512;
}
