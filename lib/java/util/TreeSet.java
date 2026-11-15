package java.util;

/**
 * The model of java.util.TreeSet: a TreeMap's keys (see TreeMap). Not modelled: the views (headSet, tailSet,
 * subSet, descendingSet, descendingIterator).
 */
public class TreeSet<E> extends AbstractSet<E> implements NavigableSet<E>, Cloneable, java.io.Serializable {
    private final TreeMap<E, Object> m;

    private static final Object PRESENT = new Object();

    public TreeSet() {
        m = new TreeMap<>();
    }

    public TreeSet(Comparator<? super E> comparator) {
        m = new TreeMap<>(comparator);
    }

    public TreeSet(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    public TreeSet(SortedSet<E> s) {
        this(s.comparator());
        addAll(s);
    }

    public Iterator<E> iterator() {
        return m.keySet().iterator();
    }

    public int size() {
        return m.size();
    }

    public boolean isEmpty() {
        return m.isEmpty();
    }

    public boolean contains(Object o) {
        return m.containsKey(o);
    }

    public boolean add(E e) {
        return m.put(e, PRESENT) == null;
    }

    public boolean remove(Object o) {
        return m.remove(o) == PRESENT;
    }

    public void clear() {
        m.clear();
    }

    public Comparator<? super E> comparator() {
        return m.comparator();
    }

    public E first() {
        return m.firstKey();
    }

    public E last() {
        return m.lastKey();
    }

    public E lower(E e) {
        return m.lowerKey(e);
    }

    public E floor(E e) {
        return m.floorKey(e);
    }

    public E ceiling(E e) {
        return m.ceilingKey(e);
    }

    public E higher(E e) {
        return m.higherKey(e);
    }

    public E pollFirst() {
        Map.Entry<E, ?> e = m.pollFirstEntry();
        return (e == null) ? null : e.getKey();
    }

    public E pollLast() {
        Map.Entry<E, ?> e = m.pollLastEntry();
        return (e == null) ? null : e.getKey();
    }

    /** Sorted by the set's comparator, which it gives (null for the natural order: a stream's sorted() then does nothing). */
    public Spliterator<E> spliterator() {
        Comparator<? super E> order = m.comparator();
        return new Spliterators.IteratorSpliterator<E>(this, Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED) {
            @Override
            public Comparator<? super E> getComparator() {
                return order;
            }
        };
    }
}
