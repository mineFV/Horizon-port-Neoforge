package it.unimi.dsi.fastutil.objects;

/** The stand-in for fastutil's AbstractReferenceCollection: its text, {a, b} in its order (observed, `standins`). */
public abstract class AbstractReferenceCollection<K> extends java.util.AbstractCollection<K> implements ReferenceCollection<K> {
    protected AbstractReferenceCollection() {}

    @Override
    public abstract ObjectIterator<K> iterator();

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("{");
        ObjectIterator<K> i = iterator();
        boolean first = true;
        while (i.hasNext()) {
            if (first) first = false;
            else s.append(", ");
            Object k = i.next();
            s.append(this == k ? "(this collection)" : String.valueOf(k));
        }
        return s.append('}').toString();
    }
}
