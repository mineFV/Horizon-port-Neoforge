package net.minecraft.core;

/**
 * The stand-in for the game's BlockPos, from Horizon's `horizon-core/src/position.rs`: its packed form (`pack`,
 * `unpack`: 26 bits of x, 26 of z, 12 of y, truncated as in vanilla) and its offsets, which wrap as Java's int
 * additions do (`offset`, `wrapping_add`). Which object a method gives back (this one, a new one, of which class)
 * is observed (`standins`, probe/).
 */
public class BlockPos extends Vec3i {
    // The game's codecs: built as objects that don't run yet (HorizonCodecs, HorizonStreamCodecs).
    public static final com.mojang.serialization.Codec<BlockPos> CODEC = com.mojang.serialization.HorizonCodecs.game("BlockPos.CODEC");
    public static final net.minecraft.network.codec.StreamCodec<Object, BlockPos> STREAM_CODEC = new net.minecraft.network.codec.HorizonStreamCodecs.Opaque<>("BlockPos.STREAM_CODEC");

    /** Observed: a BlockPos of its own, not Vec3i.ZERO. */
    public static final BlockPos ZERO = new BlockPos(0, 0, 0);

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

    /**
     * Observed: the block a point is in, each coordinate rounded down as an int cast then one less when the cast
     * went up (NaN gives 0, beyond the ints the cast's bound, below MIN it wraps to MAX).
     */
    public static BlockPos containing(double x, double y, double z) {
        return new BlockPos(floor(x), floor(y), floor(z));
    }

    /** Observed: the block a position is in, as containing(x, y, z). */
    public static BlockPos containing(Position p) {
        return containing(p.x(), p.y(), p.z());
    }

    /** Observed: the middle of the block (Vec3.atCenterOf). */
    public net.minecraft.world.phys.Vec3 getCenter() {
        return net.minecraft.world.phys.Vec3.atCenterOf(this);
    }

    private static int floor(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }

    /** Observed: this one when nothing is added, else a new BlockPos. */
    public BlockPos offset(int dx, int dy, int dz) {
        return dx == 0 && dy == 0 && dz == 0 ? this : new BlockPos(getX() + dx, getY() + dy, getZ() + dz);
    }

    /** Observed: as offset(int, int, int), called on this object (a MutableBlockPos gives a new BlockPos). */
    public BlockPos offset(Vec3i v) {
        return offset(v.getX(), v.getY(), v.getZ());
    }

    /** Observed: offset by the negated coordinates, which wrap ((1, 2, 3) less (MIN, 0, 0) gives x = -2147483647). */
    public BlockPos subtract(Vec3i v) {
        return offset(-v.getX(), -v.getY(), -v.getZ());
    }

    /** One step towards a direction: its normal added (Horizon's `geometry::Direction::normal`); always a new one. */
    public BlockPos relative(Direction direction) {
        return new BlockPos(getX() + direction.getStepX(), getY() + direction.getStepY(), getZ() + direction.getStepZ());
    }

    /** Observed: this one for 0 steps, else the normal times n added, each with int wrapping. */
    public BlockPos relative(Direction direction, int n) {
        if (n == 0) return this;
        return new BlockPos(getX() + direction.getStepX() * n, getY() + direction.getStepY() * n, getZ() + direction.getStepZ() * n);
    }

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

    /** Observed: relative(direction, n), called on this object. */
    public BlockPos above(int n) {
        return relative(Direction.UP, n);
    }

    public BlockPos below(int n) {
        return relative(Direction.DOWN, n);
    }

    public BlockPos north(int n) {
        return relative(Direction.NORTH, n);
    }

    public BlockPos south(int n) {
        return relative(Direction.SOUTH, n);
    }

    public BlockPos west(int n) {
        return relative(Direction.WEST, n);
    }

    public BlockPos east(int n) {
        return relative(Direction.EAST, n);
    }

    /** Observed: this one (a mod's subclass too); a MutableBlockPos gives a copy. */
    public BlockPos immutable() {
        return this;
    }

    /**
     * The stand-in for the game's BlockPos.MutableBlockPos: a position changed in place. Observed: what it gives
     * that isn't itself is a new BlockPos (never this one, even for an offset of nothing), and its setters give it.
     */
    public static class MutableBlockPos extends BlockPos {
        public MutableBlockPos() {
            this(0, 0, 0);
        }

        public MutableBlockPos(int x, int y, int z) {
            super(x, y, z);
        }

        @Override
        public BlockPos offset(int dx, int dy, int dz) {
            return super.offset(dx, dy, dz).immutable();
        }

        @Override
        public BlockPos relative(Direction direction, int n) {
            return super.relative(direction, n).immutable();
        }

        @Override
        public BlockPos immutable() {
            return new BlockPos(getX(), getY(), getZ());
        }

        public MutableBlockPos set(int x, int y, int z) {
            setX(x);
            setY(y);
            setZ(z);
            return this;
        }

        public MutableBlockPos setWithOffset(Vec3i v, Direction direction) {
            return set(v.getX() + direction.getStepX(), v.getY() + direction.getStepY(), v.getZ() + direction.getStepZ());
        }

        public MutableBlockPos setWithOffset(Vec3i v, int dx, int dy, int dz) {
            return set(v.getX() + dx, v.getY() + dy, v.getZ() + dz);
        }

        public MutableBlockPos setWithOffset(Vec3i v, Vec3i w) {
            return set(v.getX() + w.getX(), v.getY() + w.getY(), v.getZ() + w.getZ());
        }

        public MutableBlockPos move(Direction direction) {
            return set(getX() + direction.getStepX(), getY() + direction.getStepY(), getZ() + direction.getStepZ());
        }

        @Override
        public MutableBlockPos setX(int x) {
            super.setX(x);
            return this;
        }

        @Override
        public MutableBlockPos setY(int y) {
            super.setY(y);
            return this;
        }

        @Override
        public MutableBlockPos setZ(int z) {
            super.setZ(z);
            return this;
        }
    }
}
