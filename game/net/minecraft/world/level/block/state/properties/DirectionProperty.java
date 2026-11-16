package net.minecraft.world.level.block.state.properties;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Direction;

/** The stand-in for the game's DirectionProperty: an EnumProperty of directions, observed (`standins`). */
public class DirectionProperty extends EnumProperty<Direction> {
    protected DirectionProperty(String name, Collection<Direction> values) {
        super(name, Direction.class, values);
    }

    /** Observed: every direction, in Direction's order. */
    public static DirectionProperty create(String name) {
        return create(name, d -> true);
    }

    public static DirectionProperty create(String name, Predicate<Direction> filter) {
        List<Direction> kept = new java.util.ArrayList<>();
        for (Direction d : Direction.values()) if (filter.test(d)) kept.add(d);
        return new DirectionProperty(name, kept);
    }

    public static DirectionProperty create(String name, Direction... values) {
        return new DirectionProperty(name, List.of(values));
    }

    public static DirectionProperty create(String name, Collection<Direction> values) {
        return new DirectionProperty(name, values);
    }
}
