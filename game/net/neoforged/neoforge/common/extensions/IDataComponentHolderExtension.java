package net.neoforged.neoforge.common.extensions;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;

/** The stand-in for NeoForge's IDataComponentHolderExtension: a holder's questions by a supplier of the type. */
public interface IDataComponentHolderExtension {
    default <T> T get(Supplier<? extends DataComponentType<? extends T>> type) {
        return ((DataComponentHolder) this).get(type.get());
    }

    default <T> T getOrDefault(Supplier<? extends DataComponentType<? extends T>> type, T fallback) {
        return ((DataComponentHolder) this).getOrDefault(type.get(), fallback);
    }

    default boolean has(Supplier<? extends DataComponentType<?>> type) {
        return ((DataComponentHolder) this).has(type.get());
    }
}
