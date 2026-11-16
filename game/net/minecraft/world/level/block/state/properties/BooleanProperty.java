package net.minecraft.world.level.block.state.properties;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** The stand-in for the game's BooleanProperty, observed (`standins`): true then false, named "true" and "false". */
public class BooleanProperty extends Property<Boolean> {
    private final List<Boolean> values = List.of(true, false);

    protected BooleanProperty(String name) {
        super(name, Boolean.class);
    }

    public static BooleanProperty create(String name) {
        return new BooleanProperty(name);
    }

    @Override
    public Collection<Boolean> getPossibleValues() {
        return values;
    }

    /** Observed: "true" and "false" exactly (not "TRUE"). */
    @Override
    public Optional<Boolean> getValue(String text) {
        return "true".equals(text) ? Optional.of(true) : "false".equals(text) ? Optional.of(false) : Optional.empty();
    }

    @Override
    public String getName(Boolean value) {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof BooleanProperty other && super.equals(other) && values.equals(other.values);
    }

    /** Observed: 31 times Property's, plus its values' (a set of true and false). */
    @Override
    public int generateHashCode() {
        return 31 * super.generateHashCode() + Boolean.hashCode(true) + Boolean.hashCode(false);
    }
}
