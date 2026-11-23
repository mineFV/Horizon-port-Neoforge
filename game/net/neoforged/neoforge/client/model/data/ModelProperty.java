package net.neoforged.neoforge.client.model.data;

import java.util.function.Predicate;

/**
 * The stand-in for NeoForge's ModelProperty, the key of a block entity's model data: a predicate of the values it
 * may hold, true of every one (null too) when made without one. Observed (the probe's ListProbe).
 */
public class ModelProperty<T> implements Predicate<T> {
    private final Predicate<T> predicate;

    public ModelProperty() {
        this(value -> true);
    }

    public ModelProperty(Predicate<T> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(T value) {
        return predicate.test(value);
    }
}
