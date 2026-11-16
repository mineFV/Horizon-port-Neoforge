package net.minecraft.nbt;

/**
 * The stand-in for the game's ByteArrayTag, observed (`standins`): it holds the array it was given (not a copy), its
 * changes make a new array, and an index past the end throws as the game's does.
 */
public class ByteArrayTag extends CollectionTag<ByteTag> {
    private byte[] data;

    public ByteArrayTag(byte[] data) {
        this.data = data;
    }

    @Override
    public byte getId() {
        return 7;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("[B;");
        for (int i = 0; i < data.length; i++) {
            if (i > 0) b.append(',');
            b.append(data[i]).append("B");
        }
        return b.append(']').toString();
    }

    /** Observed: a copy of the array. */
    @Override
    public ByteArrayTag copy() {
        byte[] copied = new byte[data.length];
        System.arraycopy(data, 0, copied, 0, data.length);
        return new ByteArrayTag(copied);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ByteArrayTag other && java.util.Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(data);
    }

    public byte[] getAsByteArray() {
        return data;
    }

    @Override
    public int size() {
        return data.length;
    }

    @Override
    public ByteTag get(int i) {
        return ByteTag.valueOf(data[i]);
    }

    @Override
    public ByteTag set(int i, ByteTag t) {
        byte old = data[i];
        data[i] = t.getAsByte();
        return ByteTag.valueOf(old);
    }

    @Override
    public void add(int i, ByteTag t) {
        insert(i, t.getAsByte());
    }

    /** Observed: past the end, an IndexOutOfBoundsException saying "Index: i, Length: n". */
    private void insert(int i, byte v) {
        if (i < 0 || i > data.length) throw new IndexOutOfBoundsException("Index: " + i + ", Length: " + data.length);
        byte[] grown = new byte[data.length + 1];
        System.arraycopy(data, 0, grown, 0, i);
        grown[i] = v;
        System.arraycopy(data, i, grown, i + 1, data.length - i);
        data = grown;
    }

    /** Observed: a number of any kind, as this kind; anything else isn't taken. */
    @Override
    public boolean setTag(int i, Tag t) {
        if (!(t instanceof NumericTag n)) return false;
        data[i] = n.getAsByte();
        return true;
    }

    @Override
    public boolean addTag(int i, Tag t) {
        if (!(t instanceof NumericTag n)) return false;
        insert(i, n.getAsByte());
        return true;
    }

    /** Observed: an index past the end throws as reading the array does. */
    @Override
    public ByteTag remove(int i) {
        byte old = data[i];
        byte[] shrunk = new byte[data.length - 1];
        System.arraycopy(data, 0, shrunk, 0, i);
        System.arraycopy(data, i + 1, shrunk, i, data.length - i - 1);
        data = shrunk;
        return ByteTag.valueOf(old);
    }

    @Override
    public byte getElementType() {
        return 1;
    }

    @Override
    public void clear() {
        data = new byte[0];
    }
}
