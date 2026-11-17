package net.minecraft.core.component;

import java.util.Map;
import java.util.Set;

/**
 * Not the game's: the components of an item of the game's, as they were given (HorizonRegistries.known). The modelled
 * types' values are answered; when the game's item has types that aren't modelled (complete false), what lists them
 * (keySet) can't be answered and says so. Not given at all, nothing can be answered.
 */
public final class HorizonGivenComponents implements DataComponentMap {
    private final String item;
    private final Map<DataComponentType<?>, Object> values;
    private final boolean complete;

    public HorizonGivenComponents(String item, Map<DataComponentType<?>, Object> values, boolean complete) {
        this.item = item;
        this.values = values;
        this.complete = complete;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(DataComponentType<? extends T> type) {
        if (values == null) throw new IllegalStateException("the game's components of " + item + " weren't given (HorizonRegistries.known)");
        if (!complete && !values.containsKey(type)) throw new IllegalStateException("the game's components of " + item + " aren't all modelled: " + type + " can't be answered");
        return (T) values.get(type);
    }

    @Override
    public Set<DataComponentType<?>> keySet() {
        if (values == null || !complete) throw new IllegalStateException("the game's components of " + item + " aren't all modelled");
        return new HorizonKeys(values.keySet());
    }

    @Override
    public String toString() {
        if (values == null || !complete) throw new IllegalStateException("the game's components of " + item + " aren't all modelled");
        return HorizonKeys.text(this);
    }
}
