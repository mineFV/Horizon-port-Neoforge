package net.neoforged.neoforge.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The stand-in for NeoForge's ModConfigSpec (decision 3): a mod's config values, by path. Once loaded (the bridge loads
 * the configs a mod registered, HorizonModContainer), each value is its default, as a new world's config file has it;
 * the player's values in Horizon's settings aren't read yet. Observed (`standins --horizon-src`).
 */
public class ModConfigSpec implements net.neoforged.fml.config.IConfigSpec {
    private final List<ConfigValue<?>> values;
    private boolean loaded;

    private ModConfigSpec(List<ConfigValue<?>> values) {
        this.values = values;
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean isLoaded() {
        return loaded;
    }

    /** Writing the config file isn't modelled (the player's values aren't kept yet). */
    public void save() {}

    /** The bridge's: the config loaded, each value its default. Not the game's API. */
    public void horizonLoad() {
        loaded = true;
        for (ConfigValue<?> v : values) v.horizonReset();
    }

    public static class Builder {
        private final List<String> path = new ArrayList<>();
        private final List<ConfigValue<?>> values = new ArrayList<>();
        private final List<List<String>> defined = new ArrayList<>();

        public Builder() {}

        public Builder comment(String comment) {
            return this;
        }

        public Builder comment(String... comment) {
            return this;
        }

        public Builder translation(String key) {
            return this;
        }

        public Builder worldRestart() {
            return this;
        }

        public Builder gameRestart() {
            return this;
        }

        public Builder push(String name) {
            for (String part : name.split("\\.")) path.add(part);
            return this;
        }

        public Builder push(List<String> names) {
            path.addAll(names);
            return this;
        }

        /** Observed: popping more than was pushed says so. */
        public Builder pop() {
            return pop(1);
        }

        public Builder pop(int count) {
            if (count > path.size()) throw new IllegalArgumentException("Attempted to pop " + count + " elements when we only had: " + path);
            for (int i = 0; i < count; i++) path.remove(path.size() - 1);
            return this;
        }

        private List<String> at(String name) {
            List<String> full = new ArrayList<>(path);
            for (String part : name.split("\\.")) full.add(part);
            return List.copyOf(full);
        }

        /** Observed: a default its validator refuses isn't refused here (a config file holding it would be corrected). */
        private <T, V extends ConfigValue<T>> V add(V value) {
            values.add(value);
            return value;
        }

        public <T> ConfigValue<T> define(String path, T defaultValue) {
            return define(path, defaultValue, o -> o != null && defaultValue.getClass().isInstance(o));
        }

        public <T> ConfigValue<T> define(String path, T defaultValue, Predicate<Object> validator) {
            return add(new ConfigValue<>(this, at(path), () -> defaultValue, validator));
        }

        public BooleanValue define(String path, boolean defaultValue) {
            return add(new BooleanValue(this, at(path), () -> defaultValue));
        }

        public IntValue defineInRange(String path, int defaultValue, int min, int max) {
            return add(new IntValue(this, at(path), () -> defaultValue, o -> o instanceof Integer i && i >= min && i <= max));
        }

        public LongValue defineInRange(String path, long defaultValue, long min, long max) {
            return add(new LongValue(this, at(path), () -> defaultValue, o -> o instanceof Long l && l >= min && l <= max));
        }

        public DoubleValue defineInRange(String path, double defaultValue, double min, double max) {
            return add(new DoubleValue(this, at(path), () -> defaultValue, o -> o instanceof Double d && d >= min && d <= max));
        }

        @SuppressWarnings("unchecked")
        public <V extends Enum<V>> EnumValue<V> defineEnum(String path, V defaultValue) {
            return add(new EnumValue<>(this, at(path), () -> defaultValue, o -> o != null && defaultValue.getDeclaringClass().isInstance(o)));
        }

        public <T> ConfigValue<List<? extends T>> defineListAllowEmpty(String path, Supplier<List<? extends T>> defaultValue, Supplier<T> newElement, Predicate<Object> elementValidator) {
            return add(new ConfigValue<>(this, at(path), defaultValue, o -> {
                if (!(o instanceof List<?> list)) return false;
                for (Object e : list) if (!elementValidator.test(e)) return false;
                return true;
            }));
        }

        public ModConfigSpec build() {
            ModConfigSpec spec = new ModConfigSpec(List.copyOf(values));
            for (ConfigValue<?> v : values) v.spec = spec;
            return spec;
        }
    }

    public static class ConfigValue<T> implements Supplier<T> {
        private final Builder parent;
        final List<String> path;
        final Supplier<T> defaultValue;
        final Predicate<Object> validator;
        ModConfigSpec spec;
        private T value;
        private T cached;

        ConfigValue(Builder parent, List<String> path, Supplier<T> defaultValue, Predicate<Object> validator) {
            this.parent = parent;
            this.path = path;
            this.defaultValue = defaultValue;
            this.validator = validator;
        }

        void horizonReset() {
            value = defaultValue.get();
            cached = null;
        }

        public List<String> getPath() {
            return new ArrayList<>(path);
        }

        /** Observed: before its config is loaded, IllegalStateException; once read, the same value until the cache is cleared. */
        @Override
        public T get() {
            if (spec == null || !spec.isLoaded()) throw new IllegalStateException("Cannot get config value before config is loaded.");
            if (cached == null) cached = value;
            return cached;
        }

        public void clearCache() {
            cached = null;
        }

        public T getDefault() {
            return defaultValue.get();
        }

        public Builder next() {
            return parent;
        }

        public void save() {}

        /** Observed: what get gives doesn't change until the cache is cleared (a reload). */
        public void set(T value) {
            if (spec == null || !spec.isLoaded()) throw new IllegalStateException("Cannot set config value before config is loaded.");
            this.value = value;
        }
    }

    public static class BooleanValue extends ConfigValue<Boolean> implements BooleanSupplier {
        BooleanValue(Builder parent, List<String> path, Supplier<Boolean> defaultValue) {
            super(parent, path, defaultValue, o -> o instanceof Boolean);
        }

        @Override
        public boolean getAsBoolean() {
            return get();
        }

        public boolean isTrue() {
            return getAsBoolean();
        }

        public boolean isFalse() {
            return !getAsBoolean();
        }
    }

    public static class IntValue extends ConfigValue<Integer> implements IntSupplier {
        IntValue(Builder parent, List<String> path, Supplier<Integer> defaultValue, Predicate<Object> validator) {
            super(parent, path, defaultValue, validator);
        }

        @Override
        public int getAsInt() {
            return get();
        }
    }

    public static class LongValue extends ConfigValue<Long> implements LongSupplier {
        LongValue(Builder parent, List<String> path, Supplier<Long> defaultValue, Predicate<Object> validator) {
            super(parent, path, defaultValue, validator);
        }

        @Override
        public long getAsLong() {
            return get();
        }
    }

    public static class DoubleValue extends ConfigValue<Double> implements DoubleSupplier {
        DoubleValue(Builder parent, List<String> path, Supplier<Double> defaultValue, Predicate<Object> validator) {
            super(parent, path, defaultValue, validator);
        }

        @Override
        public double getAsDouble() {
            return get();
        }
    }

    public static class EnumValue<T extends Enum<T>> extends ConfigValue<T> {
        EnumValue(Builder parent, List<String> path, Supplier<T> defaultValue, Predicate<Object> validator) {
            super(parent, path, defaultValue, validator);
        }
    }
}
