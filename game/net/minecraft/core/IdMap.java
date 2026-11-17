package net.minecraft.core;

/** The stand-in for the game's IdMap: values by number. */
public interface IdMap<T> extends Iterable<T> {
    int getId(T value);

    T byId(int id);

    int size();
}
