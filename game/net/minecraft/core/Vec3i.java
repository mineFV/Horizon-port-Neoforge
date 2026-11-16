package net.minecraft.core;

/**
 * The stand-in for the game's Vec3i: three int coordinates (Horizon's `horizon-core/src/position.rs`, BlockPos's
 * x, y, z). What the game answers beyond that is observed (`standins`, probe/), and each method says so.
 */
public class Vec3i implements Comparable<Vec3i> {
    /** Observed: one Vec3i of zeros, the same object each time. */
    public static final Vec3i ZERO = new Vec3i(0, 0, 0);

    private int x;
    private int y;
    private int z;

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

    /** For MutableBlockPos, which changes its coordinates in place. */
    protected Vec3i setX(int x) {
        this.x = x;
        return this;
    }

    protected Vec3i setY(int y) {
        this.y = y;
        return this;
    }

    protected Vec3i setZ(int z) {
        this.z = z;
        return this;
    }

    /**
     * Observed: equal to any Vec3i of the same coordinates, whatever its class (a Vec3i, a BlockPos, a
     * MutableBlockPos, a mod's subclass); Horizon's positions as keys are equal by coordinates (`game/explosion.rs`).
     */
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
     * The game's order of positions (Horizon's `game/explosion.rs`: y, then z, then x), and the number it gives as
     * observed: the first coordinate that differs, this one's minus the other's, as an int subtraction that wraps
     * ((MAX, 0, 0) against (MIN, 0, 0) gives -1).
     */
    @Override
    public int compareTo(Vec3i other) {
        if (y != other.y) return y - other.y;
        if (z != other.z) return z - other.z;
        return x - other.x;
    }

    /**
     * Observed: the squared distance, in doubles (a difference of extremes doesn't wrap): ((double) x - other's x)
     * squared, and so on, summed.
     */
    public double distSqr(Vec3i other) {
        double dx = (double) x - other.x;
        double dy = (double) y - other.y;
        double dz = (double) z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Observed: the class's simple name, then the coordinates ("BlockPos{x=1, y=2, z=3}", "{x=1, y=2, z=3}" for an
     * anonymous class); Horizon writes the same for a BlockPos (`game/lan_actions.rs`).
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{x=" + x + ", y=" + y + ", z=" + z + "}";
    }
}
