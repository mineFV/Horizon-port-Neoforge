package net.minecraft.core;

/**
 * The stand-in for the game's BlockPos, from Horizon's `horizon-core/src/position.rs`: its packed form (`pack`,
 * `unpack`: 26 bits of x, 26 of z, 12 of y, truncated as in vanilla) and its offsets, which wrap as Java's int
 * additions do (`offset`, `wrapping_add`). Each method gives a new position (game/README.md).
 */
public class BlockPos extends Vec3i {
    public BlockPos(int x, int y, int z) {
        super(x, y, z);
    }

    /** position.rs, `pack`. */
    public static long asLong(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | ((long) y & 0xFFFL);
    }

    public long asLong() {
        return asLong(getX(), getY(), getZ());
    }

    /** position.rs, `unpack`: x is the top 26 bits, sign kept. */
    public static int getX(long packed) {
        return (int) (packed >> 38);
    }

    /** position.rs, `unpack`: y is the low 12 bits, sign kept. */
    public static int getY(long packed) {
        return (int) (packed << 52 >> 52);
    }

    /** position.rs, `unpack`: z is the 26 bits above y, sign kept. */
    public static int getZ(long packed) {
        return (int) (packed << 26 >> 38);
    }

    public static BlockPos of(long packed) {
        return new BlockPos(getX(packed), getY(packed), getZ(packed));
    }

    /** One step towards a direction: its normal added (Horizon's `geometry::Direction::normal`). */
    public BlockPos relative(Direction direction) {
        return new BlockPos(getX() + direction.getStepX(), getY() + direction.getStepY(), getZ() + direction.getStepZ());
    }

    // One step along an axis, without Direction (whose toString isn't modelled, which keeps its objects out):
    // the normals of geometry/mod.rs.

    public BlockPos above() {
        return new BlockPos(getX(), getY() + 1, getZ());
    }

    public BlockPos below() {
        return new BlockPos(getX(), getY() - 1, getZ());
    }

    public BlockPos north() {
        return new BlockPos(getX(), getY(), getZ() - 1);
    }

    public BlockPos south() {
        return new BlockPos(getX(), getY(), getZ() + 1);
    }

    public BlockPos west() {
        return new BlockPos(getX() - 1, getY(), getZ());
    }

    public BlockPos east() {
        return new BlockPos(getX() + 1, getY(), getZ());
    }

    /**
     * The game's text for a BlockPos, as Horizon writes it in the server's warning of a use too far away
     * (`game/lan_actions.rs`, "BlockPos{x=..., y=..., z=...}"). Declared here rather than in Vec3i, whose text
     * for another class isn't known.
     */
    @Override
    public String toString() {
        return "BlockPos{x=" + getX() + ", y=" + getY() + ", z=" + getZ() + "}";
    }
}
