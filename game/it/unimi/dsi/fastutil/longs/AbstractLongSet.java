package it.unimi.dsi.fastutil.longs;

/** The stand-in for fastutil's AbstractLongSet: a set's equality, and its hash, the sum of its longs' hashes. */
public abstract class AbstractLongSet extends AbstractLongCollection implements Cloneable, LongSet {
    protected AbstractLongSet() {}

    @Override
    public abstract LongIterator iterator();

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof java.util.Set<?> s)) return false;
        if (s.size() != size()) return false;
        return containsAll(s);
    }

    @Override
    public int hashCode() {
        int h = 0, n = size();
        LongIterator i = iterator();
        while (n-- != 0) h += it.unimi.dsi.fastutil.HashCommon.long2int(i.nextLong());
        return h;
    }

    @Override
    public boolean remove(long k) {
        return super.rem(k);
    }

    @Override
    public boolean rem(long k) {
        return remove(k);
    }

    @Override
    public boolean remove(Object o) {
        return LongSet.super.remove(o);
    }
}
