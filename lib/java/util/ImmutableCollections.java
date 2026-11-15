package java.util;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * The model of what List.of, Set.of and Map.of give: no null in them, nothing changes them (UnsupportedOperationException),
 * and asking them about null throws NullPointerException, as the JDK's do. Their classes have the JDK's names. Sets and
 * maps iterate in the order their elements were given (the JDK's order is salted at random, run to run).
 */
final class ImmutableCollections {
    private ImmutableCollections() {}

    static final Object EMPTY = new Object();

    static final ListN<?> EMPTY_LIST = new ListN<>(new Object[0]);
    static final SetN<?> EMPTY_SET = new SetN<>(new Object[0]);
    static final MapN<?, ?> EMPTY_MAP = new MapN<>(new Object[0]);

    static UnsupportedOperationException uoe() {
        return new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    static <E> List<E> listOf(Object[] input) {
        switch (input.length) {
            case 0:
                return (List<E>) EMPTY_LIST;
            case 1:
                return new List12<>((E) input[0]);
            case 2:
                return new List12<>((E) input[0], (E) input[1]);
            default:
                Object[] copy = new Object[input.length];
                for (int i = 0; i < input.length; i++) copy[i] = Objects.requireNonNull(input[i]);
                return new ListN<>(copy);
        }
    }

    @SuppressWarnings("unchecked")
    static <E> List<E> listCopy(Collection<? extends E> coll) {
        if (coll instanceof List12 || coll instanceof ListN) return (List<E>) coll;
        return listOf(coll.toArray());
    }

    @SuppressWarnings("unchecked")
    static <E> Set<E> setOf(Object[] input) {
        switch (input.length) {
            case 0:
                return (Set<E>) EMPTY_SET;
            case 1:
                return new Set12<>((E) input[0]);
            case 2:
                return new Set12<>((E) input[0], (E) input[1]);
            default:
                Object[] elements = new Object[input.length];
                for (int i = 0; i < input.length; i++) {
                    Object e = Objects.requireNonNull(input[i]);
                    for (int j = 0; j < i; j++) {
                        if (e.equals(elements[j])) throw new IllegalArgumentException("duplicate element: ".concat(String.valueOf(e)));
                    }
                    elements[i] = e;
                }
                return new SetN<>(elements);
        }
    }

    static <K, V> Map<K, V> mapOf(Object[] input) {
        int n = input.length >> 1;
        Object[] table = new Object[input.length];
        for (int i = 0; i < n; i++) {
            Object k = Objects.requireNonNull(input[i << 1]);
            Object v = Objects.requireNonNull(input[(i << 1) + 1]);
            for (int j = 0; j < i; j++) {
                if (k.equals(table[j << 1])) throw new IllegalArgumentException("duplicate key: ".concat(String.valueOf(k)));
            }
            table[i << 1] = k;
            table[(i << 1) + 1] = v;
        }
        return new MapN<>(table);
    }

    abstract static class AbstractImmutableCollection<E> extends AbstractCollection<E> {
        public boolean add(E e) {
            throw uoe();
        }

        public boolean addAll(Collection<? extends E> c) {
            throw uoe();
        }

        public void clear() {
            throw uoe();
        }

        public boolean remove(Object o) {
            throw uoe();
        }

        public boolean removeAll(Collection<?> c) {
            throw uoe();
        }

        public boolean removeIf(Predicate<? super E> filter) {
            throw uoe();
        }

        public boolean retainAll(Collection<?> c) {
            throw uoe();
        }
    }

    abstract static class AbstractImmutableList<E> extends AbstractImmutableCollection<E> implements List<E>, RandomAccess {
        public void add(int index, E element) {
            throw uoe();
        }

        public boolean addAll(int index, Collection<? extends E> c) {
            throw uoe();
        }

        public E remove(int index) {
            throw uoe();
        }

        public void replaceAll(UnaryOperator<E> operator) {
            throw uoe();
        }

        public E set(int index, E element) {
            throw uoe();
        }

        public void sort(Comparator<? super E> c) {
            throw uoe();
        }

        /** A copy, where the JDK's is a view: the same, since neither changes. */
        public List<E> subList(int fromIndex, int toIndex) {
            int size = size();
            AbstractList.subListRangeCheck(fromIndex, toIndex, size);
            Object[] part = new Object[toIndex - fromIndex];
            for (int i = 0; i < part.length; i++) part[i] = get(fromIndex + i);
            return new ListN<>(part);
        }

        public Iterator<E> iterator() {
            return new ListItr<>(this, size());
        }

        public boolean contains(Object o) {
            return indexOf(o) >= 0;
        }

        public int indexOf(Object o) {
            Objects.requireNonNull(o);
            for (int i = 0, s = size(); i < s; i++) if (o.equals(get(i))) return i;
            return -1;
        }

        public int lastIndexOf(Object o) {
            Objects.requireNonNull(o);
            for (int i = size() - 1; i >= 0; i--) if (o.equals(get(i))) return i;
            return -1;
        }

        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof List)) return false;
            Iterator<?> oit = ((List<?>) o).iterator();
            for (int i = 0, s = size(); i < s; i++) {
                if (!oit.hasNext() || !get(i).equals(oit.next())) return false;
            }
            return !oit.hasNext();
        }

        public int hashCode() {
            int hash = 1;
            for (int i = 0, s = size(); i < s; i++) hash = 31 * hash + get(i).hashCode();
            return hash;
        }

        IndexOutOfBoundsException outOfBounds(int index) {
            return new IndexOutOfBoundsException("Index: ".concat(Integer.toString(index)).concat(" Size: ").concat(Integer.toString(size())));
        }
    }

    static final class ListItr<E> implements Iterator<E> {
        private final List<E> list;
        private final int size;
        private int cursor;

        ListItr(List<E> list, int size) {
            this.list = list;
            this.size = size;
        }

        public boolean hasNext() {
            return cursor != size;
        }

        public E next() {
            if (cursor >= size) throw new NoSuchElementException();
            return list.get(cursor++);
        }

        public void remove() {
            throw uoe();
        }
    }

    static final class List12<E> extends AbstractImmutableList<E> implements java.io.Serializable {
        private final E e0;
        private final Object e1;

        List12(E e0) {
            this.e0 = Objects.requireNonNull(e0);
            this.e1 = EMPTY;
        }

        List12(E e0, E e1) {
            this.e0 = Objects.requireNonNull(e0);
            this.e1 = Objects.requireNonNull(e1);
        }

        public int size() {
            return e1 != EMPTY ? 2 : 1;
        }

        public boolean isEmpty() {
            return false;
        }

        @SuppressWarnings("unchecked")
        public E get(int index) {
            if (index == 0) return e0;
            if (index == 1 && e1 != EMPTY) return (E) e1;
            throw outOfBounds(index);
        }

        @SuppressWarnings("unchecked")
        public E getFirst() {
            return e0;
        }

        @SuppressWarnings("unchecked")
        public E getLast() {
            return e1 == EMPTY ? e0 : (E) e1;
        }

        public Object[] toArray() {
            return e1 == EMPTY ? new Object[] {e0} : new Object[] {e0, e1};
        }
    }

    static final class ListN<E> extends AbstractImmutableList<E> implements java.io.Serializable {
        private final Object[] elements;

        ListN(Object[] elements) {
            this.elements = elements;
        }

        public int size() {
            return elements.length;
        }

        public boolean isEmpty() {
            return elements.length == 0;
        }

        @SuppressWarnings("unchecked")
        public E get(int index) {
            return (E) elements[index];
        }

        public Object[] toArray() {
            return Arrays.copyOfObjects(elements, elements.length);
        }
    }

    abstract static class AbstractImmutableSet<E> extends AbstractImmutableCollection<E> implements Set<E> {
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Set<?> c)) return false;
            if (c.size() != size()) return false;
            for (Object e : c) {
                if (e == null || !contains(e)) return false;
            }
            return true;
        }

        public abstract int hashCode();
    }

    static final class Set12<E> extends AbstractImmutableSet<E> implements java.io.Serializable {
        private final E e0;
        private final Object e1;

        Set12(E e0) {
            this.e0 = Objects.requireNonNull(e0);
            this.e1 = EMPTY;
        }

        Set12(E e0, E e1) {
            if (e0.equals(Objects.requireNonNull(e1))) throw new IllegalArgumentException("duplicate element: ".concat(String.valueOf(e0)));
            this.e0 = e0;
            this.e1 = e1;
        }

        public int size() {
            return (e1 == EMPTY) ? 1 : 2;
        }

        public boolean isEmpty() {
            return false;
        }

        public boolean contains(Object o) {
            return o.equals(e0) || e1.equals(o);
        }

        public int hashCode() {
            return e0.hashCode() + (e1 == EMPTY ? 0 : e1.hashCode());
        }

        public Iterator<E> iterator() {
            return new Iterator<E>() {
                private int idx = size();

                public boolean hasNext() {
                    return idx > 0;
                }

                @SuppressWarnings("unchecked")
                public E next() {
                    if (idx == 0) throw new NoSuchElementException();
                    int at = size() - idx--;
                    return at == 0 ? e0 : (E) e1;
                }

                public void remove() {
                    throw uoe();
                }
            };
        }
    }

    static final class SetN<E> extends AbstractImmutableSet<E> implements java.io.Serializable {
        private final Object[] elements;

        SetN(Object[] elements) {
            this.elements = elements;
        }

        public int size() {
            return elements.length;
        }

        public boolean isEmpty() {
            return elements.length == 0;
        }

        public boolean contains(Object o) {
            Objects.requireNonNull(o);
            for (Object e : elements) if (o.equals(e)) return true;
            return false;
        }

        public int hashCode() {
            int h = 0;
            for (Object e : elements) h += e.hashCode();
            return h;
        }

        public Iterator<E> iterator() {
            return new Iterator<E>() {
                private int i = 0;

                public boolean hasNext() {
                    return i < elements.length;
                }

                @SuppressWarnings("unchecked")
                public E next() {
                    if (i >= elements.length) throw new NoSuchElementException();
                    return (E) elements[i++];
                }

                public void remove() {
                    throw uoe();
                }
            };
        }
    }

    abstract static class AbstractImmutableMap<K, V> extends AbstractMap<K, V> {
        public void clear() {
            throw uoe();
        }

        public V compute(K key, BiFunction<? super K, ? super V, ? extends V> rf) {
            throw uoe();
        }

        public V computeIfAbsent(K key, Function<? super K, ? extends V> mf) {
            throw uoe();
        }

        public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> rf) {
            throw uoe();
        }

        public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> rf) {
            throw uoe();
        }

        public V put(K key, V value) {
            throw uoe();
        }

        public void putAll(Map<? extends K, ? extends V> m) {
            throw uoe();
        }

        public V putIfAbsent(K key, V value) {
            throw uoe();
        }

        public V remove(Object key) {
            throw uoe();
        }

        public boolean remove(Object key, Object value) {
            throw uoe();
        }

        public V replace(K key, V value) {
            throw uoe();
        }

        public boolean replace(K key, V oldValue, V newValue) {
            throw uoe();
        }

        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> f) {
            throw uoe();
        }

        public V getOrDefault(Object key, V defaultValue) {
            V v;
            return ((v = get(key)) != null) ? v : defaultValue;
        }
    }

    static final class Map1<K, V> extends AbstractImmutableMap<K, V> implements java.io.Serializable {
        private final K k0;
        private final V v0;

        Map1(K k0, V v0) {
            this.k0 = Objects.requireNonNull(k0);
            this.v0 = Objects.requireNonNull(v0);
        }

        public Set<Map.Entry<K, V>> entrySet() {
            return new Set12<Map.Entry<K, V>>(new KeyValueHolder<>(k0, v0));
        }

        public V get(Object o) {
            return o.equals(k0) ? v0 : null;
        }

        public boolean containsKey(Object o) {
            return o.equals(k0);
        }

        public boolean containsValue(Object o) {
            return o.equals(v0);
        }

        public int size() {
            return 1;
        }

        public boolean isEmpty() {
            return false;
        }

        public int hashCode() {
            return k0.hashCode() ^ v0.hashCode();
        }
    }

    /** Its keys and values one after the other, in the order given. */
    static final class MapN<K, V> extends AbstractImmutableMap<K, V> implements java.io.Serializable {
        private final Object[] table;
        private final int size;

        MapN(Object[] table) {
            this.table = table;
            this.size = table.length >> 1;
        }

        private int find(Object k) {
            Objects.requireNonNull(k);
            for (int i = 0; i < size; i++) if (k.equals(table[i << 1])) return i;
            return -1;
        }

        public boolean containsKey(Object o) {
            return find(o) >= 0;
        }

        public boolean containsValue(Object o) {
            Objects.requireNonNull(o);
            for (int i = 0; i < size; i++) if (o.equals(table[(i << 1) + 1])) return true;
            return false;
        }

        public int hashCode() {
            int hash = 0;
            for (int i = 0; i < size; i++) hash += table[i << 1].hashCode() ^ table[(i << 1) + 1].hashCode();
            return hash;
        }

        @SuppressWarnings("unchecked")
        public V get(Object o) {
            int i = find(o);
            return i >= 0 ? (V) table[(i << 1) + 1] : null;
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        @SuppressWarnings("unchecked")
        public Set<Map.Entry<K, V>> entrySet() {
            Object[] entries = new Object[size];
            for (int i = 0; i < size; i++) entries[i] = new KeyValueHolder<>((K) table[i << 1], (V) table[(i << 1) + 1]);
            return new SetN<>(entries);
        }
    }
}
