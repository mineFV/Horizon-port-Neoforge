package java.util;

/**
 * The model of java.util.BitSet: bits in longs, the words in use and the array's growth (doubling, or what is
 * needed) as the JDK's, so that size() is its. Not modelled: clone, stream, valueOf of byte arrays and buffers.
 */
public class BitSet implements Cloneable, java.io.Serializable {
    private long[] words;
    private int wordsInUse = 0;
    private boolean sizeIsSticky = false;

    private static int wordIndex(int bitIndex) {
        return bitIndex >> 6;
    }

    public BitSet() {
        words = new long[wordIndex(64 - 1) + 1];
        sizeIsSticky = false;
    }

    public BitSet(int nbits) {
        if (nbits < 0) throw new NegativeArraySizeException("nbits < 0: ".concat(Integer.toString(nbits)));
        words = new long[wordIndex(nbits - 1) + 1];
        sizeIsSticky = true;
    }

    private BitSet(long[] words) {
        this.words = words;
        this.wordsInUse = words.length;
        checkInvariants();
    }

    public static BitSet valueOf(long[] longs) {
        int n;
        for (n = longs.length; n > 0 && longs[n - 1] == 0; n--) {
        }
        return new BitSet(Arrays.copyOf(longs, n));
    }

    public long[] toLongArray() {
        return Arrays.copyOf(words, wordsInUse);
    }

    private void checkInvariants() {}

    private void recalculateWordsInUse() {
        int i;
        for (i = wordsInUse - 1; i >= 0; i--) if (words[i] != 0) break;
        wordsInUse = i + 1;
    }

    private void ensureCapacity(int wordsRequired) {
        if (words.length < wordsRequired) {
            int request = Math.max(2 * words.length, wordsRequired);
            words = Arrays.copyOf(words, request);
            sizeIsSticky = false;
        }
    }

    private void expandTo(int wordIndex) {
        int wordsRequired = wordIndex + 1;
        if (wordsInUse < wordsRequired) {
            ensureCapacity(wordsRequired);
            wordsInUse = wordsRequired;
        }
    }

    private static void checkRange(int fromIndex, int toIndex) {
        if (fromIndex < 0) throw new IndexOutOfBoundsException("fromIndex < 0: ".concat(Integer.toString(fromIndex)));
        if (toIndex < 0) throw new IndexOutOfBoundsException("toIndex < 0: ".concat(Integer.toString(toIndex)));
        if (fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("fromIndex: ".concat(Integer.toString(fromIndex)).concat(" > toIndex: ").concat(Integer.toString(toIndex)));
        }
    }

    public void flip(int bitIndex) {
        if (bitIndex < 0) throw new IndexOutOfBoundsException("bitIndex < 0: ".concat(Integer.toString(bitIndex)));
        int wordIndex = wordIndex(bitIndex);
        expandTo(wordIndex);
        words[wordIndex] ^= (1L << bitIndex);
        recalculateWordsInUse();
    }

    public void set(int bitIndex) {
        if (bitIndex < 0) throw new IndexOutOfBoundsException("bitIndex < 0: ".concat(Integer.toString(bitIndex)));
        int wordIndex = wordIndex(bitIndex);
        expandTo(wordIndex);
        words[wordIndex] |= (1L << bitIndex);
    }

    public void set(int bitIndex, boolean value) {
        if (value) set(bitIndex);
        else clear(bitIndex);
    }

    public void set(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        if (fromIndex == toIndex) return;
        // Grown once, to the last word (as the JDK's).
        expandTo(wordIndex(toIndex - 1));
        for (int i = fromIndex; i < toIndex; i++) words[wordIndex(i)] |= (1L << i);
    }

    public void clear(int bitIndex) {
        if (bitIndex < 0) throw new IndexOutOfBoundsException("bitIndex < 0: ".concat(Integer.toString(bitIndex)));
        int wordIndex = wordIndex(bitIndex);
        if (wordIndex >= wordsInUse) return;
        words[wordIndex] &= ~(1L << bitIndex);
        recalculateWordsInUse();
    }

    public void clear(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        if (fromIndex == toIndex) return;
        for (int i = fromIndex; i < toIndex; i++) {
            int wordIndex = wordIndex(i);
            if (wordIndex >= wordsInUse) break;
            words[wordIndex] &= ~(1L << i);
        }
        recalculateWordsInUse();
    }

    public void clear() {
        while (wordsInUse > 0) words[--wordsInUse] = 0;
    }

    public boolean get(int bitIndex) {
        if (bitIndex < 0) throw new IndexOutOfBoundsException("bitIndex < 0: ".concat(Integer.toString(bitIndex)));
        int wordIndex = wordIndex(bitIndex);
        return (wordIndex < wordsInUse) && ((words[wordIndex] & (1L << bitIndex)) != 0);
    }

    public int nextSetBit(int fromIndex) {
        if (fromIndex < 0) throw new IndexOutOfBoundsException("fromIndex < 0: ".concat(Integer.toString(fromIndex)));
        int u = wordIndex(fromIndex);
        if (u >= wordsInUse) return -1;
        long word = words[u] & (-1L << fromIndex);
        while (true) {
            if (word != 0) return (u * 64) + Long.numberOfTrailingZeros(word);
            if (++u == wordsInUse) return -1;
            word = words[u];
        }
    }

    public int nextClearBit(int fromIndex) {
        if (fromIndex < 0) throw new IndexOutOfBoundsException("fromIndex < 0: ".concat(Integer.toString(fromIndex)));
        int u = wordIndex(fromIndex);
        if (u >= wordsInUse) return fromIndex;
        long word = ~words[u] & (-1L << fromIndex);
        while (true) {
            if (word != 0) return (u * 64) + Long.numberOfTrailingZeros(word);
            if (++u == wordsInUse) return wordsInUse * 64;
            word = ~words[u];
        }
    }

    public int previousSetBit(int fromIndex) {
        if (fromIndex < 0) {
            if (fromIndex == -1) return -1;
            throw new IndexOutOfBoundsException("fromIndex < -1: ".concat(Integer.toString(fromIndex)));
        }
        int u = wordIndex(fromIndex);
        if (u >= wordsInUse) return length() - 1;
        long word = words[u] & (-1L >>> -(fromIndex + 1));
        while (true) {
            if (word != 0) return (u + 1) * 64 - 1 - Long.numberOfLeadingZeros(word);
            if (u-- == 0) return -1;
            word = words[u];
        }
    }

    public int previousClearBit(int fromIndex) {
        if (fromIndex < 0) {
            if (fromIndex == -1) return -1;
            throw new IndexOutOfBoundsException("fromIndex < -1: ".concat(Integer.toString(fromIndex)));
        }
        int u = wordIndex(fromIndex);
        if (u >= wordsInUse) return fromIndex;
        long word = ~words[u] & (-1L >>> -(fromIndex + 1));
        while (true) {
            if (word != 0) return (u + 1) * 64 - 1 - Long.numberOfLeadingZeros(word);
            if (u-- == 0) return -1;
            word = ~words[u];
        }
    }

    public int length() {
        if (wordsInUse == 0) return 0;
        return 64 * (wordsInUse - 1) + (64 - Long.numberOfLeadingZeros(words[wordsInUse - 1]));
    }

    public boolean isEmpty() {
        return wordsInUse == 0;
    }

    public boolean intersects(BitSet set) {
        for (int i = Math.min(wordsInUse, set.wordsInUse) - 1; i >= 0; i--) if ((words[i] & set.words[i]) != 0) return true;
        return false;
    }

    public int cardinality() {
        int sum = 0;
        for (int i = 0; i < wordsInUse; i++) sum += Long.bitCount(words[i]);
        return sum;
    }

    public void and(BitSet set) {
        if (this == set) return;
        while (wordsInUse > set.wordsInUse) words[--wordsInUse] = 0;
        for (int i = 0; i < wordsInUse; i++) words[i] &= set.words[i];
        recalculateWordsInUse();
    }

    public void or(BitSet set) {
        if (this == set) return;
        int wordsInCommon = Math.min(wordsInUse, set.wordsInUse);
        if (wordsInUse < set.wordsInUse) {
            ensureCapacity(set.wordsInUse);
            wordsInUse = set.wordsInUse;
        }
        for (int i = 0; i < wordsInCommon; i++) words[i] |= set.words[i];
        for (int i = wordsInCommon; i < set.wordsInUse; i++) words[i] = set.words[i];
    }

    public void xor(BitSet set) {
        int wordsInCommon = Math.min(wordsInUse, set.wordsInUse);
        if (wordsInUse < set.wordsInUse) {
            ensureCapacity(set.wordsInUse);
            wordsInUse = set.wordsInUse;
        }
        for (int i = 0; i < wordsInCommon; i++) words[i] ^= set.words[i];
        for (int i = wordsInCommon; i < set.wordsInUse; i++) words[i] = set.words[i];
        recalculateWordsInUse();
    }

    public void andNot(BitSet set) {
        for (int i = Math.min(wordsInUse, set.wordsInUse) - 1; i >= 0; i--) words[i] &= ~set.words[i];
        recalculateWordsInUse();
    }

    public int hashCode() {
        long h = 1234;
        for (int i = wordsInUse; --i >= 0; ) h ^= words[i] * (i + 1);
        return (int) ((h >> 32) ^ h);
    }

    public int size() {
        return words.length * 64;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof BitSet set)) return false;
        if (this == obj) return true;
        if (wordsInUse != set.wordsInUse) return false;
        for (int i = 0; i < wordsInUse; i++) if (words[i] != set.words[i]) return false;
        return true;
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('{');
        int i = nextSetBit(0);
        if (i != -1) {
            b.append(i);
            while (true) {
                if (++i < 0) break;
                if ((i = nextSetBit(i)) < 0) break;
                int endOfRun = nextClearBit(i);
                do {
                    b.append(", ").append(i);
                } while (++i != endOfRun);
            }
        }
        b.append('}');
        return b.toString();
    }
}
