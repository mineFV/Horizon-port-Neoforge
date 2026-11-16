package net.minecraft.nbt;

/** The stand-in for the game's LongTag: long numbers; texts, hash and conversions observed (`standins`). */
public class LongTag extends NumericTag {
    private final long data;

    private LongTag(long data) {
        this.data = data;
    }

    private static final LongTag[] CACHE = new LongTag[1153];

    static {
        for (int i = 0; i < CACHE.length; i++) CACHE[i] = new LongTag((long) (-128 + i));
    }

    /** Observed: the same object each time from -128 to 1024, a new one otherwise. */
    public static LongTag valueOf(long v) {
        return v >= -128 && v <= 1024 ? CACHE[(int) v - (-128)] : new LongTag(v);
    }

    @Override
    public byte getId() {
        return 4;
    }

    @Override
    public String toString() {
        return data + "L";
    }

    /** Observed: itself (it never changes). */
    @Override
    public LongTag copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof LongTag other && data == other.data;
    }

    @Override
    public int hashCode() {
        return (int) (data ^ data >>> 32);
    }

    @Override
    public long getAsLong() {
        return data;
    }

    @Override
    public int getAsInt() {
        return (int) data;
    }

    @Override
    public short getAsShort() {
        return (short) (data & 65535L);
    }

    @Override
    public byte getAsByte() {
        return (byte) (data & 255L);
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
        return Long.valueOf(data);
    }
}
