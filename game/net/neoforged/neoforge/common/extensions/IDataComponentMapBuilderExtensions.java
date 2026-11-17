package net.neoforged.neoforge.common.extensions;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;

/** The stand-in for NeoForge's IDataComponentMapBuilderExtensions: setting by a supplier of the type. */
public interface IDataComponentMapBuilderExtensions {
    default <T> DataComponentMap.Builder set(Supplier<? extends DataComponentType<T>> type, T value) {
        return ((DataComponentMap.Builder) this).set(type.get(), value);
    }
}
