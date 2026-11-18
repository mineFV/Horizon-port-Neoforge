package net.minecraft.core;

import net.minecraft.util.StringRepresentable;

/**
 * The stand-in for the game's Direction, from Horizon's `horizon-core/src/geometry/mod.rs` (`Direction`: its order,
 * `opposite`, `normal`, `approximate_nearest`) and `horizon-assets/src/model.rs` (`Direction::name`, the names block
 * states use); the rest observed (`standins`, probe/), as each member says.
 */
public enum Direction implements StringRepresentable {
    DOWN(0, -1, 0, "down", Axis.Y, AxisDirection.NEGATIVE, -1),
    UP(0, 1, 0, "up", Axis.Y, AxisDirection.POSITIVE, -1),
    NORTH(0, 0, -1, "north", Axis.Z, AxisDirection.NEGATIVE, 2),
    SOUTH(0, 0, 1, "south", Axis.Z, AxisDirection.POSITIVE, 0),
    WEST(-1, 0, 0, "west", Axis.X, AxisDirection.NEGATIVE, 1),
    EAST(1, 0, 0, "east", Axis.X, AxisDirection.POSITIVE, 3);

    // The game's codecs: built as objects that don't run yet (HorizonCodecs, HorizonStreamCodecs).
    public static final net.minecraft.util.StringRepresentable.EnumCodec<Direction> CODEC = net.minecraft.util.StringRepresentable.fromEnum(Direction::values);
    public static final net.minecraft.network.codec.StreamCodec<Object, Direction> STREAM_CODEC = new net.minecraft.network.codec.HorizonStreamCodecs.Opaque<>("Direction.STREAM_CODEC");


    private static final Direction[] BY_3D = values();

    private final int stepX;
    private final int stepY;
    private final int stepZ;
    private final String serializedName;
    private final Axis axis;
    private final AxisDirection axisDirection;
    private final int data2d;
    /** Observed: one Vec3i for each direction, the same object each time. */
    private final Vec3i normal;

    Direction(int stepX, int stepY, int stepZ, String serializedName, Axis axis, AxisDirection axisDirection, int data2d) {
        this.stepX = stepX;
        this.stepY = stepY;
        this.stepZ = stepZ;
        this.serializedName = serializedName;
        this.axis = axis;
        this.axisDirection = axisDirection;
        this.data2d = data2d;
        this.normal = new Vec3i(stepX, stepY, stepZ);
    }

    /** geometry/mod.rs, `opposite`. */
    public Direction getOpposite() {
        return switch (this) {
            case DOWN -> UP;
            case UP -> DOWN;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
        };
    }

    public int getStepX() {
        return stepX;
    }

    public int getStepY() {
        return stepY;
    }

    public int getStepZ() {
        return stepZ;
    }

    public Vec3i getNormal() {
        return normal;
    }

    /** Observed: X for west and east, Y for down and up, Z for north and south. */
    public Axis getAxis() {
        return axis;
    }

    /** Observed: POSITIVE for up, south and east. */
    public AxisDirection getAxisDirection() {
        return axisDirection;
    }

    /** Observed: south 0, west 1, north 2, east 3; down and up -1. */
    public int get2DDataValue() {
        return data2d;
    }

    /** Observed: the 2D value's two low bits times 90 degrees (down and up: 270). */
    public float toYRot() {
        return (float) ((data2d & 3) * 90);
    }

    /** Observed: a quarter turn clockwise seen from above; down and up throw. */
    public Direction getClockWise() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
            default -> throw new IllegalStateException("Unable to get Y-rotated facing of " + this);
        };
    }

    /** Observed: a quarter turn counterclockwise seen from above; down and up throw. */
    public Direction getCounterClockWise() {
        return switch (this) {
            case NORTH -> WEST;
            case WEST -> SOUTH;
            case SOUTH -> EAST;
            case EAST -> NORTH;
            default -> throw new IllegalStateException("Unable to get CCW facing of " + this);
        };
    }

    /** Observed: the direction of index |i % 6| (-1 gives up, 6 down). */
    public static Direction from3DDataValue(int i) {
        return BY_3D[Math.abs(i % 6)];
    }

    /**
     * geometry/mod.rs, `approximate_nearest`: the direction whose normal has the largest dot product with (x, y, z)
     * in floats, more than the smallest float above zero, in the order of values(); north when none is.
     */
    public static Direction getNearest(float x, float y, float z) {
        Direction result = NORTH;
        float best = 1.4E-45f; // the smallest float above zero (f32::from_bits(1))
        for (Direction d : BY_3D) {
            float dot = x * d.stepX + y * d.stepY + z * d.stepZ;
            if (dot > best) {
                best = dot;
                result = d;
            }
        }
        return result;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    /** Observed: its name in lower case, the same as getSerializedName. */
    @Override
    public String toString() {
        return serializedName;
    }

    /**
     * The stand-in for Direction.Axis. Observed: each constant has a class of its own (Direction$Axis$1 for X...),
     * as here, and its text is its lower-case name.
     */
    public enum Axis implements StringRepresentable, java.util.function.Predicate<Direction> {
        X("x") {
            @Override
            public int choose(int x, int y, int z) {
                return x;
            }

            @Override
            public double choose(double x, double y, double z) {
                return x;
            }
        },
        Y("y") {
            @Override
            public int choose(int x, int y, int z) {
                return y;
            }

            @Override
            public double choose(double x, double y, double z) {
                return y;
            }
        },
        Z("z") {
            @Override
            public int choose(int x, int y, int z) {
                return z;
            }

            @Override
            public double choose(double x, double y, double z) {
                return z;
            }
        };

        private final String name;

        Axis(String name) {
            this.name = name;
        }

        public boolean isVertical() {
            return this == Y;
        }

        public boolean isHorizontal() {
            return this == X || this == Z;
        }

        /** Observed: whether a direction is along this axis; null isn't. */
        @Override
        public boolean test(Direction d) {
            return d != null && d.getAxis() == this;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        /** Observed: the coordinate of this axis. */
        public abstract int choose(int x, int y, int z);

        public abstract double choose(double x, double y, double z);
    }

    /** The stand-in for Direction.AxisDirection. Observed: its step, its opposite and its text. */
    public enum AxisDirection {
        POSITIVE(1, "Towards positive"),
        NEGATIVE(-1, "Towards negative");

        private final int step;
        private final String name;

        AxisDirection(int step, String name) {
            this.step = step;
            this.name = name;
        }

        public int getStep() {
            return step;
        }

        public AxisDirection opposite() {
            return this == POSITIVE ? NEGATIVE : POSITIVE;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
