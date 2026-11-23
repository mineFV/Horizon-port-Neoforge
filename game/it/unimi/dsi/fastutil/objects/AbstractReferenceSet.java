package it.unimi.dsi.fastutil.objects;

/**
 * The stand-in for fastutil's AbstractReferenceSet: a set's equality (its own contains, by identity) and hash (the
 * elements' identity hashes), observed against fastutil's (`standins`).
 */
public abstract class AbstractReferenceSet<K> extends AbstractReferenceCollection<K> implements Cloneable, ReferenceSet<K> {
    protected AbstractReferenceSet() {}

    @Override
    public abstract ObjectIterator<K> iterator();

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof java.util.Set<?> s)) return false;
        if (s.size() != size()) return false;
        return containsAll(s);
    }

    @Override
    public int hashCode() {
        int h = 0;
        ObjectIterator<K> i = iterator();
        int n = size();
        while (n-- != 0) h += System.identityHashCode(i.next());
        return h;
    }
}
