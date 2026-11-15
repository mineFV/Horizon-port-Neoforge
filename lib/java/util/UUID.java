package java.util;

/**
 * The model of java.util.UUID: its two longs, their text, equality, order and hash as the JDK's. randomUUID's bits come
 * from the runtime's random source (the JDK's from a SecureRandom: either is random). Not modelled: fromString,
 * nameUUIDFromBytes, timestamp and its kin.
 */
public final class UUID implements java.io.Serializable, Comparable<UUID> {
    private final long mostSigBits;
    private final long leastSigBits;

    public UUID(long mostSigBits, long leastSigBits) {
        this.mostSigBits = mostSigBits;
        this.leastSigBits = leastSigBits;
    }

    /** Version 4, IETF variant: the bits the JDK sets in its random 16 bytes. */
    public static UUID randomUUID() {
        long msb = jRandom();
        long lsb = jRandom();
        msb = (msb & ~0xf000L) | 0x4000L;
        lsb = (lsb & 0x3fffffffffffffffL) | 0x8000000000000000L;
        return new UUID(msb, lsb);
    }

    private static native long jRandom();

    public long getLeastSignificantBits() {
        return leastSigBits;
    }

    public long getMostSignificantBits() {
        return mostSigBits;
    }

    public int version() {
        return (int) ((mostSigBits >> 12) & 0x0f);
    }

    /** By the top bits of the least significant long: 0xx is 0, 10x is 2 (IETF), 110 is 6, 111 is 7. */
    public int variant() {
        int top = (int) (leastSigBits >>> 61);
        if ((top & 4) == 0) return 0;
        if ((top & 2) == 0) return 2;
        return top;
    }

    public String toString() {
        return digits(mostSigBits >> 32, 8).concat("-").concat(digits(mostSigBits >> 16, 4)).concat("-").concat(digits(mostSigBits, 4)).concat("-")
                .concat(digits(leastSigBits >> 48, 4)).concat("-").concat(digits(leastSigBits, 12));
    }

    /** The low n hexadecimal digits of val, lower case, zeros in front. */
    private static String digits(long val, int n) {
        char[] out = new char[n];
        for (int i = n - 1; i >= 0; i--) {
            out[i] = "0123456789abcdef".charAt((int) (val & 0xf));
            val >>>= 4;
        }
        return new String(out);
    }

    public int hashCode() {
        long hilo = mostSigBits ^ leastSigBits;
        return ((int) (hilo >> 32)) ^ (int) hilo;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof UUID id)) return false;
        return mostSigBits == id.mostSigBits && leastSigBits == id.leastSigBits;
    }

    /** By the most significant bits, then the least, as signed longs (as the JDK compares them). */
    public int compareTo(UUID val) {
        int mostSigBits = Long.compare(this.mostSigBits, val.mostSigBits);
        return mostSigBits != 0 ? mostSigBits : Long.compare(this.leastSigBits, val.leastSigBits);
    }
}
