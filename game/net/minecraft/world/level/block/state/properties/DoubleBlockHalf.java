package net.minecraft.world.level.block.state.properties;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/** The stand-in for the game's DoubleBlockHalf: a two-block block's halves, their names and the other half (observed). */
public enum DoubleBlockHalf implements StringRepresentable {
    UPPER(Direction.DOWN),
    LOWER(Direction.UP);

    private final Direction directionToOther;

    DoubleBlockHalf(Direction directionToOther) {
        this.directionToOther = directionToOther;
    }

    public Direction getDirectionToOther() {
        return directionToOther;
    }

    @Override
    public String toString() {
        return getSerializedName();
    }

    @Override
    public String getSerializedName() {
        return this == UPPER ? "upper" : "lower";
    }

    public DoubleBlockHalf getOtherHalf() {
        return this == UPPER ? LOWER : UPPER;
    }
}
