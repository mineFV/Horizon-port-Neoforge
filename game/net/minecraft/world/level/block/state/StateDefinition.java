package net.minecraft.world.level.block.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.world.level.block.state.properties.Property;

/** The stand-in for the game's StateDefinition: a block's properties (by name) and every state they make. */
public class StateDefinition<O, S extends StateHolder<O, S>> {
    private final O owner;
    private final Map<String, Property<?>> properties;
    private final List<S> states;

    StateDefinition(O owner, Map<String, Property<?>> properties, List<S> states) {
        this.owner = owner;
        this.properties = properties;
        this.states = states;
    }

    /** The first state: each property's first value. */
    public S any() {
        return states.get(0);
    }

    public O getOwner() {
        return owner;
    }

    public Collection<Property<?>> getProperties() {
        return properties.values();
    }

    public Property<?> getProperty(String name) {
        return properties.get(name);
    }

    /** Observed (`dump`'s exporter, texts.definition): its owner and its properties' names. */
    @Override
    public String toString() {
        return "StateDefinition{block=" + owner + ", properties=" + new ArrayList<>(properties.keySet()) + "}";
    }

    /** HorizonBridge's: the states, for finding one by its values. Not the game's API. */
    public List<S> horizonStates() {
        return states;
    }

    /** The stand-in for StateDefinition.Builder: the properties a block adds. */
    public static class Builder<O, S extends StateHolder<O, S>> {
        private final O owner;
        private final Map<String, Property<?>> properties = new TreeMap<>();

        public Builder(O owner) {
            this.owner = owner;
        }

        public Builder<O, S> add(Property<?>... added) {
            for (Property<?> p : added) properties.put(p.getName(), p);
            return this;
        }

        /**
         * Block's: every combination of the properties' values, in the game's order (the properties by name, the last
         * one changing fastest), each state given its neighbours. Not the game's API (its create takes fastutil's and
         * DataFixerUpper's types).
         */
        public StateDefinition<O, S> horizonCreate(java.util.function.BiFunction<O, Map<Property<?>, Comparable<?>>, S> factory) {
            List<Map<Property<?>, Comparable<?>>> combinations = new ArrayList<>();
            combinations.add(new java.util.LinkedHashMap<>());
            for (Property<?> p : properties.values()) {
                List<Map<Property<?>, Comparable<?>>> next = new ArrayList<>();
                for (Map<Property<?>, Comparable<?>> c : combinations) {
                    for (Comparable<?> v : p.getPossibleValues()) {
                        Map<Property<?>, Comparable<?>> m = new java.util.LinkedHashMap<>(c);
                        m.put(p, v);
                        next.add(m);
                    }
                }
                combinations = next;
            }
            List<S> states = new ArrayList<>();
            for (Map<Property<?>, Comparable<?>> c : combinations) states.add(factory.apply(owner, c));
            for (S s : states) s.horizonNeighbours(states);
            return new StateDefinition<>(owner, properties, states);
        }
    }
}
