package net.minecraft.nbt;

/** The stand-in for the game's NumericTag: each kind's conversions are observed (`standins`). */
public abstract class NumericTag implements Tag {
    protected NumericTag() {}

    public abstract long getAsLong();

    public abstract int getAsInt();

    public abstract short getAsShort();

    public abstract byte getAsByte();

    public abstract double getAsDouble();

    public abstract float getAsFloat();

    public abstract Number getAsNumber();

    /** Each kind writes its own (observed: the number and the kind's suffix). */
    @Override
    public abstract String toString();

    /** Observed: the game's floor of a float or a double as an int: the cast, one less when the cast went up (it wraps below MIN). */
    static int floor(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }
}
