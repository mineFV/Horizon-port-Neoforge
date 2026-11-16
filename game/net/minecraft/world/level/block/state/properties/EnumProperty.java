package net.minecraft.world.level.block.state.properties;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.util.StringRepresentable;

/**
 * The stand-in for the game's EnumProperty, observed (`standins`): its values in the order given (an enum's own order
 * when all or a filter of them), named by their serialized names, equal to another with the same values in any order.
 */
public class EnumProperty<T extends Enum<T> & StringRepresentable> extends Property<T> {
    private final Set<T> values;
    private final Map<String, T> names = new HashMap<>();

    protected EnumProperty(String name, Class<T> clazz, Collection<T> values) {
        super(name, clazz);
        this.values = new LinkedHashSet<>(values);
        for (T v : values) names.put(v.getSerializedName(), v);
    }

    @Override
    public Collection<T> getPossibleValues() {
        return values;
    }

    @Override
    public Optional<T> getValue(String text) {
        return Optional.ofNullable(names.get(text));
    }

    @Override
    public String getName(T value) {
        return value.getSerializedName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof EnumProperty<?> other && super.equals(other) && values.equals(other.values);
    }

    @Override
    public int generateHashCode() {
        return 31 * super.generateHashCode() + values.hashCode();
    }

    public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String name, Class<T> clazz) {
        return create(name, clazz, v -> true);
    }

    public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String name, Class<T> clazz, Predicate<T> filter) {
        List<T> kept = new java.util.ArrayList<>();
        for (T v : clazz.getEnumConstants()) if (filter.test(v)) kept.add(v);
        return new EnumProperty<>(name, clazz, kept);
    }

    @SafeVarargs
    public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String name, Class<T> clazz, T... values) {
        return new EnumProperty<>(name, clazz, List.of(values));
    }

    public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String name, Class<T> clazz, Collection<T> values) {
        return new EnumProperty<>(name, clazz, values);
    }
}
