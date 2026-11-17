package net.minecraft.core.component;

/** The stand-in for the game's TypedDataComponent: a type and its value (its text observed). */
public record TypedDataComponent<T>(DataComponentType<T> type, T value) {
    @Override
    public String toString() {
        return type + "=>" + value;
    }
}
