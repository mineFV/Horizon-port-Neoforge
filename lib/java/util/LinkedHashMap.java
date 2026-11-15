package java.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * The model of java.util.LinkedHashMap: a HashMap whose entries are also in a list, in the order they were put (or,
 * made with accessOrder, last used last), which is the order it iterates in. removeEldestEntry is asked after each
 * put, as the API specifies. SequencedMap's own methods aren't modelled.
 */
public class LinkedHashMap<K, V> extends HashMap<K, V> implements SequencedMap<K, V> {
    static class Entry<K, V> extends HashMap.Node<K, V> {
        Entry<K, V> before, after;

        Entry(int hash, K key, V value, Node<K, V> next) {
            super(hash, key, value, next);
        }
    }

    transient LinkedHashMap.Entry<K, V> head;
    transient LinkedHashMap.Entry<K, V> tail;
    final boolean accessOrder;

    public LinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        accessOrder = false;
    }

    public LinkedHashMap(int initialCapacity) {
        super(initialCapacity);
        accessOrder = false;
    }

    public LinkedHashMap() {
        super();
        accessOrder = false;
    }

    public LinkedHashMap(Map<? extends K, ? extends V> m) {
        super();
        accessOrder = false;
        putMapEntries(m, false);
    }

    public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor);
        this.accessOrder = accessOrder;
    }

    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int numMappings) {
        if (numMappings < 0) throw new IllegalArgumentException("Negative number of mappings: ".concat(Integer.toString(numMappings)));
        return new LinkedHashMap<>(HashMap.calculateHashMapCapacity(numMappings));
    }

    private void linkNodeLast(LinkedHashMap.Entry<K, V> p) {
        LinkedHashMap.Entry<K, V> last = tail;
        tail = p;
        if (last == null) {
            head = p;
        } else {
            p.before = last;
            last.after = p;
        }
    }

    Node<K, V> newNode(int hash, K key, V value, Node<K, V> e) {
        LinkedHashMap.Entry<K, V> p = new LinkedHashMap.Entry<>(hash, key, value, e);
        linkNodeLast(p);
        return p;
    }

    void afterNodeRemoval(Node<K, V> e) {
        LinkedHashMap.Entry<K, V> p = (LinkedHashMap.Entry<K, V>) e, b = p.before, a = p.after;
        p.before = p.after = null;
        if (b == null) head = a;
        else b.after = a;
        if (a == null) tail = b;
        else a.before = b;
    }

    void afterNodeInsertion(boolean evict) {
        LinkedHashMap.Entry<K, V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }

    void afterNodeAccess(Node<K, V> e) {
        LinkedHashMap.Entry<K, V> last;
        if (accessOrder && (last = tail) != e) {
            LinkedHashMap.Entry<K, V> p = (LinkedHashMap.Entry<K, V>) e, b = p.before, a = p.after;
            p.after = null;
            if (b == null) head = a;
            else b.after = a;
            if (a != null) a.before = b;
            else last = b;
            if (last == null) {
                head = p;
            } else {
                p.before = last;
                last.after = p;
            }
            tail = p;
            ++modCount;
        }
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return false;
    }

    public boolean containsValue(Object value) {
        for (LinkedHashMap.Entry<K, V> e = head; e != null; e = e.after) {
            V v = e.value;
            if (v == value || (value != null && value.equals(v))) return true;
        }
        return false;
    }

    public V get(Object key) {
        Node<K, V> e;
        if ((e = getNode(key)) == null) return null;
        if (accessOrder) afterNodeAccess(e);
        return e.value;
    }

    public V getOrDefault(Object key, V defaultValue) {
        Node<K, V> e;
        if ((e = getNode(key)) == null) return defaultValue;
        if (accessOrder) afterNodeAccess(e);
        return e.value;
    }

    public void clear() {
        super.clear();
        head = tail = null;
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) throw new NullPointerException();
        int mc = modCount;
        for (LinkedHashMap.Entry<K, V> e = head; e != null; e = e.after) action.accept(e.key, e.value);
        if (modCount != mc) throw new ConcurrentModificationException();
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) throw new NullPointerException();
        int mc = modCount;
        for (LinkedHashMap.Entry<K, V> e = head; e != null; e = e.after) e.value = function.apply(e.key, e.value);
        if (modCount != mc) throw new ConcurrentModificationException();
    }

    Iterator<K> newKeyIterator() {
        return new LinkedKeyIterator();
    }

    Iterator<V> newValueIterator() {
        return new LinkedValueIterator();
    }

    Iterator<Map.Entry<K, V>> newEntryIterator() {
        return new LinkedEntryIterator();
    }

    abstract class LinkedHashIterator {
        LinkedHashMap.Entry<K, V> next;
        LinkedHashMap.Entry<K, V> current;
        int expectedModCount;

        LinkedHashIterator() {
            next = head;
            expectedModCount = modCount;
            current = null;
        }

        public final boolean hasNext() {
            return next != null;
        }

        final LinkedHashMap.Entry<K, V> nextNode() {
            LinkedHashMap.Entry<K, V> e = next;
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
            if (e == null) throw new NoSuchElementException();
            current = e;
            next = e.after;
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

    final class LinkedKeyIterator extends LinkedHashIterator implements Iterator<K> {
        public final K next() {
            return nextNode().getKey();
        }
    }

    final class LinkedValueIterator extends LinkedHashIterator implements Iterator<V> {
        public final V next() {
            return nextNode().value;
        }
    }

    final class LinkedEntryIterator extends LinkedHashIterator implements Iterator<Map.Entry<K, V>> {
        public final Map.Entry<K, V> next() {
            return nextNode();
        }
    }
}
