package net.minecraft.nbt;

/** The stand-in for the game's DoubleTag: double numbers; texts, hash and conversions observed (`standins`). */
public class DoubleTag extends NumericTag {
    /** Observed: the tag of zero, which valueOf gives for 0 and -0 alike. */
    public static final DoubleTag ZERO = new DoubleTag(0.0);

    private final double data;

    private DoubleTag(double data) {
        this.data = data;
    }

    /** Observed: ZERO for a zero (negative or not), a new tag otherwise. */
    public static DoubleTag valueOf(double v) {
        return v == 0.0 ? ZERO : new DoubleTag(v);
    }

    @Override
    public byte getId() {
        return 6;
    }

    @Override
    public String toString() {
        return data + "d";
    }

    /** Observed: itself (it never changes). */
    @Override
    public DoubleTag copy() {
        return this;
    }

    /** Observed: equal numbers by ==, so NaN isn't equal to itself. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof DoubleTag other && data == other.data;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(data);
        return (int) (bits ^ bits >>> 32);
    }

    /** Observed: the floor, as a long. */
    @Override
    public long getAsLong() {
        return (long) Math.floor(data);
    }

    /** Observed: the floor, as the game's ints round it (NumericTag.floor). */
    @Override
    public int getAsInt() {
        return floor(data);
    }

    @Override
    public short getAsShort() {
        return (short) (floor(data) & 65535);
    }

    @Override
    public byte getAsByte() {
        return (byte) (floor(data) & 255);
    }

    @Override
    public double getAsDouble() {
        return data;
    }

    @Override
    public float getAsFloat() {
        return (float) data;
    }

    @Override
    public Number getAsNumber() {
        return Double.valueOf(data);
    }
}
