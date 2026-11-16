package net.minecraft.nbt;

/** The stand-in for the game's ByteTag: byte numbers; texts, hash and conversions observed (`standins`). */
public class ByteTag extends NumericTag {
    private final byte data;

    private ByteTag(byte data) {
        this.data = data;
    }

    private static final ByteTag[] CACHE = new ByteTag[256];

    static {
        for (int i = 0; i < 256; i++) CACHE[i] = new ByteTag((byte) (i - 128));
    }

    /** Observed: one tag for each byte, the same object each time. */
    public static final ByteTag ZERO = valueOf((byte) 0);
    public static final ByteTag ONE = valueOf((byte) 1);

    public static ByteTag valueOf(byte v) {
        return CACHE[v + 128];
    }

    /** Observed: ONE for true, ZERO for false. */
    public static ByteTag valueOf(boolean v) {
        return v ? ONE : ZERO;
    }

    @Override
    public byte getId() {
        return 1;
    }

    @Override
    public String toString() {
        return data + "b";
    }

    /** Observed: itself (it never changes). */
    @Override
    public ByteTag copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ByteTag other && data == other.data;
    }

    @Override
    public int hashCode() {
        return data;
    }

    @Override
    public long getAsLong() {
        return data;
    }

    @Override
    public int getAsInt() {
        return data;
    }

    @Override
    public short getAsShort() {
        return (short) data;
    }

    @Override
    public byte getAsByte() {
        return data;
    }

    @Override
    public double getAsDouble() {
        return data;
    }

    @Override
    public float getAsFloat() {
        return data;
    }

    @Override
    public Number getAsNumber() {
        return Byte.valueOf(data);
    }
}
