package net.minecraft.core;

import net.minecraft.util.StringRepresentable;

/**
 * The stand-in for the game's Direction, from Horizon's `horizon-core/src/geometry/mod.rs` (`Direction`: its order,
 * `opposite`, `normal`) and `horizon-assets/src/model.rs` (`Direction::name`, the names block states use).
 */
public enum Direction implements StringRepresentable {
    DOWN(0, -1, 0, "down"),
    UP(0, 1, 0, "up"),
    NORTH(0, 0, -1, "north"),
    SOUTH(0, 0, 1, "south"),
    WEST(-1, 0, 0, "west"),
    EAST(1, 0, 0, "east");

    private final int stepX;
    private final int stepY;
    private final int stepZ;
    private final String serializedName;

    Direction(int stepX, int stepY, int stepZ, String serializedName) {
        this.stepX = stepX;
        this.stepY = stepY;
        this.stepZ = stepZ;
        this.serializedName = serializedName;
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

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    /** Its name in lower case, as observed in the game (`standins`, probe/): the same as getSerializedName. */
    @Override
    public String toString() {
        return serializedName;
    }
}
