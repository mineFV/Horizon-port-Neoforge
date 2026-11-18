package net.minecraft.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * The stand-in for the game's StringRepresentable: a value with the name the game's data files give it, and the
 * lookups and codecs of such values by name (observed, `standins`). The codecs don't encode nor decode yet.
 */
public interface StringRepresentable {
    int PRE_BUILT_MAP_THRESHOLD = 16;

    String getSerializedName();

    static <E extends Enum<E> & StringRepresentable> EnumCodec<E> fromEnum(Supplier<E[]> values) {
        return fromEnumWithMapping(values, s -> s);
    }

    static <E extends Enum<E> & StringRepresentable> EnumCodec<E> fromEnumWithMapping(Supplier<E[]> values, Function<String, String> mapping) {
        E[] all = values.get();
        return new EnumCodec<>(all, createNameLookup(all, mapping));
    }

    static <T extends StringRepresentable> com.mojang.serialization.Codec<T> fromValues(Supplier<T[]> values) {
        T[] all = values.get();
        return new StringRepresentableCodec<>(all, createNameLookup(all, s -> s), t -> -1);
    }

    /**
     * Observed: the value whose name, mapped, is the one asked; null when none. Above PRE_BUILT_MAP_THRESHOLD values,
     * by a map made once (two values of one name are refused), else by going through them in order (the first wins).
     */
    static <T extends StringRepresentable> Function<String, T> createNameLookup(T[] values, Function<String, String> mapping) {
        if (values.length > PRE_BUILT_MAP_THRESHOLD) {
            Map<String, T> byName = new HashMap<>();
            for (T v : values) {
                String key = mapping.apply(v.getSerializedName());
                T old = byName.put(key, v);
                // Observed: the game's map refuses a name twice (Collectors.toMap's words).
                if (old != null) throw new IllegalStateException("Duplicate key " + key + " (attempted merging values " + old + " and " + v + ")");
            }
            return name -> name == null ? null : byName.get(name);
        }
        return name -> {
            for (T v : values) if (mapping.apply(v.getSerializedName()).equals(name)) return v;
            return null;
        };
    }

    /** The stand-in for StringRepresentable.StringRepresentableCodec: its values and lookup (encoding isn't modelled yet). */
    class StringRepresentableCodec<S extends StringRepresentable> implements com.mojang.serialization.Codec<S> {
        final S[] values;
        final Function<String, S> lookup;

        public StringRepresentableCodec(S[] values, Function<String, S> lookup, ToIntFunction<S> index) {
            this.values = values;
            this.lookup = lookup;
        }
    }

    /** The stand-in for StringRepresentable.EnumCodec: an enum's values by name. */
    class EnumCodec<E extends Enum<E> & StringRepresentable> extends StringRepresentableCodec<E> {
        public EnumCodec(E[] values, Function<String, E> lookup) {
            super(values, lookup, Enum::ordinal);
        }

        public E byName(String name) {
            return lookup.apply(name);
        }

        /** Observed: the fallback when the name is none of theirs. */
        public E byName(String name, E fallback) {
            E found = lookup.apply(name);
            return found != null ? found : fallback;
        }
    }
}
