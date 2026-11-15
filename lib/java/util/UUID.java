package java.util;

/**
 * The model of java.util.UUID: its two longs, their text, equality, order and hash as the JDK's. randomUUID's bits come
 * from the runtime's random source (the JDK's from a SecureRandom: either is random); nameUUIDFromBytes's MD5 is
 * written here from RFC 1321. Not modelled: fromString, timestamp and its kin.
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

    /** Version 3: the MD5 of the bytes, with the version and the IETF variant set. */
    public static UUID nameUUIDFromBytes(byte[] name) {
        byte[] md5 = md5(name);
        md5[6] &= 0x0f;
        md5[6] |= 0x30;
        md5[8] &= 0x3f;
        md5[8] = (byte) (md5[8] | 0x80);
        long msb = 0, lsb = 0;
        for (int i = 0; i < 8; i++) msb = (msb << 8) | (md5[i] & 0xff);
        for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (md5[i] & 0xff);
        return new UUID(msb, lsb);
    }

    private static final int[] SHIFTS = {7, 12, 17, 22, 5, 9, 14, 20, 4, 11, 16, 23, 6, 10, 15, 21};

    /** RFC 1321's table: the integer part of 2^32 times abs(sin(i)), i from 1 to 64. */
    private static final int[] T = {
        0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee, 0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
        0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be, 0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
        0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa, 0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
        0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed, 0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
        0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c, 0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
        0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05, 0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
        0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039, 0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
        0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1, 0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391};

    /** RFC 1321's MD5 of the bytes. */
    static byte[] md5(byte[] message) {
        int[] k = T;
        long bitLength = (long) message.length * 8;
        int padded = ((message.length + 8) / 64 + 1) * 64;
        byte[] m = java.util.Arrays.copyOf(message, padded);
        m[message.length] = (byte) 0x80;
        for (int i = 0; i < 8; i++) m[padded - 8 + i] = (byte) (bitLength >>> (8 * i));
        int a0 = 0x67452301, b0 = 0xefcdab89, c0 = 0x98badcfe, d0 = 0x10325476;
        for (int chunk = 0; chunk < padded; chunk += 64) {
            int[] w = new int[16];
            for (int i = 0; i < 16; i++) {
                int at = chunk + i * 4;
                w[i] = (m[at] & 0xff) | (m[at + 1] & 0xff) << 8 | (m[at + 2] & 0xff) << 16 | (m[at + 3] & 0xff) << 24;
            }
            int a = a0, b = b0, c = c0, d = d0;
            for (int i = 0; i < 64; i++) {
                int f, g;
                if (i < 16) {
                    f = (b & c) | (~b & d);
                    g = i;
                } else if (i < 32) {
                    f = (d & b) | (~d & c);
                    g = (5 * i + 1) % 16;
                } else if (i < 48) {
                    f = b ^ c ^ d;
                    g = (3 * i + 5) % 16;
                } else {
                    f = c ^ (b | ~d);
                    g = (7 * i) % 16;
                }
                int rotated = a + f + k[i] + w[g];
                a = d;
                d = c;
                c = b;
                b = b + Integer.rotateLeft(rotated, SHIFTS[(i / 16) * 4 + i % 4]);
            }
            a0 += a;
            b0 += b;
            c0 += c;
            d0 += d;
        }
        byte[] out = new byte[16];
        int[] words = {a0, b0, c0, d0};
        for (int i = 0; i < 16; i++) out[i] = (byte) (words[i / 4] >>> (8 * (i % 4)));
        return out;
    }

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
