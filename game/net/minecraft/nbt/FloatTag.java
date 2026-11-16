package net.minecraft.nbt;

/** The stand-in for the game's FloatTag: float numbers; texts, hash and conversions observed (`standins`). */
public class FloatTag extends NumericTag {
    /** Observed: the tag of zero, which valueOf gives for 0 and -0 alike. */
    public static final FloatTag ZERO = new FloatTag(0.0F);

    private final float data;

    private FloatTag(float data) {
        this.data = data;
    }

    /** Observed: ZERO for a zero (negative or not), a new tag otherwise. */
    public static FloatTag valueOf(float v) {
        return v == 0.0F ? ZERO : new FloatTag(v);
    }

    @Override
    public byte getId() {
        return 5;
    }

    @Override
    public String toString() {
        return data + "f";
    }

    /** Observed: itself (it never changes). */
    @Override
    public FloatTag copy() {
        return this;
    }

    /** Observed: equal numbers by ==, so NaN isn't equal to itself. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof FloatTag other && data == other.data;
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(data);
    }

    /** Observed: the cast. */
    @Override
    public long getAsLong() {
        return (long) data;
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
        return data;
    }

    @Override
    public Number getAsNumber() {
        return Float.valueOf(data);
    }
}
