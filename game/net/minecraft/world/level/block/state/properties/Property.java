package net.minecraft.world.level.block.state.properties;

import java.util.Collection;
import java.util.Optional;

/**
 * The stand-in for the game's Property: a block state's property, by its name and its values' class. Observed
 * (`standins`, probe/RegistryProbe.java): equal by class and name, its hash 31 times (31 times its class's hash plus its
 * name's) plus its values', its text MoreObjects' form.
 */
public abstract class Property<T extends Comparable<T>> {
    private final Class<T> clazz;
    private final String name;

    protected Property(String name, Class<T> clazz) {
        this.clazz = clazz;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class<T> getValueClass() {
        return clazz;
    }

    public abstract Collection<T> getPossibleValues();

    public abstract String getName(T value);

    public abstract Optional<T> getValue(String text);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{name=" + name + ", clazz=" + clazz + ", values=" + getPossibleValues() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof Property<?> other && clazz.equals(other.clazz) && name.equals(other.name);
    }

    @Override
    public final int hashCode() {
        return generateHashCode();
    }

    public int generateHashCode() {
        return 31 * clazz.hashCode() + name.hashCode();
    }
}
