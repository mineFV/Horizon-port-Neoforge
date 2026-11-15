package java.util;

/**
 * The model of java.util.Collections: its empty, single and unmodifiable collections (with the JDK's class names and
 * messages), and sort, reverse, swap, addAll, max, min, frequency, nCopies, reverseOrder. Sorting gives the JDK's result
 * (any stable sort does) but asks the comparator in another order. Synchronized and checked wrappers, shuffle and the
 * rest aren't modelled.
 */
public class Collections {
    private Collections() {}

    @SuppressWarnings("rawtypes")
    public static final List EMPTY_LIST = new EmptyList<>();
    @SuppressWarnings("rawtypes")
    public static final Set EMPTY_SET = new EmptySet<>();
    @SuppressWarnings("rawtypes")
    public static final Map EMPTY_MAP = new EmptyMap<>();

    @SuppressWarnings("unchecked")
    public static final <T> List<T> emptyList() {
        return (List<T>) EMPTY_LIST;
    }

    @SuppressWarnings("unchecked")
    public static final <T> Set<T> emptySet() {
        return (Set<T>) EMPTY_SET;
    }

    @SuppressWarnings("unchecked")
    public static final <K, V> Map<K, V> emptyMap() {
        return (Map<K, V>) EMPTY_MAP;
    }

    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> emptyIterator() {
        return (Iterator<T>) EmptyIterator.EMPTY_ITERATOR;
    }

    private static final class EmptyIterator<E> implements Iterator<E> {
        static final EmptyIterator<Object> EMPTY_ITERATOR = new EmptyIterator<>();

        public boolean hasNext() {
            return false;
        }

        public E next() {
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new IllegalStateException();
        }
    }

    private static final class EmptyList<E> extends AbstractList<E> implements RandomAccess, java.io.Serializable {
        public Iterator<E> iterator() {
            return emptyIterator();
        }

        public int size() {
            return 0;
        }

        public boolean isEmpty() {
            return true;
        }

        public void clear() {}

        public boolean contains(Object obj) {
            return false;
        }

        public boolean containsAll(Collection<?> c) {
            return c.isEmpty();
        }

        public Object[] toArray() {
            return new Object[0];
        }

        public <T> T[] toArray(T[] a) {
            if (a.length > 0) a[0] = null;
            return a;
        }

        public E get(int index) {
            throw new IndexOutOfBoundsException("Index: ".concat(Integer.toString(index)));
        }

        public boolean equals(Object o) {
            return (o instanceof List) && ((List<?>) o).isEmpty();
        }

        public int hashCode() {
            return 1;
        }

        public void sort(Comparator<? super E> c) {}
    }

    private static final class EmptySet<E> extends AbstractSet<E> implements java.io.Serializable {
        public Iterator<E> iterator() {
            return emptyIterator();
        }

        public int size() {
            return 0;
        }

        public boolean isEmpty() {
            return true;
        }

        public void clear() {}

        public boolean contains(Object obj) {
            return false;
        }

        public boolean containsAll(Collection<?> c) {
            return c.isEmpty();
        }

        public Object[] toArray() {
            return new Object[0];
        }

        public <T> T[] toArray(T[] a) {
            if (a.length > 0) a[0] = null;
            return a;
        }

        public int hashCode() {
            return 0;
        }
    }

    private static final class EmptyMap<K, V> extends AbstractMap<K, V> implements java.io.Serializable {
        public int size() {
            return 0;
        }

        public boolean isEmpty() {
            return true;
        }

        public void clear() {}

        public boolean containsKey(Object key) {
            return false;
        }

        public boolean containsValue(Object value) {
            return false;
        }

        public V get(Object key) {
            return null;
        }

        public Set<K> keySet() {
            return emptySet();
        }

        public Collection<V> values() {
            return emptySet();
        }

        public Set<Map.Entry<K, V>> entrySet() {
            return emptySet();
        }

        public boolean equals(Object o) {
            return (o instanceof Map) && ((Map<?, ?>) o).isEmpty();
        }

        public int hashCode() {
            return 0;
        }

        public V getOrDefault(Object k, V defaultValue) {
            return defaultValue;
        }
    }

    public static <T> List<T> singletonList(T o) {
        return new SingletonList<>(o);
    }

    public static <T> Set<T> singleton(T o) {
        return new SingletonSet<>(o);
    }

    public static <K, V> Map<K, V> singletonMap(K key, V value) {
        return new SingletonMap<>(key, value);
    }

    static <E> Iterator<E> singletonIterator(final E e) {
        return new Iterator<E>() {
            private boolean hasNext = true;

            public boolean hasNext() {
                return hasNext;
            }

            public E next() {
                if (hasNext) {
                    hasNext = false;
                    return e;
                }
                throw new NoSuchElementException();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static final class SingletonList<E> extends AbstractList<E> implements RandomAccess, java.io.Serializable {
        private final E element;

        SingletonList(E obj) {
            element = obj;
        }

        public Iterator<E> iterator() {
            return singletonIterator(element);
        }

        public int size() {
            return 1;
        }

        public boolean contains(Object obj) {
            return Objects.equals(obj, element);
        }

        public E get(int index) {
            if (index != 0) throw new IndexOutOfBoundsException("Index: ".concat(Integer.toString(index)).concat(", Size: 1"));
            return element;
        }

        public int hashCode() {
            return 31 + Objects.hashCode(element);
        }

        public void sort(Comparator<? super E> c) {}
    }

    private static final class SingletonSet<E> extends AbstractSet<E> implements java.io.Serializable {
        private final E element;

        SingletonSet(E e) {
            element = e;
        }

        public Iterator<E> iterator() {
            return singletonIterator(element);
        }

        public int size() {
            return 1;
        }

        public boolean contains(Object o) {
            return Objects.equals(o, element);
        }

        public int hashCode() {
            return Objects.hashCode(element);
        }
    }

    private static final class SingletonMap<K, V> extends AbstractMap<K, V> implements java.io.Serializable {
        private final K k;
        private final V v;

        SingletonMap(K key, V value) {
            k = key;
            v = value;
        }

        public int size() {
            return 1;
        }

        public boolean isEmpty() {
            return false;
        }

        public boolean containsKey(Object key) {
            return Objects.equals(key, k);
        }

        public boolean containsValue(Object value) {
            return Objects.equals(value, v);
        }

        public V get(Object key) {
            return Objects.equals(key, k) ? v : null;
        }

        public Set<K> keySet() {
            return singleton(k);
        }

        public Set<Map.Entry<K, V>> entrySet() {
            return Collections.<Map.Entry<K, V>>singleton(new AbstractMap.SimpleImmutableEntry<>(k, v));
        }

        public Collection<V> values() {
            return singleton(v);
        }

        public int hashCode() {
            return Objects.hashCode(k) ^ Objects.hashCode(v);
        }
    }

    // ---- unmodifiable views ----

    @SuppressWarnings("unchecked")
    public static <T> Collection<T> unmodifiableCollection(Collection<? extends T> c) {
        if (c instanceof UnmodifiableCollection && !(c instanceof UnmodifiableList) && !(c instanceof UnmodifiableSet)) return (Collection<T>) c;
        return new UnmodifiableCollection<>(c);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> unmodifiableList(List<? extends T> list) {
        if (list instanceof UnmodifiableList) return (List<T>) list;
        return list instanceof RandomAccess ? new UnmodifiableRandomAccessList<>(list) : new UnmodifiableList<>(list);
    }

    @SuppressWarnings("unchecked")
    public static <T> Set<T> unmodifiableSet(Set<? extends T> s) {
        if (s instanceof UnmodifiableSet) return (Set<T>) s;
        return new UnmodifiableSet<>(s);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> unmodifiableMap(Map<? extends K, ? extends V> m) {
        if (m instanceof UnmodifiableMap) return (Map<K, V>) m;
        return new UnmodifiableMap<>(m);
    }

    static class UnmodifiableCollection<E> implements Collection<E>, java.io.Serializable {
        final Collection<? extends E> c;

        UnmodifiableCollection(Collection<? extends E> c) {
            if (c == null) throw new NullPointerException();
            this.c = c;
        }

        public int size() {
            return c.size();
        }

        public boolean isEmpty() {
            return c.isEmpty();
        }

        public boolean contains(Object o) {
            return c.contains(o);
        }

        public Object[] toArray() {
            return c.toArray();
        }

        public <T> T[] toArray(T[] a) {
            return c.toArray(a);
        }

        public String toString() {
            return c.toString();
        }

        public Iterator<E> iterator() {
            return new Iterator<E>() {
                private final Iterator<? extends E> i = c.iterator();

                public boolean hasNext() {
                    return i.hasNext();
                }

                public E next() {
                    return i.next();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean containsAll(Collection<?> coll) {
            return c.containsAll(coll);
        }

        public boolean addAll(Collection<? extends E> coll) {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection<?> coll) {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(Collection<?> coll) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public boolean removeIf(java.util.function.Predicate<? super E> filter) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unchecked")
        public void forEach(java.util.function.Consumer<? super E> action) {
            ((Collection<E>) c).forEach(action);
        }

        @SuppressWarnings("unchecked")
        public Spliterator<E> spliterator() {
            return (Spliterator<E>) c.spliterator();
        }

        @SuppressWarnings("unchecked")
        public java.util.stream.Stream<E> stream() {
            return (java.util.stream.Stream<E>) c.stream();
        }

        @SuppressWarnings("unchecked")
        public java.util.stream.Stream<E> parallelStream() {
            return (java.util.stream.Stream<E>) c.parallelStream();
        }
    }

    static class UnmodifiableSet<E> extends UnmodifiableCollection<E> implements Set<E> {
        UnmodifiableSet(Set<? extends E> s) {
            super(s);
        }

        public boolean equals(Object o) {
            return o == this || c.equals(o);
        }

        public int hashCode() {
            return c.hashCode();
        }
    }

    static class UnmodifiableList<E> extends UnmodifiableCollection<E> implements List<E> {
        final List<? extends E> list;

        UnmodifiableList(List<? extends E> list) {
            super(list);
            this.list = list;
        }

        public boolean equals(Object o) {
            return o == this || list.equals(o);
        }

        public int hashCode() {
            return list.hashCode();
        }

        public E get(int index) {
            return list.get(index);
        }

        public E set(int index, E element) {
            throw new UnsupportedOperationException();
        }

        public void add(int index, E element) {
            throw new UnsupportedOperationException();
        }

        public E remove(int index) {
            throw new UnsupportedOperationException();
        }

        public int indexOf(Object o) {
            return list.indexOf(o);
        }

        public int lastIndexOf(Object o) {
            return list.lastIndexOf(o);
        }

        public boolean addAll(int index, Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        public void replaceAll(java.util.function.UnaryOperator<E> operator) {
            throw new UnsupportedOperationException();
        }

        public void sort(Comparator<? super E> c) {
            throw new UnsupportedOperationException();
        }

        public List<E> subList(int fromIndex, int toIndex) {
            return new UnmodifiableList<>(list.subList(fromIndex, toIndex));
        }
    }

    static class UnmodifiableRandomAccessList<E> extends UnmodifiableList<E> implements RandomAccess {
        UnmodifiableRandomAccessList(List<? extends E> list) {
            super(list);
        }

        public List<E> subList(int fromIndex, int toIndex) {
            return new UnmodifiableRandomAccessList<>(list.subList(fromIndex, toIndex));
        }
    }

    static class UnmodifiableMap<K, V> implements Map<K, V>, java.io.Serializable {
        private final Map<? extends K, ? extends V> m;

        UnmodifiableMap(Map<? extends K, ? extends V> m) {
            if (m == null) throw new NullPointerException();
            this.m = m;
        }

        public int size() {
            return m.size();
        }

        public boolean isEmpty() {
            return m.isEmpty();
        }

        public boolean containsKey(Object key) {
            return m.containsKey(key);
        }

        public boolean containsValue(Object val) {
            return m.containsValue(val);
        }

        public V get(Object key) {
            return m.get(key);
        }

        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        public void putAll(Map<? extends K, ? extends V> map) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public Set<K> keySet() {
            return unmodifiableSet(m.keySet());
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public Set<Map.Entry<K, V>> entrySet() {
            return new UnmodifiableEntrySet<>((Set) m.entrySet());
        }

        public Collection<V> values() {
            return unmodifiableCollection(m.values());
        }

        public boolean equals(Object o) {
            return o == this || m.equals(o);
        }

        public int hashCode() {
            return m.hashCode();
        }

        public String toString() {
            return m.toString();
        }

        @SuppressWarnings("unchecked")
        public V getOrDefault(Object k, V defaultValue) {
            return ((Map<K, V>) m).getOrDefault(k, defaultValue);
        }

        @SuppressWarnings("unchecked")
        public void forEach(java.util.function.BiConsumer<? super K, ? super V> action) {
            ((Map<K, V>) m).forEach(action);
        }

        public V putIfAbsent(K key, V value) {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        public boolean replace(K key, V oldValue, V newValue) {
            throw new UnsupportedOperationException();
        }

        public V replace(K key, V value) {
            throw new UnsupportedOperationException();
        }

        public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> f) {
            throw new UnsupportedOperationException();
        }

        public V computeIfPresent(K key, java.util.function.BiFunction<? super K, ? super V, ? extends V> f) {
            throw new UnsupportedOperationException();
        }

        public V compute(K key, java.util.function.BiFunction<? super K, ? super V, ? extends V> f) {
            throw new UnsupportedOperationException();
        }

        public V merge(K key, V value, java.util.function.BiFunction<? super V, ? super V, ? extends V> f) {
            throw new UnsupportedOperationException();
        }

        public void replaceAll(java.util.function.BiFunction<? super K, ? super V, ? extends V> f) {
            throw new UnsupportedOperationException();
        }
    }

    /** The entries of an unmodifiable map: a view whose entries refuse setValue. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static class UnmodifiableEntrySet<K, V> extends UnmodifiableSet<Map.Entry<K, V>> {
        UnmodifiableEntrySet(Set<? extends Map.Entry<? extends K, ? extends V>> s) {
            super((Set) s);
        }

        public Iterator<Map.Entry<K, V>> iterator() {
            return new Iterator<Map.Entry<K, V>>() {
                private final Iterator<? extends Map.Entry<K, V>> i = c.iterator();

                public boolean hasNext() {
                    return i.hasNext();
                }

                public Map.Entry<K, V> next() {
                    return new UnmodifiableEntry<>(i.next());
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public Object[] toArray() {
            Object[] a = c.toArray();
            for (int i = 0; i < a.length; i++) a[i] = new UnmodifiableEntry<>((Map.Entry<? extends K, ? extends V>) a[i]);
            return a;
        }

        public <T> T[] toArray(T[] a) {
            Object[] all = toArray();
            T[] r = a.length >= all.length ? a : (T[]) Arrays.newArrayLike(a, all.length);
            for (int i = 0; i < all.length; i++) r[i] = (T) all[i];
            if (r.length > all.length) r[all.length] = null;
            return r;
        }
    }

    private static class UnmodifiableEntry<K, V> implements Map.Entry<K, V> {
        private final Map.Entry<? extends K, ? extends V> e;

        UnmodifiableEntry(Map.Entry<? extends K, ? extends V> e) {
            this.e = Objects.requireNonNull(e);
        }

        public K getKey() {
            return e.getKey();
        }

        public V getValue() {
            return e.getValue();
        }

        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        public int hashCode() {
            return e.hashCode();
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof Map.Entry<?, ?> t && Objects.equals(e.getKey(), t.getKey()) && Objects.equals(e.getValue(), t.getValue());
        }

        public String toString() {
            return e.toString();
        }
    }

    // ---- algorithms ----

    public static <T extends Comparable<? super T>> void sort(List<T> list) {
        list.sort(null);
    }

    public static <T> void sort(List<T> list, Comparator<? super T> c) {
        list.sort(c);
    }

    public static void reverse(List<?> list) {
        int size = list.size();
        for (int i = 0, mid = size >> 1, j = size - 1; i < mid; i++, j--) swap(list, i, j);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void swap(List<?> list, int i, int j) {
        final List l = list;
        l.set(i, l.set(j, l.get(i)));
    }

    @SafeVarargs
    public static <T> boolean addAll(Collection<? super T> c, T... elements) {
        boolean result = false;
        for (T element : elements) result |= c.add(element);
        return result;
    }

    public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll) {
        Iterator<? extends T> i = coll.iterator();
        T candidate = i.next();
        while (i.hasNext()) {
            T next = i.next();
            if (next.compareTo(candidate) > 0) candidate = next;
        }
        return candidate;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T max(Collection<? extends T> coll, Comparator<? super T> comp) {
        if (comp == null) return (T) max((Collection) coll);
        Iterator<? extends T> i = coll.iterator();
        T candidate = i.next();
        while (i.hasNext()) {
            T next = i.next();
            if (comp.compare(next, candidate) > 0) candidate = next;
        }
        return candidate;
    }

    public static <T extends Object & Comparable<? super T>> T min(Collection<? extends T> coll) {
        Iterator<? extends T> i = coll.iterator();
        T candidate = i.next();
        while (i.hasNext()) {
            T next = i.next();
            if (next.compareTo(candidate) < 0) candidate = next;
        }
        return candidate;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T min(Collection<? extends T> coll, Comparator<? super T> comp) {
        if (comp == null) return (T) min((Collection) coll);
        Iterator<? extends T> i = coll.iterator();
        T candidate = i.next();
        while (i.hasNext()) {
            T next = i.next();
            if (comp.compare(next, candidate) < 0) candidate = next;
        }
        return candidate;
    }

    public static int frequency(Collection<?> c, Object o) {
        int result = 0;
        if (o == null) {
            for (Object e : c) if (e == null) result++;
        } else {
            for (Object e : c) if (o.equals(e)) result++;
        }
        return result;
    }

    public static <T> List<T> nCopies(int n, T o) {
        if (n < 0) throw new IllegalArgumentException("List length = ".concat(Integer.toString(n)));
        return new CopiesList<>(n, o);
    }

    private static final class CopiesList<E> extends AbstractList<E> implements RandomAccess, java.io.Serializable {
        final int n;
        final E element;

        CopiesList(int n, E e) {
            this.n = n;
            element = e;
        }

        public int size() {
            return n;
        }

        public boolean contains(Object obj) {
            return n != 0 && Objects.equals(obj, element);
        }

        public E get(int index) {
            Objects.checkIndex(index, n);
            return element;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> reverseOrder() {
        return (Comparator<T>) ReverseComparator.REVERSE_ORDER;
    }

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> reverseOrder(Comparator<T> cmp) {
        if (cmp == null) return (Comparator<T>) ReverseComparator.REVERSE_ORDER;
        if (cmp == ReverseComparator.REVERSE_ORDER) return (Comparator<T>) Comparators.NaturalOrderComparator.INSTANCE;
        if (cmp == Comparators.NaturalOrderComparator.INSTANCE) return (Comparator<T>) ReverseComparator.REVERSE_ORDER;
        if (cmp instanceof ReverseComparator2<T> r) return r.cmp;
        return new ReverseComparator2<>(cmp);
    }

    private static final class ReverseComparator implements Comparator<Comparable<Object>>, java.io.Serializable {
        static final ReverseComparator REVERSE_ORDER = new ReverseComparator();

        public int compare(Comparable<Object> c1, Comparable<Object> c2) {
            return c2.compareTo(c1);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public Comparator<Comparable<Object>> reversed() {
            return (Comparator) Comparator.naturalOrder();
        }
    }

    private static final class ReverseComparator2<T> implements Comparator<T>, java.io.Serializable {
        final Comparator<T> cmp;

        ReverseComparator2(Comparator<T> cmp) {
            this.cmp = cmp;
        }

        public int compare(T t1, T t2) {
            return cmp.compare(t2, t1);
        }

        public boolean equals(Object o) {
            return (o == this) || (o instanceof ReverseComparator2 && cmp.equals(((ReverseComparator2<?>) o).cmp));
        }

        public int hashCode() {
            return cmp.hashCode() ^ Integer.MIN_VALUE;
        }

        public Comparator<T> reversed() {
            return cmp;
        }
    }

    /**
     * Stream.toList's list, for java.util.stream (the JDK reaches it through its shared secrets). Not the JDK's API:
     * a mod, built against the JDK, can't call it.
     */
    public static <E> List<E> jListFromTrustedArrayNullsAllowed(Object[] input) {
        return ImmutableCollections.listFromTrustedArrayNullsAllowed(input);
    }
}
