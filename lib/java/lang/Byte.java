package java.lang;

/** The model of java.lang.Byte: boxes (valueOf's cache from -128 to 127 included). */
public final class Byte extends Number implements Comparable<Byte> {
    @SuppressWarnings("unchecked")
    public static final Class<Byte> TYPE = (Class<Byte>) Class.jPrimitive('B');

    private final byte value;

    public Byte(byte value) {
        this.value = value;
    }

    /** The objects valueOf gives from -128 to 127: always the same ones, as the JDK's cache. */
    private static final Byte[] CACHE = new Byte[127 - (-128) + 1];

    static {
        for (int i = 0; i < CACHE.length; i++) CACHE[i] = new Byte((byte) (i + (-128)));
    }

    public static Byte valueOf(byte i) {
        if (i >= -128 && i <= 127) return CACHE[(int) i - (-128)];
        return new Byte(i);
    }

    public int intValue() {
        return (int) value;
    }

    public long longValue() {
        return (long) value;
    }

    public float floatValue() {
        return (float) value;
    }

    public double doubleValue() {
        return (double) value;
    }

    public static int hashCode(byte value) {
        return (int) value;
    }

    public int hashCode() {
        return hashCode(value);
    }

    public boolean equals(Object obj) {
        return obj instanceof Byte other && value == other.value;
    }

    /** As the JDK specifies it for Byte: the difference. */
    public static int compare(byte x, byte y) {
        return x - y;
    }

    public int compareTo(Byte other) {
        return compare(value, other.value);
    }

    public String toString() {
        return Long.toString(value);
    }

    public static String toString(byte s) {
        return Long.toString(s);
    }
}
