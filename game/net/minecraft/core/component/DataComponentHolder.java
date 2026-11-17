package net.minecraft.core.component;

/** The stand-in for the game's DataComponentHolder: its components, and what they hold. */
public interface DataComponentHolder extends net.neoforged.neoforge.common.extensions.IDataComponentHolderExtension {
    DataComponentMap getComponents();

    default <T> T get(DataComponentType<? extends T> type) {
        return getComponents().get(type);
    }

    default <T> T getOrDefault(DataComponentType<? extends T> type, T fallback) {
        return getComponents().getOrDefault(type, fallback);
    }

    default boolean has(DataComponentType<?> type) {
        return getComponents().has(type);
    }
}
