package net.minecraft.nbt;

/** The stand-in for the game's IntTag: int numbers; texts, hash and conversions observed (`standins`). */
public class IntTag extends NumericTag {
    private final int data;

    private IntTag(int data) {
        this.data = data;
    }

    private static final IntTag[] CACHE = new IntTag[1153];

    static {
        for (int i = 0; i < CACHE.length; i++) CACHE[i] = new IntTag((int) (-128 + i));
    }

    /** Observed: the same object each time from -128 to 1024, a new one otherwise. */
    public static IntTag valueOf(int v) {
        return v >= -128 && v <= 1024 ? CACHE[(int) v - (-128)] : new IntTag(v);
    }

    @Override
    public byte getId() {
        return 3;
    }

    @Override
    public String toString() {
        return data + "";
    }

    /** Observed: itself (it never changes). */
    @Override
    public IntTag copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof IntTag other && data == other.data;
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
        return (short) (data & 65535);
    }

    @Override
    public byte getAsByte() {
        return (byte) (data & 255);
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
        return Integer.valueOf(data);
    }
}
