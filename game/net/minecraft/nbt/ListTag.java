package net.minecraft.nbt;

import java.util.ArrayList;
import java.util.List;

/**
 * The stand-in for the game's ListTag, observed (`standins`): its elements' kind is the first one's (0 while it is
 * empty, again once emptied), another kind isn't taken, and its getters give a default for a wrong kind or index.
 */
public class ListTag extends CollectionTag<Tag> {
    private final List<Tag> list;
    private byte type;

    public ListTag() {
        this(new ArrayList<>(), (byte) 0);
    }

    /** Observed: an empty list of no kind yet, as ListTag(). */
    public ListTag(int capacity) {
        this(new ArrayList<>(capacity), (byte) 0);
    }

    private ListTag(List<Tag> list, byte type) {
        this.list = list;
        this.type = type;
    }

    @Override
    public byte getId() {
        return 9;
    }

    /** Observed: the elements' texts between brackets, joined by commas. */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) b.append(',');
            b.append(list.get(i));
        }
        return b.append(']').toString();
    }

    private void emptied() {
        if (list.isEmpty()) type = 0;
    }

    @Override
    public Tag remove(int i) {
        Tag t = list.remove(i);
        emptied();
        return t;
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    private Tag at(int i, int kind) {
        if (i < 0 || i >= list.size()) return null;
        Tag t = list.get(i);
        return t.getId() == kind ? t : null;
    }

    public CompoundTag getCompound(int i) {
        Tag t = at(i, 10);
        return t != null ? (CompoundTag) t : new CompoundTag();
    }

    public ListTag getList(int i) {
        Tag t = at(i, 9);
        return t != null ? (ListTag) t : new ListTag();
    }

    public short getShort(int i) {
        Tag t = at(i, 2);
        return t != null ? ((ShortTag) t).getAsShort() : 0;
    }

    public int getInt(int i) {
        Tag t = at(i, 3);
        return t != null ? ((IntTag) t).getAsInt() : 0;
    }

    public int[] getIntArray(int i) {
        Tag t = at(i, 11);
        return t != null ? ((IntArrayTag) t).getAsIntArray() : new int[0];
    }

    public long[] getLongArray(int i) {
        Tag t = at(i, 12);
        return t != null ? ((LongArrayTag) t).getAsLongArray() : new long[0];
    }

    public double getDouble(int i) {
        Tag t = at(i, 6);
        return t != null ? ((DoubleTag) t).getAsDouble() : 0.0;
    }

    public float getFloat(int i) {
        Tag t = at(i, 5);
        return t != null ? ((FloatTag) t).getAsFloat() : 0.0F;
    }

    /** Observed: a StringTag's characters, another tag's text, "" past the end. */
    public String getString(int i) {
        if (i < 0 || i >= list.size()) return "";
        Tag t = list.get(i);
        return t.getId() == 8 ? t.getAsString() : t.toString();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public Tag get(int i) {
        return list.get(i);
    }

    /** Observed: what was there; another kind throws (the message names both kinds' ids). */
    @Override
    public Tag set(int i, Tag t) {
        Tag old = get(i);
        if (!setTag(i, t)) throw new UnsupportedOperationException("Trying to add tag of type " + t.getId() + " to list of " + type);
        return old;
    }

    @Override
    public void add(int i, Tag t) {
        if (!addTag(i, t)) throw new UnsupportedOperationException("Trying to add tag of type " + t.getId() + " to list of " + type);
    }

    @Override
    public boolean setTag(int i, Tag t) {
        if (!takes(t)) return false;
        list.set(i, t);
        return true;
    }

    @Override
    public boolean addTag(int i, Tag t) {
        if (!takes(t)) return false;
        list.add(i, t);
        return true;
    }

    private boolean takes(Tag t) {
        if (t.getId() == 0) return false;
        if (type == 0) {
            type = t.getId();
            return true;
        }
        return type == t.getId();
    }

    /** Observed: each element copied (a compound or an array is a new one). */
    @Override
    public ListTag copy() {
        List<Tag> copied = new ArrayList<>(list.size());
        for (Tag t : list) copied.add(t.copy());
        return new ListTag(copied, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ListTag other && java.util.Objects.equals(list, other.list);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    @Override
    public byte getElementType() {
        return type;
    }

    @Override
    public void clear() {
        list.clear();
        type = 0;
    }
}
