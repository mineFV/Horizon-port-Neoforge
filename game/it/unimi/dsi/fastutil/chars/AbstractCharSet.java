package it.unimi.dsi.fastutil.chars;

/** The stand-in for fastutil's AbstractCharSet: a set's equality, and its hash, the sum of its chars (observed). */
public abstract class AbstractCharSet extends AbstractCharCollection implements Cloneable, CharSet {
    protected AbstractCharSet() {}

    @Override
    public abstract CharIterator iterator();

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
        CharIterator i = iterator();
        while (n-- != 0) h += i.nextChar();
        return h;
    }

    @Override
    public boolean remove(char k) {
        return super.rem(k);
    }

    @Override
    public boolean rem(char k) {
        return remove(k);
    }

    @Override
    public boolean remove(Object o) {
        return CharSet.super.remove(o);
    }
}
