package net.minecraft.world.level;

import net.minecraft.core.BlockPos;

/**
 * The stand-in for the game's ChunkPos: a chunk's coordinates, packed in a long (x in the low 32 bits, z in the high
 * ones) and a block position's chunk (its coordinates shifted by 4), as Horizon's horizon_core::position::ChunkPos
 * (MIT); its hash, text, bounds, regions and distances observed (`standins`, probe/ChunkProbe.java).
 */
public class ChunkPos {
    public static final long INVALID_CHUNK_POS = asLong(1875066, 1875066);
    public static final ChunkPos ZERO = new ChunkPos(0, 0);
    public static final int REGION_SIZE = 32;
    public static final int REGION_MAX_INDEX = 31;
    public final int x;
    public final int z;

    public ChunkPos(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public ChunkPos(BlockPos pos) {
        this.x = pos.getX() >> 4;
        this.z = pos.getZ() >> 4;
    }

    public ChunkPos(long packed) {
        this.x = (int) packed;
        this.z = (int) (packed >> 32);
    }

    public long toLong() {
        return asLong(x, z);
    }

    public static long asLong(int x, int z) {
        return (long) x & 0xFFFFFFFFL | ((long) z & 0xFFFFFFFFL) << 32;
    }

    public static long asLong(BlockPos pos) {
        return asLong(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static int getX(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }

    public static int getZ(long packed) {
        return (int) (packed >>> 32 & 0xFFFFFFFFL);
    }

    /** Observed: each coordinate through a linear congruence (z flipped first), the two xored. */
    @Override
    public int hashCode() {
        return hash(x, z);
    }

    public static int hash(int x, int z) {
        int i = 1664525 * x + 1013904223;
        int j = 1664525 * (z ^ -559038737) + 1013904223;
        return i ^ j;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ChunkPos c && x == c.x && z == c.z;
    }

    public int getMiddleBlockX() {
        return getBlockX(8);
    }

    public int getMiddleBlockZ() {
        return getBlockZ(8);
    }

    public int getMinBlockX() {
        return x << 4;
    }

    public int getMinBlockZ() {
        return z << 4;
    }

    public int getMaxBlockX() {
        return getBlockX(15);
    }

    public int getMaxBlockZ() {
        return getBlockZ(15);
    }

    public int getRegionX() {
        return x >> 5;
    }

    public int getRegionZ() {
        return z >> 5;
    }

    public int getRegionLocalX() {
        return x & 31;
    }

    public int getRegionLocalZ() {
        return z & 31;
    }

    public int getBlockX(int offset) {
        return (x << 4) + offset;
    }

    public int getBlockZ(int offset) {
        return (z << 4) + offset;
    }

    /** Observed: "[x, z]". */
    @Override
    public String toString() {
        return "[" + x + ", " + z + "]";
    }

    public BlockPos getWorldPosition() {
        return new BlockPos(getMinBlockX(), 0, getMinBlockZ());
    }

    public int getChessboardDistance(ChunkPos other) {
        return getChessboardDistance(other.x, other.z);
    }

    public int getChessboardDistance(int x, int z) {
        return Math.max(Math.abs(this.x - x), Math.abs(this.z - z));
    }

    public int distanceSquared(ChunkPos other) {
        return distanceSquared(other.x, other.z);
    }

    public int distanceSquared(long packed) {
        return distanceSquared(getX(packed), getZ(packed));
    }

    private int distanceSquared(int x, int z) {
        int dx = x - this.x;
        int dz = z - this.z;
        return dx * dx + dz * dz;
    }
}
