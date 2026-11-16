package net.minecraft.world.level.block.state;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * The stand-in for the game's StateHolder: a state, its owner (its block) and its properties' values; setValue gives
 * the state of the same owner with that value (one object for each combination, as the game's).
 */
public abstract class StateHolder<O, S> {
    protected final O owner;
    private final Map<Property<?>, Comparable<?>> values;
    private List<S> neighbours = List.of();

    protected StateHolder(O owner, Map<Property<?>, Comparable<?>> values) {
        this.owner = owner;
        this.values = values;
    }

    /** StateDefinition's: the other states of the owner, among which setValue finds one. */
    void horizonNeighbours(List<S> states) {
        this.neighbours = states;
    }

    public Map<Property<?>, Comparable<?>> getValues() {
        return values;
    }

    public Collection<Property<?>> getProperties() {
        return values.keySet();
    }

    public <T extends Comparable<T>> boolean hasProperty(Property<T> property) {
        return values.containsKey(property);
    }

    public <T extends Comparable<T>> T getValue(Property<T> property) {
        Comparable<?> v = values.get(property);
        if (v == null) throw new IllegalArgumentException("Cannot get property " + property + " as it does not exist in " + owner);
        return property.getValueClass().cast(v);
    }

    public <T extends Comparable<T>> Optional<T> getOptionalValue(Property<T> property) {
        Comparable<?> v = values.get(property);
        return v == null ? Optional.empty() : Optional.of(property.getValueClass().cast(v));
    }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>, V extends T> S setValue(Property<T> property, V value) {
        Comparable<?> now = values.get(property);
        if (now == null) throw new IllegalArgumentException("Cannot set property " + property + " as it does not exist in " + owner);
        if (now.equals(value)) return (S) this;
        for (S s : neighbours) {
            Map<Property<?>, Comparable<?>> other = ((StateHolder<?, ?>) s).values;
            boolean same = true;
            for (Map.Entry<Property<?>, Comparable<?>> e : values.entrySet()) {
                Object want = e.getKey().equals(property) ? value : e.getValue();
                if (!want.equals(other.get(e.getKey()))) {
                    same = false;
                    break;
                }
            }
            if (same) return s;
        }
        throw new IllegalArgumentException("Cannot set property " + property + " to " + value + " on " + owner + ", it is not an allowed value");
    }

    public <T extends Comparable<T>, V extends T> S trySetValue(Property<T> property, V value) {
        return values.containsKey(property) ? setValue(property, value) : (S) cast();
    }

    @SuppressWarnings("unchecked")
    private S cast() {
        return (S) this;
    }

    /** The owner's text, then its properties' values in brackets (the game's text, as `dump`'s exporter observed it). */
    @Override
    public String toString() {
        if (values.isEmpty()) return String.valueOf(owner);
        StringBuilder b = new StringBuilder(String.valueOf(owner)).append('[');
        boolean first = true;
        for (Map.Entry<Property<?>, Comparable<?>> e : values.entrySet()) {
            if (!first) b.append(',');
            first = false;
            b.append(e.getKey().getName()).append('=').append(name(e.getKey(), e.getValue()));
        }
        return b.append(']').toString();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String name(Property p, Comparable v) {
        return p.getName(v);
    }
}
