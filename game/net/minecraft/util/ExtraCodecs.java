package net.minecraft.util;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;

/** The stand-in for the game's ExtraCodecs: those Mekanism uses, built as the game builds them. */
public class ExtraCodecs {
    public static final Codec<Integer> NON_NEGATIVE_INT = Codec.intRange(0, Integer.MAX_VALUE);
    public static final Codec<Integer> POSITIVE_INT = Codec.intRange(1, Integer.MAX_VALUE);
    public static final Codec<String> NON_EMPTY_STRING = Codec.STRING.validate(s -> s.isEmpty()
            ? com.mojang.serialization.DataResult.error(() -> "Expected non-empty string") : com.mojang.serialization.DataResult.success(s));

    public static <T> Codec<List<T>> nonEmptyList(Codec<List<T>> codec) {
        return codec.validate(l -> l.isEmpty() ? com.mojang.serialization.DataResult.error(() -> "List must have contents") : com.mojang.serialization.DataResult.success(l));
    }

    /** Not modelled beyond what it is (an empty map for none): a codec of the game's, by name. */
    public static <A> Codec<Optional<A>> optionalEmptyMap(Codec<A> codec) {
        return com.mojang.serialization.HorizonCodecs.game("ExtraCodecs.optionalEmptyMap");
    }
}
