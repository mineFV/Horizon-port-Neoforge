package net.minecraft.core;

/**
 * The stand-in for the game's Vec3i: three int coordinates (Horizon's `horizon-core/src/position.rs`, BlockPos's
 * x, y, z).
 *
 * Abstract here, though the game's isn't: Horizon's code knows how a BlockPos compares and hashes, not whether a
 * Vec3i of its own class equals a BlockPos of the same coordinates. So a mod's `new Vec3i` isn't translated, and
 * every Vec3i there is is a BlockPos (the only subclass the stand-ins have, game/README.md).
 */
public abstract class Vec3i implements Comparable<Vec3i> {
    private final int x;
    private final int y;
    private final int z;

    public Vec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    /** The same coordinates: Horizon's positions as Java keys (`game/explosion.rs`, `Key`, equal by coordinates). */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vec3i other)) return false;
        return x == other.x && y == other.y && z == other.z;
    }

    /** `game/explosion.rs` and `horizon-world/src/level/redstone.rs`, Vec3i.hashCode: (y + z * 31) * 31 + x. */
    @Override
    public int hashCode() {
        return (y + z * 31) * 31 + x;
    }

    /**
     * Not modelled: Horizon's code gives the game's order of positions (y, then z, then x), not the number compareTo
     * returns, which a difference could overflow.
     */
    @Override
    public native int compareTo(Vec3i other);

    /** Not modelled here: a BlockPos's own is below. */
    @Override
    public native String toString();
}
