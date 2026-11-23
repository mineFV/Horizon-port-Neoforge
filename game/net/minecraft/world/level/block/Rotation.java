package net.minecraft.world.level.block;

import net.minecraft.core.Direction;

/**
 * The stand-in for the game's Rotation: a quarter turn about the vertical, its names, two turns composed, a
 * direction turned (vertical ones kept) and an index among count turned, observed (`standins`,
 * probe/ShapeProbe.java). Not here: its octahedral group, the random ones, its codec.
 */
public enum Rotation implements net.minecraft.util.StringRepresentable {
    NONE("none"),
    CLOCKWISE_90("clockwise_90"),
    CLOCKWISE_180("180"),
    COUNTERCLOCKWISE_90("counterclockwise_90");

    private final String id;

    Rotation(String id) {
        this.id = id;
    }

    /** Observed: the two turns added, as quarter turns. */
    public Rotation getRotated(Rotation rotation) {
        return values()[(ordinal() + rotation.ordinal()) % 4];
    }

    /** Observed: a vertical direction kept, a horizontal one turned. */
    public Direction rotate(Direction direction) {
        if (direction.getAxis() == Direction.Axis.Y) return direction;
        if (this == CLOCKWISE_90) return direction.getClockWise();
        if (this == CLOCKWISE_180) return direction.getOpposite();
        if (this == COUNTERCLOCKWISE_90) return direction.getCounterClockWise();
        return direction;
    }

    /** Observed: an index among count turned by this many quarters of count. */
    public int rotate(int index, int count) {
        if (this == CLOCKWISE_90) return (index + count / 4) % count;
        if (this == CLOCKWISE_180) return (index + count / 2) % count;
        if (this == COUNTERCLOCKWISE_90) return (index + count * 3 / 4) % count;
        return index;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
