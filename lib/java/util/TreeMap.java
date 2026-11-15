package java.util;

import java.util.function.BiConsumer;

/**
 * The model of java.util.TreeMap: a red-black tree as in Cormen, Leiserson and Rivest, the JDK's own source, so
 * that the same keys asked in the same order make the same tree and the comparator is asked about the same pairs.
 * The first key put is compared with itself (a null or incomparable key fails there, as in the JDK). The entries
 * firstEntry and its kin give are snapshots that can't be set. Not modelled: the views (headMap, tailMap, subMap,
 * descendingMap, descendingKeySet), and the balanced tree the JDK builds at once from a sorted map (put one by
 * one here: the same map, other comparisons).
 */
public class TreeMap<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V>, Cloneable, java.io.Serializable {
    private final Comparator<? super K> comparator;
    private transient Entry<K, V> root;
    private transient int size = 0;
    private transient int modCount = 0;

    private static final boolean RED = false;
    private static final boolean BLACK = true;

    public TreeMap() {
        comparator = null;
    }

    public TreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    public TreeMap(Map<? extends K, ? extends V> m) {
        comparator = null;
        putAll(m);
    }

    public TreeMap(SortedMap<K, ? extends V> m) {
        comparator = m.comparator();
        putAll(m);
    }

    public int size() {
        return size;
    }

    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    public boolean containsValue(Object value) {
        for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) if (Objects.equals(value, e.value)) return true;
        return false;
    }

    public V get(Object key) {
        Entry<K, V> p = getEntry(key);
        return p == null ? null : p.value;
    }

    public Comparator<? super K> comparator() {
        return comparator;
    }

    public K firstKey() {
        return key(getFirstEntry());
    }

    public K lastKey() {
        return key(getLastEntry());
    }

    @SuppressWarnings("unchecked")
    final Entry<K, V> getEntry(Object key) {
        if (comparator != null) return getEntryUsingComparator(key);
        Objects.requireNonNull(key);
        Comparable<? super K> k = (Comparable<? super K>) key;
        Entry<K, V> p = root;
        while (p != null) {
            int cmp = k.compareTo(p.key);
            if (cmp < 0) p = p.left;
            else if (cmp > 0) p = p.right;
            else return p;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    final Entry<K, V> getEntryUsingComparator(Object key) {
        K k = (K) key;
        Comparator<? super K> cpr = comparator;
        if (cpr != null) {
            Entry<K, V> p = root;
            while (p != null) {
                int cmp = cpr.compare(k, p.key);
                if (cmp < 0) p = p.left;
                else if (cmp > 0) p = p.right;
                else return p;
            }
        }
        return null;
    }

    /** The entry for the least key at or above key (ceiling), above it (higher), and so on; null when none. */
    final Entry<K, V> getCeilingEntry(K key) {
        Entry<K, V> p = root;
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp < 0) {
                if (p.left != null) p = p.left;
                else return p;
            } else if (cmp > 0) {
                if (p.right != null) {
                    p = p.right;
                } else {
                    Entry<K, V> parent = p.parent;
                    Entry<K, V> ch = p;
                    while (parent != null && ch == parent.right) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            } else {
                return p;
            }
        }
        return null;
    }

    final Entry<K, V> getFloorEntry(K key) {
        Entry<K, V> p = root;
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp > 0) {
                if (p.right != null) p = p.right;
                else return p;
            } else if (cmp < 0) {
                if (p.left != null) {
                    p = p.left;
                } else {
                    Entry<K, V> parent = p.parent;
                    Entry<K, V> ch = p;
                    while (parent != null && ch == parent.left) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            } else {
                return p;
            }
        }
        return null;
    }

    final Entry<K, V> getHigherEntry(K key) {
        Entry<K, V> p = root;
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp < 0) {
                if (p.left != null) p = p.left;
                else return p;
            } else {
                if (p.right != null) {
                    p = p.right;
                } else {
                    Entry<K, V> parent = p.parent;
                    Entry<K, V> ch = p;
                    while (parent != null && ch == parent.right) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            }
        }
        return null;
    }

    final Entry<K, V> getLowerEntry(K key) {
        Entry<K, V> p = root;
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp > 0) {
                if (p.right != null) p = p.right;
                else return p;
            } else {
                if (p.left != null) {
                    p = p.left;
                } else {
                    Entry<K, V> parent = p.parent;
                    Entry<K, V> ch = p;
                    while (parent != null && ch == parent.left) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            }
        }
        return null;
    }

    public V put(K key, V value) {
        return put(key, value, true);
    }

    public V putIfAbsent(K key, V value) {
        return put(key, value, false);
    }

    @SuppressWarnings("unchecked")
    private V put(K key, V value, boolean replaceOld) {
        Entry<K, V> t = root;
        if (t == null) {
            // Its type (and null) checked against itself.
            compare(key, key);
            root = new Entry<>(key, value, null);
            size = 1;
            modCount++;
            return null;
        }
        int cmp;
        Entry<K, V> parent;
        Comparator<? super K> cpr = comparator;
        if (cpr != null) {
            do {
                parent = t;
                cmp = cpr.compare(key, t.key);
                if (cmp < 0) t = t.left;
                else if (cmp > 0) t = t.right;
                else return setOld(t, value, replaceOld);
            } while (t != null);
        } else {
            Objects.requireNonNull(key);
            Comparable<? super K> k = (Comparable<? super K>) key;
            do {
                parent = t;
                cmp = k.compareTo(t.key);
                if (cmp < 0) t = t.left;
                else if (cmp > 0) t = t.right;
                else return setOld(t, value, replaceOld);
            } while (t != null);
        }
        Entry<K, V> e = new Entry<>(key, value, parent);
        if (cmp < 0) parent.left = e;
        else parent.right = e;
        fixAfterInsertion(e);
        size++;
        modCount++;
        return null;
    }

    private static <K, V> V setOld(Entry<K, V> t, V value, boolean replaceOld) {
        V oldValue = t.value;
        if (replaceOld || oldValue == null) t.value = value;
        return oldValue;
    }

    public V remove(Object key) {
        Entry<K, V> p = getEntry(key);
        if (p == null) return null;
        V oldValue = p.value;
        deleteEntry(p);
        return oldValue;
    }

    public void clear() {
        modCount++;
        size = 0;
        root = null;
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        int expectedModCount = modCount;
        for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
            action.accept(e.key, e.value);
            if (expectedModCount != modCount) throw new ConcurrentModificationException();
        }
    }

    public Map.Entry<K, V> firstEntry() {
        return exportEntry(getFirstEntry());
    }

    public Map.Entry<K, V> lastEntry() {
        return exportEntry(getLastEntry());
    }

    public Map.Entry<K, V> pollFirstEntry() {
        Entry<K, V> p = getFirstEntry();
        Map.Entry<K, V> result = exportEntry(p);
        if (p != null) deleteEntry(p);
        return result;
    }

    public Map.Entry<K, V> pollLastEntry() {
        Entry<K, V> p = getLastEntry();
        Map.Entry<K, V> result = exportEntry(p);
        if (p != null) deleteEntry(p);
        return result;
    }

    public Map.Entry<K, V> lowerEntry(K key) {
        return exportEntry(getLowerEntry(key));
    }

    public K lowerKey(K key) {
        return keyOrNull(getLowerEntry(key));
    }

    public Map.Entry<K, V> floorEntry(K key) {
        return exportEntry(getFloorEntry(key));
    }

    public K floorKey(K key) {
        return keyOrNull(getFloorEntry(key));
    }

    public Map.Entry<K, V> ceilingEntry(K key) {
        return exportEntry(getCeilingEntry(key));
    }

    public K ceilingKey(K key) {
        return keyOrNull(getCeilingEntry(key));
    }

    public Map.Entry<K, V> higherEntry(K key) {
        return exportEntry(getHigherEntry(key));
    }

    public K higherKey(K key) {
        return keyOrNull(getHigherEntry(key));
    }

    public Set<K> keySet() {
        return new AbstractSet<K>() {
            public Iterator<K> iterator() {
                return new PrivateIterator<K>(0);
            }

            public int size() {
                return TreeMap.this.size();
            }

            public boolean contains(Object o) {
                return containsKey(o);
            }

            public boolean remove(Object o) {
                Entry<K, V> e = getEntry(o);
                if (e == null) return false;
                deleteEntry(e);
                return true;
            }

            public void clear() {
                TreeMap.this.clear();
            }
        };
    }

    public Collection<V> values() {
        return new AbstractCollection<V>() {
            public Iterator<V> iterator() {
                return new PrivateIterator<V>(1);
            }

            public int size() {
                return TreeMap.this.size();
            }

            public boolean contains(Object o) {
                return containsValue(o);
            }

            public void clear() {
                TreeMap.this.clear();
            }
        };
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>() {
            public Iterator<Map.Entry<K, V>> iterator() {
                return new PrivateIterator<Map.Entry<K, V>>(2);
            }

            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry<?, ?> entry)) return false;
                Entry<K, V> p = getEntry(entry.getKey());
                return p != null && Objects.equals(p.getValue(), entry.getValue());
            }

            public boolean remove(Object o) {
                if (!(o instanceof Map.Entry<?, ?> entry)) return false;
                Entry<K, V> p = getEntry(entry.getKey());
                if (p != null && Objects.equals(p.getValue(), entry.getValue())) {
                    deleteEntry(p);
                    return true;
                }
                return false;
            }

            public int size() {
                return TreeMap.this.size();
            }

            public void clear() {
                TreeMap.this.clear();
            }
        };
    }

    /** In key order: keys (0), values (1) or entries (2); fail-fast. */
    final class PrivateIterator<T> implements Iterator<T> {
        private final int kind;
        Entry<K, V> next;
        Entry<K, V> lastReturned;
        int expectedModCount;

        PrivateIterator(int kind) {
            this.kind = kind;
            expectedModCount = modCount;
            lastReturned = null;
            next = getFirstEntry();
        }

        public boolean hasNext() {
            return next != null;
        }

        @SuppressWarnings("unchecked")
        public T next() {
            Entry<K, V> e = next;
            if (e == null) throw new NoSuchElementException();
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
            next = successor(e);
            lastReturned = e;
            return kind == 0 ? (T) e.key : kind == 1 ? (T) e.value : (T) e;
        }

        public void remove() {
            if (lastReturned == null) throw new IllegalStateException();
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
            // Deleting an entry with two children moves its successor's key and value into it.
            if (lastReturned.left != null && lastReturned.right != null) next = lastReturned;
            deleteEntry(lastReturned);
            expectedModCount = modCount;
            lastReturned = null;
        }
    }

    @SuppressWarnings("unchecked")
    final int compare(Object k1, Object k2) {
        return comparator == null ? ((Comparable<? super K>) k1).compareTo((K) k2) : comparator.compare((K) k1, (K) k2);
    }

    static <K, V> Map.Entry<K, V> exportEntry(TreeMap.Entry<K, V> e) {
        return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<>(e);
    }

    static <K, V> K keyOrNull(TreeMap.Entry<K, V> e) {
        return (e == null) ? null : e.key;
    }

    static <K> K key(Entry<K, ?> e) {
        if (e == null) throw new NoSuchElementException();
        return e.key;
    }

    static final class Entry<K, V> implements Map.Entry<K, V> {
        K key;
        V value;
        Entry<K, V> left;
        Entry<K, V> right;
        Entry<K, V> parent;
        boolean color = BLACK;

        Entry(K key, V value, Entry<K, V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public boolean equals(Object o) {
            return o instanceof Map.Entry<?, ?> e && Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue());
        }

        public int hashCode() {
            int keyHash = (key == null ? 0 : key.hashCode());
            int valueHash = (value == null ? 0 : value.hashCode());
            return keyHash ^ valueHash;
        }

        public String toString() {
            return String.valueOf(key).concat("=").concat(String.valueOf(value));
        }
    }

    final Entry<K, V> getFirstEntry() {
        Entry<K, V> p = root;
        if (p != null) while (p.left != null) p = p.left;
        return p;
    }

    final Entry<K, V> getLastEntry() {
        Entry<K, V> p = root;
        if (p != null) while (p.right != null) p = p.right;
        return p;
    }

    static <K, V> TreeMap.Entry<K, V> successor(Entry<K, V> t) {
        if (t == null) return null;
        if (t.right != null) {
            Entry<K, V> p = t.right;
            while (p.left != null) p = p.left;
            return p;
        }
        Entry<K, V> p = t.parent;
        Entry<K, V> ch = t;
        while (p != null && ch == p.right) {
            ch = p;
            p = p.parent;
        }
        return p;
    }

    static <K, V> Entry<K, V> predecessor(Entry<K, V> t) {
        if (t == null) return null;
        if (t.left != null) {
            Entry<K, V> p = t.left;
            while (p.right != null) p = p.right;
            return p;
        }
        Entry<K, V> p = t.parent;
        Entry<K, V> ch = t;
        while (p != null && ch == p.left) {
            ch = p;
            p = p.parent;
        }
        return p;
    }

    // ---- balancing (CLR's, the nil leaves as null) ----

    private static <K, V> boolean colorOf(Entry<K, V> p) {
        return (p == null ? BLACK : p.color);
    }

    private static <K, V> Entry<K, V> parentOf(Entry<K, V> p) {
        return (p == null ? null : p.parent);
    }

    private static <K, V> void setColor(Entry<K, V> p, boolean c) {
        if (p != null) p.color = c;
    }

    private static <K, V> Entry<K, V> leftOf(Entry<K, V> p) {
        return (p == null) ? null : p.left;
    }

    private static <K, V> Entry<K, V> rightOf(Entry<K, V> p) {
        return (p == null) ? null : p.right;
    }

    private void rotateLeft(Entry<K, V> p) {
        if (p != null) {
            Entry<K, V> r = p.right;
            p.right = r.left;
            if (r.left != null) r.left.parent = p;
            r.parent = p.parent;
            if (p.parent == null) root = r;
            else if (p.parent.left == p) p.parent.left = r;
            else p.parent.right = r;
            r.left = p;
            p.parent = r;
        }
    }

    private void rotateRight(Entry<K, V> p) {
        if (p != null) {
            Entry<K, V> l = p.left;
            p.left = l.right;
            if (l.right != null) l.right.parent = p;
            l.parent = p.parent;
            if (p.parent == null) root = l;
            else if (p.parent.right == p) p.parent.right = l;
            else p.parent.left = l;
            l.right = p;
            p.parent = l;
        }
    }

    private void fixAfterInsertion(Entry<K, V> x) {
        x.color = RED;
        while (x != null && x != root && x.parent.color == RED) {
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                Entry<K, V> y = rightOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateLeft(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateRight(parentOf(parentOf(x)));
                }
            } else {
                Entry<K, V> y = leftOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.color = BLACK;
    }

    /** Deletes p: one with two children takes its successor's key and value, and the successor goes instead. */
    private void deleteEntry(Entry<K, V> p) {
        modCount++;
        size--;
        if (p.left != null && p.right != null) {
            Entry<K, V> s = successor(p);
            p.key = s.key;
            p.value = s.value;
            p = s;
        }
        Entry<K, V> replacement = (p.left != null ? p.left : p.right);
        if (replacement != null) {
            replacement.parent = p.parent;
            if (p.parent == null) root = replacement;
            else if (p == p.parent.left) p.parent.left = replacement;
            else p.parent.right = replacement;
            p.left = p.right = p.parent = null;
            if (p.color == BLACK) fixAfterDeletion(replacement);
        } else if (p.parent == null) {
            root = null;
        } else {
            if (p.color == BLACK) fixAfterDeletion(p);
            if (p.parent != null) {
                if (p == p.parent.left) p.parent.left = null;
                else if (p == p.parent.right) p.parent.right = null;
                p.parent = null;
            }
        }
    }

    private void fixAfterDeletion(Entry<K, V> x) {
        while (x != root && colorOf(x) == BLACK) {
            if (x == leftOf(parentOf(x))) {
                Entry<K, V> sib = rightOf(parentOf(x));
                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateLeft(parentOf(x));
                    sib = rightOf(parentOf(x));
                }
                if (colorOf(leftOf(sib)) == BLACK && colorOf(rightOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(rightOf(sib)) == BLACK) {
                        setColor(leftOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateRight(sib);
                        sib = rightOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(rightOf(sib), BLACK);
                    rotateLeft(parentOf(x));
                    x = root;
                }
            } else {
                Entry<K, V> sib = leftOf(parentOf(x));
                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateRight(parentOf(x));
                    sib = leftOf(parentOf(x));
                }
                if (colorOf(rightOf(sib)) == BLACK && colorOf(leftOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(leftOf(sib)) == BLACK) {
                        setColor(rightOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateLeft(sib);
                        sib = leftOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(leftOf(sib), BLACK);
                    rotateRight(parentOf(x));
                    x = root;
                }
            }
        }
        setColor(x, BLACK);
    }
}
