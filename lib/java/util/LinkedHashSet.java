package java.util;

/** The model of java.util.LinkedHashSet: a HashSet over a LinkedHashMap, iterated in the order added. reversed isn't modelled. */
public class LinkedHashSet<E> extends HashSet<E> implements SequencedSet<E>, Cloneable, java.io.Serializable {
    public LinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
    }

    public LinkedHashSet(int initialCapacity) {
        super(initialCapacity, .75f, true);
    }

    public LinkedHashSet() {
        super(16, .75f, true);
    }

    public LinkedHashSet(Collection<? extends E> c) {
        super(HashMap.calculateHashMapCapacity(Math.max(c.size(), 12)), .75f, true);
        addAll(c);
    }

    public static <T> LinkedHashSet<T> newLinkedHashSet(int numElements) {
        if (numElements < 0) throw new IllegalArgumentException("Negative number of elements: ".concat(Integer.toString(numElements)));
        return new LinkedHashSet<>(HashMap.calculateHashMapCapacity(numElements));
    }

    public E getFirst() {
        return iterator().next();
    }
}
