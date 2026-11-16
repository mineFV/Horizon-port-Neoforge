package net.minecraft.nbt;

/**
 * The stand-in for the game's LongArrayTag, observed (`standins`): it holds the array it was given (not a copy), its
 * changes make a new array, and an index past the end throws as the game's does.
 */
public class LongArrayTag extends CollectionTag<LongTag> {
    private long[] data;

    public LongArrayTag(long[] data) {
        this.data = data;
    }

    @Override
    public byte getId() {
        return 12;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("[L;");
        for (int i = 0; i < data.length; i++) {
            if (i > 0) b.append(',');
            b.append(data[i]).append("L");
        }
        return b.append(']').toString();
    }

    /** Observed: a copy of the array. */
    @Override
    public LongArrayTag copy() {
        long[] copied = new long[data.length];
        System.arraycopy(data, 0, copied, 0, data.length);
        return new LongArrayTag(copied);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof LongArrayTag other && java.util.Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(data);
    }

    public long[] getAsLongArray() {
        return data;
    }

    @Override
    public int size() {
        return data.length;
    }

    @Override
    public LongTag get(int i) {
        return LongTag.valueOf(data[i]);
    }

    @Override
    public LongTag set(int i, LongTag t) {
        long old = data[i];
        data[i] = t.getAsLong();
        return LongTag.valueOf(old);
    }

    @Override
    public void add(int i, LongTag t) {
        insert(i, t.getAsLong());
    }

    /** Observed: past the end, an IndexOutOfBoundsException saying "Index: i, Length: n". */
    private void insert(int i, long v) {
        if (i < 0 || i > data.length) throw new IndexOutOfBoundsException("Index: " + i + ", Length: " + data.length);
        long[] grown = new long[data.length + 1];
        System.arraycopy(data, 0, grown, 0, i);
        grown[i] = v;
        System.arraycopy(data, i, grown, i + 1, data.length - i);
        data = grown;
    }

    /** Observed: a number of any kind, as this kind; anything else isn't taken. */
    @Override
    public boolean setTag(int i, Tag t) {
        if (!(t instanceof NumericTag n)) return false;
        data[i] = n.getAsLong();
        return true;
    }

    @Override
    public boolean addTag(int i, Tag t) {
        if (!(t instanceof NumericTag n)) return false;
        insert(i, n.getAsLong());
        return true;
    }

    /** Observed: an index past the end throws as reading the array does. */
    @Override
    public LongTag remove(int i) {
        long old = data[i];
        long[] shrunk = new long[data.length - 1];
        System.arraycopy(data, 0, shrunk, 0, i);
        System.arraycopy(data, i + 1, shrunk, i, data.length - i - 1);
        data = shrunk;
        return LongTag.valueOf(old);
    }

    @Override
    public byte getElementType() {
        return 4;
    }

    @Override
    public void clear() {
        data = new long[0];
    }
}
