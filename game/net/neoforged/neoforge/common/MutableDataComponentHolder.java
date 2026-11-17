package net.neoforged.neoforge.common;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;

/** The stand-in for NeoForge's MutableDataComponentHolder: setting and taking out a holder's components. */
public interface MutableDataComponentHolder extends net.minecraft.core.component.DataComponentHolder {
    <T> T set(DataComponentType<? super T> type, T value);

    default <T> T set(Supplier<? extends DataComponentType<? super T>> type, T value) {
        return set(type.get(), value);
    }

    <T> T remove(DataComponentType<? extends T> type);

    default <T> T remove(Supplier<? extends DataComponentType<? extends T>> type) {
        return remove(type.get());
    }

    default <T> T update(DataComponentType<T> type, T fallback, java.util.function.UnaryOperator<T> change) {
        return set(type, change.apply(getOrDefault(type, fallback)));
    }

    default <T> T update(Supplier<? extends DataComponentType<T>> type, T fallback, java.util.function.UnaryOperator<T> change) {
        return update(type.get(), fallback, change);
    }

    default <T, U> T update(DataComponentType<T> type, T fallback, U with, java.util.function.BiFunction<T, U, T> change) {
        return set(type, change.apply(getOrDefault(type, fallback), with));
    }

    default <T, U> T update(Supplier<? extends DataComponentType<T>> type, T fallback, U with, java.util.function.BiFunction<T, U, T> change) {
        return update(type.get(), fallback, with, change);
    }
}
