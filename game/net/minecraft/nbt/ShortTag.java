package net.minecraft.nbt;

/** The stand-in for the game's ShortTag: short numbers; texts, hash and conversions observed (`standins`). */
public class ShortTag extends NumericTag {
    private final short data;

    private ShortTag(short data) {
        this.data = data;
    }

    private static final ShortTag[] CACHE = new ShortTag[1153];

    static {
        for (int i = 0; i < CACHE.length; i++) CACHE[i] = new ShortTag((short) (-128 + i));
    }

    /** Observed: the same object each time from -128 to 1024, a new one otherwise. */
    public static ShortTag valueOf(short v) {
        return v >= -128 && v <= 1024 ? CACHE[(int) v - (-128)] : new ShortTag(v);
    }

    @Override
    public byte getId() {
        return 2;
    }

    @Override
    public String toString() {
        return data + "s";
    }

    /** Observed: itself (it never changes). */
    @Override
    public ShortTag copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ShortTag other && data == other.data;
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
        return data;
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
        return Short.valueOf(data);
    }
}
