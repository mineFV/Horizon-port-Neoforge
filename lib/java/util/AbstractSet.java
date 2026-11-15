package java.util;

/** The model of java.util.AbstractSet. */
public abstract class AbstractSet<E> extends AbstractCollection<E> implements Set<E> {
    protected AbstractSet() {}

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Set<?> s)) return false;
        if (s.size() != size()) return false;
        try {
            return containsAll(s);
        } catch (ClassCastException | NullPointerException unused) {
            return false;
        }
    }

    public int hashCode() {
        int h = 0;
        Iterator<E> i = iterator();
        while (i.hasNext()) {
            E obj = i.next();
            if (obj != null) h += obj.hashCode();
        }
        return h;
    }

    /** As the JDK's: over the smaller of the two. */
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        if (size() > c.size()) {
            for (Object e : c) if (remove(e)) modified = true;
        } else {
            for (Iterator<?> i = iterator(); i.hasNext();) {
                if (c.contains(i.next())) {
                    i.remove();
                    modified = true;
                }
            }
        }
        return modified;
    }
}
