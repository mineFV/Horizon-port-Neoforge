package java.util;

/** The model of java.util.HashSet: the keys of a HashMap, so it iterates in the JDK's order too. clone isn't modelled. */
public class HashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, java.io.Serializable {
    transient HashMap<E, Object> map;

    static final Object PRESENT = new Object();

    public HashSet() {
        map = new HashMap<>();
    }

    public HashSet(Collection<? extends E> c) {
        map = HashMap.newHashMap(Math.max(c.size(), 12));
        addAll(c);
    }

    public HashSet(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    public HashSet(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

    /** LinkedHashSet's. */
    HashSet(int initialCapacity, float loadFactor, boolean dummy) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    public static <T> HashSet<T> newHashSet(int numElements) {
        if (numElements < 0) throw new IllegalArgumentException("Negative number of elements: ".concat(Integer.toString(numElements)));
        return new HashSet<>(HashMap.calculateHashMapCapacity(numElements));
    }

    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }

    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }

    public void clear() {
        map.clear();
    }

    public Object[] toArray() {
        return map.keySet().toArray();
    }

    public <T> T[] toArray(T[] a) {
        return map.keySet().toArray(a);
    }

    /** The map's keys' spliterator: over the table for a HashSet, by the iterator for a LinkedHashSet. */
    @Override
    public Spliterator<E> spliterator() {
        return map.keySet().spliterator();
    }
}
