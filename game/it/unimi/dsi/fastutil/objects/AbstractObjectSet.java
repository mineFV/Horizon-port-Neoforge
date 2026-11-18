package it.unimi.dsi.fastutil.objects;

/** The stand-in for fastutil's AbstractObjectSet: a set's equality and hash. */
public abstract class AbstractObjectSet<K> extends AbstractObjectCollection<K> implements Cloneable, ObjectSet<K> {
    protected AbstractObjectSet() {}

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
        while (n-- != 0) {
            K k = i.next();
            h += k == null ? 0 : k.hashCode();
        }
        return h;
    }
}
