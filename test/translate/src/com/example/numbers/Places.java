package com.example.numbers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Made up: the game's positions and directions, through the translator's stand-ins (game/). */
public final class Places {
    private Places() {}

    public static long packed(int x, int y, int z) {
        return BlockPos.asLong(x, y, z);
    }

    public static int unpackedX(long p) {
        return BlockPos.getX(p);
    }

    public static int unpackedY(long p) {
        return BlockPos.getY(p);
    }

    public static int unpackedZ(long p) {
        return BlockPos.getZ(p);
    }

    public static int opposite(int d) {
        return Direction.values()[d].getOpposite().ordinal();
    }

    public static int steps(int d) {
        Direction dir = Direction.values()[d];
        return dir.getStepX() * 100 + dir.getStepY() * 10 + dir.getStepZ();
    }

    public static int nameLength(int d) {
        return Direction.values()[d].getSerializedName().length();
    }

    /** From a packed position, steps along the axes (d's bits say which), then up: packed again. */
    public static long walk(long p, int d) {
        BlockPos at = BlockPos.of(p);
        if ((d & 1) != 0) at = at.east();
        if ((d & 2) != 0) at = at.north();
        if ((d & 4) != 0) at = at.below().west().south();
        return at.above().asLong();
    }

    /** Positions as keys: a HashMap of them, in the JDK's order, and their texts. */
    public static String keys(int n) {
        java.util.Map<BlockPos, Integer> m = new java.util.HashMap<>();
        for (int i = 0; i < n; i++) m.merge(new BlockPos(i % 5, i * 7 - 20, -i), i, Integer::sum);
        m.merge(new BlockPos(0, -20, 0), 100, Integer::sum);
        return m.toString();
    }

    public static int eastX(int x) {
        return new BlockPos(x, 0, 0).east().getX();
    }
}
