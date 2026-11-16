package net.minecraft.nbt;

/** The stand-in for the game's CollectionTag: a list of tags of one kind, and a tag itself. */
public abstract class CollectionTag<T extends Tag> extends java.util.AbstractList<T> implements Tag {
    public CollectionTag() {}

    @Override
    public abstract T set(int i, T t);

    @Override
    public abstract void add(int i, T t);

    @Override
    public abstract T remove(int i);

    /** Whether the tag is of the kind this one holds, then put at i in place of what was there. */
    public abstract boolean setTag(int i, Tag t);

    /** Whether the tag is of the kind this one holds, then added at i. */
    public abstract boolean addTag(int i, Tag t);

    public abstract byte getElementType();
}
