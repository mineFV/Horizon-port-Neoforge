package java.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The model of java.util.HashMap, written from how the JDK's behaves, so that it iterates in the JDK's order: a
 * table of a power of two, made on the first put; a key's bin is its hash, spread (h ^ h >>> 16), masked by the
 * table's size; a bin is a list, a new key at its end (at its start for computeIfAbsent, compute and merge, as the
 * JDK's); the table doubles past its load factor, each bin split in two keeping its order. One difference, said in
 * lib/README.md: a bin of eight keys or more in a table of 64 or more is a tree in the JDK, iterated in another
 * order; here it stays a list. clone isn't modelled.
 */
public class HashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, java.io.Serializable {
    static final int DEFAULT_INITIAL_CAPACITY = 16;
    static final int MAXIMUM_CAPACITY = 1 << 30;
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    /** A bin this long is made a tree by the JDK, or the table grows when it is smaller than 64. */
    static final int TREEIFY_THRESHOLD = 8;
    static final int MIN_TREEIFY_CAPACITY = 64;

    static class Node<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        V value;
        Node<K, V> next;

        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey() {
            return key;
        }

        public final V getValue() {
            return value;
        }

        public final String toString() {
            return String.valueOf(key).concat("=").concat(String.valueOf(value));
        }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (o == this) return true;
            return o instanceof Map.Entry<?, ?> e && Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue());
        }
    }

    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /** The smallest power of two at least cap (1 for 0), up to MAXIMUM_CAPACITY. */
    static final int tableSizeFor(int cap) {
        int n = 1;
        while (n < cap && n < MAXIMUM_CAPACITY) n <<= 1;
        return n;
    }

    transient Node<K, V>[] table;
    transient Set<Map.Entry<K, V>> entrySet;
    transient int size;
    transient int modCount;
    int threshold;
    final float loadFactor;

    public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) throw new IllegalArgumentException("Illegal initial capacity: ".concat(Integer.toString(initialCapacity)));
        if (initialCapacity > MAXIMUM_CAPACITY) initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) throw new IllegalArgumentException("Illegal load factor: ".concat(Float.toString(loadFactor)));
        this.loadFactor = loadFactor;
        this.threshold = tableSizeFor(initialCapacity);
    }

    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
    }

    public HashMap(Map<? extends K, ? extends V> m) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(m, false);
    }

    static int calculateHashMapCapacity(int numMappings) {
        return (int) Math.ceil(numMappings / (double) DEFAULT_LOAD_FACTOR);
    }

    public static <K, V> HashMap<K, V> newHashMap(int numMappings) {
        if (numMappings < 0) throw new IllegalArgumentException("Negative number of mappings: ".concat(Integer.toString(numMappings)));
        return new HashMap<>(calculateHashMapCapacity(numMappings));
    }

    final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
        int s = m.size();
        if (s > 0) {
            if (table == null) {
                double dt = Math.ceil(s / (double) loadFactor);
                int t = ((dt < (double) MAXIMUM_CAPACITY) ? (int) dt : MAXIMUM_CAPACITY);
                if (t > threshold) threshold = tableSizeFor(t);
            } else {
                while (s > threshold && table.length < MAXIMUM_CAPACITY) resize();
            }
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                putVal(hash(key), key, value, false, evict);
            }
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public V get(Object key) {
        Node<K, V> e;
        return (e = getNode(key)) == null ? null : e.value;
    }

    final Node<K, V> getNode(Object key) {
        Node<K, V>[] tab = table;
        if (tab == null || tab.length == 0) return null;
        int hash = hash(key);
        for (Node<K, V> e = tab[(tab.length - 1) & hash]; e != null; e = e.next) {
            K k;
            if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) return e;
        }
        return null;
    }

    public boolean containsKey(Object key) {
        return getNode(key) != null;
    }

    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
        Node<K, V>[] tab;
        Node<K, V> p;
        int n, i;
        if ((tab = table) == null || (n = tab.length) == 0) n = (tab = resize()).length;
        if ((p = tab[i = (n - 1) & hash]) == null) {
            tab[i] = newNode(hash, key, value, null);
        } else {
            Node<K, V> e;
            K k;
            if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k)))) {
                e = p;
            } else {
                for (int binCount = 0;; ++binCount) {
                    if ((e = p.next) == null) {
                        p.next = newNode(hash, key, value, null);
                        if (binCount >= TREEIFY_THRESHOLD - 1) treeifyBin(tab);
                        break;
                    }
                    if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) break;
                    p = e;
                }
            }
            if (e != null) {
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null) e.value = value;
                afterNodeAccess(e);
                return oldValue;
            }
        }
        ++modCount;
        if (++size > threshold) resize();
        afterNodeInsertion(evict);
        return null;
    }

    /** Where the JDK makes a long bin a tree: the table grows when it is smaller than 64; a tree isn't modelled. */
    final void treeifyBin(Node<K, V>[] tab) {
        if (tab == null || tab.length < MIN_TREEIFY_CAPACITY) resize();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    final Node<K, V>[] resize() {
        Node<K, V>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        int oldThr = threshold;
        int newCap = 0;
        int newThr = 0;
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            newCap = oldCap << 1;
            if (newCap < MAXIMUM_CAPACITY && oldCap >= DEFAULT_INITIAL_CAPACITY) newThr = oldThr << 1;
        } else if (oldThr > 0) {
            newCap = oldThr;
        } else {
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        if (newThr == 0) {
            float ft = (float) newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float) MAXIMUM_CAPACITY ? (int) ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
        table = newTab;
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
                Node<K, V> e = oldTab[j];
                if (e == null) continue;
                oldTab[j] = null;
                Node<K, V> loHead = null, loTail = null, hiHead = null, hiTail = null;
                for (; e != null; e = e.next) {
                    if ((e.hash & oldCap) == 0) {
                        if (loTail == null) loHead = e;
                        else loTail.next = e;
                        loTail = e;
                    } else {
                        if (hiTail == null) hiHead = e;
                        else hiTail.next = e;
                        hiTail = e;
                    }
                }
                if (loTail != null) {
                    loTail.next = null;
                    newTab[j] = loHead;
                }
                if (hiTail != null) {
                    hiTail.next = null;
                    newTab[j + oldCap] = hiHead;
                }
            }
        }
        return newTab;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        putMapEntries(m, true);
    }

    public V remove(Object key) {
        Node<K, V> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ? null : e.value;
    }

    final Node<K, V> removeNode(int hash, Object key, Object value, boolean matchValue, boolean movable) {
        Node<K, V>[] tab = table;
        if (tab == null || tab.length == 0) return null;
        int index = (tab.length - 1) & hash;
        Node<K, V> prev = null;
        for (Node<K, V> node = tab[index]; node != null; prev = node, node = node.next) {
            K k;
            if (node.hash == hash && ((k = node.key) == key || (key != null && key.equals(k)))) {
                V v;
                if (matchValue && !((v = node.value) == value || (value != null && value.equals(v)))) return null;
                if (prev == null) tab[index] = node.next;
                else prev.next = node.next;
                ++modCount;
                --size;
                afterNodeRemoval(node);
                return node;
            }
        }
        return null;
    }

    public void clear() {
        Node<K, V>[] tab;
        modCount++;
        if ((tab = table) != null && size > 0) {
            size = 0;
            for (int i = 0; i < tab.length; ++i) tab[i] = null;
        }
    }

    public boolean containsValue(Object value) {
        Node<K, V>[] tab = table;
        if (tab != null && size > 0) {
            for (Node<K, V> e : tab) {
                for (; e != null; e = e.next) {
                    V v;
                    if ((v = e.value) == value || (value != null && value.equals(v))) return true;
                }
            }
        }
        return false;
    }

    public Set<K> keySet() {
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new KeySet();
            keySet = ks;
        }
        return ks;
    }

    final class KeySet extends AbstractSet<K> {
        public final int size() {
            return size;
        }

        public final void clear() {
            HashMap.this.clear();
        }

        public final Iterator<K> iterator() {
            return newKeyIterator();
        }

        public final boolean contains(Object o) {
            return containsKey(o);
        }

        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }
    }

    public Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            vs = new Values();
            values = vs;
        }
        return vs;
    }

    final class Values extends AbstractCollection<V> {
        public final int size() {
            return size;
        }

        public final void clear() {
            HashMap.this.clear();
        }

        public final Iterator<V> iterator() {
            return newValueIterator();
        }

        public final boolean contains(Object o) {
            return containsValue(o);
        }
    }

    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> es;
        return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
    }

    final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        public final int size() {
            return size;
        }

        public final void clear() {
            HashMap.this.clear();
        }

        public final Iterator<Map.Entry<K, V>> iterator() {
            return newEntryIterator();
        }

        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) return false;
            Object key = e.getKey();
            Node<K, V> candidate = getNode(key);
            return candidate != null && candidate.equals(e);
        }

        public final boolean remove(Object o) {
            if (o instanceof Map.Entry<?, ?> e) {
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }
    }

    // ---- what LinkedHashMap changes ----

    Node<K, V> newNode(int hash, K key, V value, Node<K, V> next) {
        return new Node<>(hash, key, value, next);
    }

    void afterNodeAccess(Node<K, V> p) {}

    void afterNodeInsertion(boolean evict) {}

    void afterNodeRemoval(Node<K, V> p) {}

    Iterator<K> newKeyIterator() {
        return new KeyIterator();
    }

    Iterator<V> newValueIterator() {
        return new ValueIterator();
    }

    Iterator<Map.Entry<K, V>> newEntryIterator() {
        return new EntryIterator();
    }

    // ---- Map's defaults, as the JDK's HashMap overrides them ----

    public V getOrDefault(Object key, V defaultValue) {
        Node<K, V> e;
        return (e = getNode(key)) == null ? defaultValue : e.value;
    }

    public V putIfAbsent(K key, V value) {
        return putVal(hash(key), key, value, true, true);
    }

    public boolean remove(Object key, Object value) {
        return removeNode(hash(key), key, value, true, true) != null;
    }

    public boolean replace(K key, V oldValue, V newValue) {
        Node<K, V> e;
        V v;
        if ((e = getNode(key)) != null && ((v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
            e.value = newValue;
            afterNodeAccess(e);
            return true;
        }
        return false;
    }

    public V replace(K key, V value) {
        Node<K, V> e;
        if ((e = getNode(key)) != null) {
            V oldValue = e.value;
            e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
        return null;
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (mappingFunction == null) throw new NullPointerException();
        int hash = hash(key);
        Node<K, V>[] tab;
        Node<K, V> first;
        int n, i;
        int binCount = 0;
        Node<K, V> old = null;
        if (size > threshold || (tab = table) == null || (n = tab.length) == 0) n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            Node<K, V> e = first;
            K k;
            do {
                if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                    old = e;
                    break;
                }
                ++binCount;
            } while ((e = e.next) != null);
            V oldValue;
            if (old != null && (oldValue = old.value) != null) {
                afterNodeAccess(old);
                return oldValue;
            }
        }
        int mc = modCount;
        V v = mappingFunction.apply(key);
        if (mc != modCount) throw new ConcurrentModificationException();
        if (v == null) return null;
        if (old != null) {
            old.value = v;
            afterNodeAccess(old);
            return v;
        }
        tab[i] = newNode(hash, key, v, first);
        if (binCount >= TREEIFY_THRESHOLD - 1) treeifyBin(tab);
        modCount = mc + 1;
        ++size;
        afterNodeInsertion(true);
        return v;
    }

    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null) throw new NullPointerException();
        Node<K, V> e;
        V oldValue;
        if ((e = getNode(key)) != null && (oldValue = e.value) != null) {
            int mc = modCount;
            V v = remappingFunction.apply(key, oldValue);
            if (mc != modCount) throw new ConcurrentModificationException();
            if (v != null) {
                e.value = v;
                afterNodeAccess(e);
                return v;
            }
            int hash = hash(key);
            removeNode(hash, key, null, false, true);
        }
        return null;
    }

    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null) throw new NullPointerException();
        int hash = hash(key);
        Node<K, V>[] tab;
        Node<K, V> first;
        int n, i;
        int binCount = 0;
        Node<K, V> old = null;
        if (size > threshold || (tab = table) == null || (n = tab.length) == 0) n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            Node<K, V> e = first;
            K k;
            do {
                if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                    old = e;
                    break;
                }
                ++binCount;
            } while ((e = e.next) != null);
        }
        V oldValue = (old == null) ? null : old.value;
        int mc = modCount;
        V v = remappingFunction.apply(key, oldValue);
        if (mc != modCount) throw new ConcurrentModificationException();
        if (old != null) {
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            } else {
                removeNode(hash, key, null, false, true);
            }
        } else if (v != null) {
            tab[i] = newNode(hash, key, v, first);
            if (binCount >= TREEIFY_THRESHOLD - 1) treeifyBin(tab);
            modCount = mc + 1;
            ++size;
            afterNodeInsertion(true);
        }
        return v;
    }

    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (value == null || remappingFunction == null) throw new NullPointerException();
        int hash = hash(key);
        Node<K, V>[] tab;
        Node<K, V> first;
        int n, i;
        int binCount = 0;
        Node<K, V> old = null;
        if (size > threshold || (tab = table) == null || (n = tab.length) == 0) n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            Node<K, V> e = first;
            K k;
            do {
                if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                    old = e;
                    break;
                }
                ++binCount;
            } while ((e = e.next) != null);
        }
        if (old != null) {
            V v;
            if (old.value != null) {
                int mc = modCount;
                v = remappingFunction.apply(old.value, value);
                if (mc != modCount) throw new ConcurrentModificationException();
            } else {
                v = value;
            }
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            } else {
                removeNode(hash, key, null, false, true);
            }
            return v;
        }
        tab[i] = newNode(hash, key, value, first);
        if (binCount >= TREEIFY_THRESHOLD - 1) treeifyBin(tab);
        ++modCount;
        ++size;
        afterNodeInsertion(true);
        return value;
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        Node<K, V>[] tab;
        if (action == null) throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (Node<K, V> e : tab) {
                for (; e != null; e = e.next) action.accept(e.key, e.value);
            }
            if (modCount != mc) throw new ConcurrentModificationException();
        }
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Node<K, V>[] tab;
        if (function == null) throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (Node<K, V> e : tab) {
                for (; e != null; e = e.next) e.value = function.apply(e.key, e.value);
            }
            if (modCount != mc) throw new ConcurrentModificationException();
        }
    }

    // ---- iterators ----

    abstract class HashIterator {
        Node<K, V> next;
        Node<K, V> current;
        int expectedModCount;
        int index;

        HashIterator() {
            expectedModCount = modCount;
            Node<K, V>[] t = table;
            current = next = null;
            index = 0;
            if (t != null && size > 0) {
                do {} while (index < t.length && (next = t[index++]) == null);
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Node<K, V> nextNode() {
            Node<K, V>[] t;
            Node<K, V> e = next;
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
            if (e == null) throw new NoSuchElementException();
            if ((next = (current = e).next) == null && (t = table) != null) {
                do {} while (index < t.length && (next = t[index++]) == null);
            }
            return e;
        }

        public final void remove() {
            Node<K, V> p = current;
            if (p == null) throw new IllegalStateException();
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
            current = null;
            removeNode(p.hash, p.key, null, false, false);
            expectedModCount = modCount;
        }
    }

    final class KeyIterator extends HashIterator implements Iterator<K> {
        public final K next() {
            return nextNode().key;
        }
    }

    final class ValueIterator extends HashIterator implements Iterator<V> {
        public final V next() {
            return nextNode().value;
        }
    }

    final class EntryIterator extends HashIterator implements Iterator<Map.Entry<K, V>> {
        public final Map.Entry<K, V> next() {
            return nextNode();
        }
    }
}
