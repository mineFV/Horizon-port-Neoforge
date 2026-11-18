package it.unimi.dsi.fastutil.objects;

/** The stand-in for fastutil's ObjectIterator. */
public interface ObjectIterator<K> extends java.util.Iterator<K> {
    /** Skips n elements (or as many as there are): how many were skipped. */
    default int skip(int n) {
        if (n < 0) throw new IllegalArgumentException("Argument must be nonnegative: " + n);
        int i = n;
        while (i-- != 0 && hasNext()) next();
        return n - i - 1;
    }
}
