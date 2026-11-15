package java.util.concurrent;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The model of java.util.concurrent.ConcurrentHashMap, laid out as the JDK's (the tests', 25) where a program can see
 * it: the table's sizes (a constructor's capacity is the power of two above 1 + capacity / 0.75, a map's its size),
 * putAll one put after another, growing when the size reaches three
 * quarters of the table (not past it, as a HashMap), a node put at the end of its bin, a resize that walks the
 * bins from the last and keeps each bin's tail that goes one way, putting the nodes before it in front, reversed;
 * a bin of eight in a table under 64 nodes long growing the table, by eight times; iterators that never throw
 * ConcurrentModificationException, and follow a resize the way the JDK's do. Null keys and values are a
 * NullPointerException. One monitor (the map's) guards it all: the JDK's reads take none, but each call is as
 * atomic. A bin the JDK makes a tree (eight nodes in a table of 64 or more) stays a list: its order differs then.
 */
public class ConcurrentHashMap<K, V> extends java.util.AbstractMap<K, V> implements ConcurrentMap<K, V>, java.io.Serializable {
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
    static final int TREEIFY_THRESHOLD = 8;
    static final int MIN_TREEIFY_CAPACITY = 64;
    static final int MOVED = -1;
    static final int RESERVED = -3;
    static final int HASH_BITS = 0x7fffffff;

    static class Node<K, V> {
        final int hash;
        final K key;
        V val;
        Node<K, V> next;

        Node(int hash, K key, V val) {
            this.hash = hash;
            this.key = key;
            this.val = val;
        }

        Node(int hash, K key, V val, Node<K, V> next) {
            this(hash, key, val);
            this.next = next;
        }
    }

    /** Where a bin went in a resize: the bin's new table. */
    static final class Forward<K, V> extends Node<K, V> {
        final Node<K, V>[] nextTable;

        Forward(Node<K, V>[] tab) {
            super(MOVED, null, null);
            this.nextTable = tab;
        }
    }

    /** A bin held while computeIfAbsent's or compute's function runs. */
    static final class Reservation<K, V> extends Node<K, V> {
        Reservation() {
            super(RESERVED, null, null);
        }
    }

    static int spread(int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }

    private static int tableSizeFor(int c) {
        int n = -1 >>> Integer.numberOfLeadingZeros(c - 1);
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    private transient Node<K, V>[] table;
    private transient int sizeCtl;
    private transient long count;

    public ConcurrentHashMap() {}

    public ConcurrentHashMap(int initialCapacity) {
        this(initialCapacity, LOAD_FACTOR, 1);
    }

    /** Sized for the map's size, as the JDK the tests run on. */
    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this(m.size());
        putAll(m);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0) throw new IllegalArgumentException();
        if (initialCapacity < concurrencyLevel) initialCapacity = concurrencyLevel;
        long size = (long) (1.0 + (long) initialCapacity / loadFactor);
        this.sizeCtl = (size >= (long) MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : tableSizeFor((int) size);
    }

    public static <K> KeySetView<K, Boolean> newKeySet() {
        return new KeySetView<K, Boolean>(new ConcurrentHashMap<K, Boolean>(), Boolean.TRUE);
    }

    @SuppressWarnings("unchecked")
    private Node<K, V>[] initTable() {
        int sc = sizeCtl;
        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
        Node<K, V>[] nt = (Node<K, V>[]) new Node<?, ?>[n];
        table = nt;
        sizeCtl = n - (n >>> 2);
        return nt;
    }

    // ---- reading ----

    public synchronized V get(Object key) {
        int h = spread(key.hashCode());
        Node<K, V>[] tab = table;
        if (tab == null || tab.length == 0) return null;
        for (Node<K, V> e = tab[(tab.length - 1) & h]; e != null; e = e.next) {
            if (e.hash == h && (e.key == key || key.equals(e.key))) return e.val;
        }
        return null;
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public synchronized boolean containsValue(Object value) {
        if (value == null) throw new NullPointerException();
        Traverser<K, V> it = new Traverser<>(table);
        for (Node<K, V> p; (p = it.advance()) != null; ) {
            V v = p.val;
            if (v == value || value.equals(v)) return true;
        }
        return false;
    }

    public synchronized int size() {
        return count < 0L ? 0 : count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    public synchronized long mappingCount() {
        return Math.max(count, 0L);
    }

    public synchronized boolean isEmpty() {
        return count <= 0L;
    }

    public V getOrDefault(Object key, V defaultValue) {
        V v = get(key);
        return v == null ? defaultValue : v;
    }

    // ---- writing ----

    public V put(K key, V value) {
        return putVal(key, value, false);
    }

    public V putIfAbsent(K key, V value) {
        return putVal(key, value, true);
    }

    final synchronized V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());
        int binCount = 0;
        Node<K, V>[] tab = table;
        if (tab == null || tab.length == 0) tab = initTable();
        int i = (tab.length - 1) & hash;
        Node<K, V> f = tab[i];
        if (f == null) {
            tab[i] = new Node<>(hash, key, value);
        } else {
            if (f.hash == RESERVED) throw new IllegalStateException("Recursive update");
            V oldVal = null;
            binCount = 1;
            for (Node<K, V> e = f; ; ++binCount) {
                if (e.hash == hash && (e.key == key || key.equals(e.key))) {
                    oldVal = e.val;
                    if (!onlyIfAbsent) e.val = value;
                    break;
                }
                Node<K, V> pred = e;
                if ((e = e.next) == null) {
                    pred.next = new Node<>(hash, key, value);
                    break;
                }
            }
            if (binCount >= TREEIFY_THRESHOLD) treeifyBin(tab);
            if (oldVal != null) return oldVal;
        }
        addCount(1L, binCount);
        return null;
    }

    /** One put after another (the JDK the tests run on doesn't size the table first). */
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) putVal(e.getKey(), e.getValue(), false);
    }

    public V remove(Object key) {
        return replaceNode(key, null, null);
    }

    public boolean remove(Object key, Object value) {
        if (key == null) throw new NullPointerException();
        return value != null && replaceNode(key, null, value) != null;
    }

    public boolean replace(K key, V oldValue, V newValue) {
        if (key == null || oldValue == null || newValue == null) throw new NullPointerException();
        return replaceNode(key, newValue, oldValue) != null;
    }

    public V replace(K key, V value) {
        if (key == null || value == null) throw new NullPointerException();
        return replaceNode(key, value, null);
    }

    /** Replaces the value (removes the node when value is null) if it equals cv (any when cv is null); the old value. */
    final synchronized V replaceNode(Object key, V value, Object cv) {
        int hash = spread(key.hashCode());
        Node<K, V>[] tab = table;
        if (tab == null || tab.length == 0) return null;
        int i = (tab.length - 1) & hash;
        Node<K, V> f = tab[i];
        if (f == null) return null;
        if (f.hash == RESERVED) throw new IllegalStateException("Recursive update");
        for (Node<K, V> e = f, pred = null; ; ) {
            if (e.hash == hash && (e.key == key || key.equals(e.key))) {
                V ev = e.val;
                if (cv == null || cv == ev || cv.equals(ev)) {
                    if (value != null) {
                        e.val = value;
                    } else if (pred != null) {
                        pred.next = e.next;
                    } else {
                        tab[i] = e.next;
                    }
                    if (value == null) addCount(-1L, -1);
                    return ev;
                }
                return null;
            }
            pred = e;
            if ((e = e.next) == null) return null;
        }
    }

    public synchronized void clear() {
        long delta = 0L;
        Node<K, V>[] tab = table;
        if (tab == null) return;
        for (int i = 0; i < tab.length; i++) {
            Node<K, V> f = tab[i];
            if (f == null) continue;
            for (Node<K, V> p = f.hash >= 0 ? f : null; p != null; p = p.next) --delta;
            tab[i] = null;
        }
        if (delta != 0L) addCount(delta, -1);
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (key == null || mappingFunction == null) throw new NullPointerException();
        synchronized (this) {
            int h = spread(key.hashCode());
            V val = null;
            int binCount = 0;
            Node<K, V>[] tab = table;
            if (tab == null || tab.length == 0) tab = initTable();
            int i = (tab.length - 1) & h;
            Node<K, V> f = tab[i];
            if (f == null) {
                Node<K, V> r = new Reservation<>();
                tab[i] = r;
                binCount = 1;
                Node<K, V> node = null;
                try {
                    if ((val = mappingFunction.apply(key)) != null) node = new Node<>(h, key, val);
                } finally {
                    tab[i] = node;
                }
            } else {
                if (f.hash == h && (f.key == key || key.equals(f.key)) && f.val != null) return f.val;
                if (f.hash == RESERVED) throw new IllegalStateException("Recursive update");
                boolean added = false;
                binCount = 1;
                for (Node<K, V> e = f; ; ++binCount) {
                    if (e.hash == h && (e.key == key || key.equals(e.key))) {
                        val = e.val;
                        break;
                    }
                    Node<K, V> pred = e;
                    if ((e = e.next) == null) {
                        if ((val = mappingFunction.apply(key)) != null) {
                            if (pred.next != null) throw new IllegalStateException("Recursive update");
                            added = true;
                            pred.next = new Node<>(h, key, val);
                        }
                        break;
                    }
                }
                if (binCount >= TREEIFY_THRESHOLD) treeifyBin(tab);
                if (!added) return val;
            }
            if (val != null) addCount(1L, binCount);
            return val;
        }
    }

    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null) throw new NullPointerException();
        synchronized (this) {
            int h = spread(key.hashCode());
            Node<K, V>[] tab = table;
            if (tab == null || tab.length == 0) return null;
            int i = (tab.length - 1) & h;
            Node<K, V> f = tab[i];
            if (f == null) return null;
            if (f.hash == RESERVED) throw new IllegalStateException("Recursive update");
            V val = null;
            int delta = 0;
            int binCount = 1;
            for (Node<K, V> e = f, pred = null; ; ++binCount) {
                if (e.hash == h && (e.key == key || key.equals(e.key))) {
                    val = remappingFunction.apply(key, e.val);
                    if (val != null) {
                        e.val = val;
                    } else {
                        delta = -1;
                        Node<K, V> en = e.next;
                        if (pred != null) pred.next = en;
                        else tab[i] = en;
                    }
                    break;
                }
                pred = e;
                if ((e = e.next) == null) break;
            }
            if (delta != 0) addCount(delta, binCount);
            return val;
        }
    }

    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null) throw new NullPointerException();
        synchronized (this) {
            int h = spread(key.hashCode());
            V val = null;
            int delta = 0;
            int binCount = 0;
            Node<K, V>[] tab = table;
            if (tab == null || tab.length == 0) tab = initTable();
            int i = (tab.length - 1) & h;
            Node<K, V> f = tab[i];
            if (f == null) {
                Node<K, V> r = new Reservation<>();
                tab[i] = r;
                binCount = 1;
                Node<K, V> node = null;
                try {
                    if ((val = remappingFunction.apply(key, null)) != null) {
                        delta = 1;
                        node = new Node<>(h, key, val);
                    }
                } finally {
                    tab[i] = node;
                }
            } else {
                if (f.hash == RESERVED) throw new IllegalStateException("Recursive update");
                binCount = 1;
                for (Node<K, V> e = f, pred = null; ; ++binCount) {
                    if (e.hash == h && (e.key == key || key.equals(e.key))) {
                        val = remappingFunction.apply(key, e.val);
                        if (val != null) {
                            e.val = val;
                        } else {
                            delta = -1;
                            Node<K, V> en = e.next;
                            if (pred != null) pred.next = en;
                            else tab[i] = en;
                        }
                        break;
                    }
                    pred = e;
                    if ((e = e.next) == null) {
                        val = remappingFunction.apply(key, null);
                        if (val != null) {
                            if (pred.next != null) throw new IllegalStateException("Recursive update");
                            delta = 1;
                            pred.next = new Node<>(h, key, val);
                        }
                        break;
                    }
                }
                if (binCount >= TREEIFY_THRESHOLD) treeifyBin(tab);
            }
            if (delta != 0) addCount(delta, binCount);
            return val;
        }
    }

    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (key == null || value == null || remappingFunction == null) throw new NullPointerException();
        synchronized (this) {
            int h = spread(key.hashCode());
            V val = null;
            int delta = 0;
            int binCount = 0;
            Node<K, V>[] tab = table;
            if (tab == null || tab.length == 0) tab = initTable();
            int i = (tab.length - 1) & h;
            Node<K, V> f = tab[i];
            if (f == null) {
                tab[i] = new Node<>(h, key, value);
                delta = 1;
                val = value;
            } else {
                if (f.hash == RESERVED) throw new IllegalStateException("Recursive update");
                binCount = 1;
                for (Node<K, V> e = f, pred = null; ; ++binCount) {
                    if (e.hash == h && (e.key == key || key.equals(e.key))) {
                        val = remappingFunction.apply(e.val, value);
                        if (val != null) {
                            e.val = val;
                        } else {
                            delta = -1;
                            Node<K, V> en = e.next;
                            if (pred != null) pred.next = en;
                            else tab[i] = en;
                        }
                        break;
                    }
                    pred = e;
                    if ((e = e.next) == null) {
                        delta = 1;
                        val = value;
                        pred.next = new Node<>(h, key, val);
                        break;
                    }
                }
                if (binCount >= TREEIFY_THRESHOLD) treeifyBin(tab);
            }
            if (delta != 0) addCount(delta, binCount);
            return val;
        }
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) throw new NullPointerException();
        Traverser<K, V> it;
        synchronized (this) {
            it = new Traverser<>(table);
        }
        for (Node<K, V> p; (p = it.advance()) != null; ) action.accept(p.key, p.val);
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) throw new NullPointerException();
        Traverser<K, V> it;
        synchronized (this) {
            it = new Traverser<>(table);
        }
        for (Node<K, V> p; (p = it.advance()) != null; ) {
            V oldValue = p.val;
            for (K key = p.key; ; ) {
                V newValue = function.apply(key, oldValue);
                if (newValue == null) throw new NullPointerException();
                if (replaceNode(key, newValue, oldValue) != null || (oldValue = get(key)) == null) break;
            }
        }
    }

    // ---- growing ----

    /** After count changed by x: a check of 0 or more (a node added in a bin of that many) may grow the table. */
    private void addCount(long x, int check) {
        long s = count += x;
        if (check >= 0) {
            Node<K, V>[] tab;
            while (s >= (long) sizeCtl && (tab = table) != null && tab.length < MAXIMUM_CAPACITY) {
                transfer(tab);
                s = count;
            }
        }
    }

    /** A bin of eight: in a table under 64, a presize to twice the table (the JDK's, which grows it eight times). */
    private void treeifyBin(Node<K, V>[] tab) {
        if (tab.length < MIN_TREEIFY_CAPACITY) tryPresize(tab.length << 1);
    }

    @SuppressWarnings("unchecked")
    private void tryPresize(int size) {
        int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY : tableSizeFor(size + (size >>> 1) + 1);
        int sc;
        while ((sc = sizeCtl) >= 0) {
            Node<K, V>[] tab = table;
            int n;
            if (tab == null || (n = tab.length) == 0) {
                n = (sc > c) ? sc : c;
                table = (Node<K, V>[]) new Node<?, ?>[n];
                sizeCtl = n - (n >>> 2);
            } else if (c <= sc || n >= MAXIMUM_CAPACITY) {
                break;
            } else {
                transfer(tab);
            }
        }
    }

    /**
     * Doubles the table: each bin from the last splits by the new bit; the run at its end that goes one way is kept,
     * the nodes before it go in front of their side's list, in reverse. Each old bin then forwards to the new table.
     */
    @SuppressWarnings("unchecked")
    private void transfer(Node<K, V>[] tab) {
        int n = tab.length;
        Node<K, V>[] nextTab = (Node<K, V>[]) new Node<?, ?>[n << 1];
        Forward<K, V> fwd = new Forward<>(nextTab);
        for (int i = n - 1; i >= 0; i--) {
            Node<K, V> f = tab[i];
            if (f != null) {
                if (f.hash == RESERVED) throw new IllegalStateException("Recursive update");
                int runBit = f.hash & n;
                Node<K, V> lastRun = f;
                for (Node<K, V> p = f.next; p != null; p = p.next) {
                    int b = p.hash & n;
                    if (b != runBit) {
                        runBit = b;
                        lastRun = p;
                    }
                }
                Node<K, V> ln, hn;
                if (runBit == 0) {
                    ln = lastRun;
                    hn = null;
                } else {
                    hn = lastRun;
                    ln = null;
                }
                for (Node<K, V> p = f; p != lastRun; p = p.next) {
                    if ((p.hash & n) == 0) ln = new Node<>(p.hash, p.key, p.val, ln);
                    else hn = new Node<>(p.hash, p.key, p.val, hn);
                }
                nextTab[i] = ln;
                nextTab[i + n] = hn;
            }
            tab[i] = fwd;
        }
        table = nextTab;
        sizeCtl = (n << 1) - (n >>> 1);
    }

    // ---- traversing ----

    /**
     * Visits the bins of the table it started with, in order; a bin forwarded by a resize sends it to the new table's
     * two bins for it (and back), as the JDK's traverser does. It never throws ConcurrentModificationException.
     */
    static class Traverser<K, V> {
        Node<K, V>[] tab;
        Node<K, V> next;
        int[] stackIndex = new int[32];
        int[] stackLength = new int[32];
        Node<K, V>[][] stackTab = newStack();
        int depth;
        int index;
        int baseIndex;
        final int baseLimit;
        final int baseSize;

        @SuppressWarnings("unchecked")
        private static <K, V> Node<K, V>[][] newStack() {
            return (Node<K, V>[][]) new Node<?, ?>[32][];
        }

        Traverser(Node<K, V>[] tab) {
            this.tab = tab;
            this.baseSize = tab == null ? 0 : tab.length;
            this.baseLimit = baseSize;
        }

        @SuppressWarnings("unchecked")
        Node<K, V> advance() {
            Node<K, V> e;
            if ((e = next) != null) e = e.next;
            for (;;) {
                Node<K, V>[] t;
                int i, n;
                if (e != null) return next = e;
                if (baseIndex >= baseLimit || (t = tab) == null || (n = t.length) <= (i = index) || i < 0) return next = null;
                if ((e = t[i]) != null && e.hash < 0) {
                    if (e instanceof Forward<K, V> fw) {
                        tab = fw.nextTable;
                        e = null;
                        stackTab[depth] = t;
                        stackLength[depth] = n;
                        stackIndex[depth] = i;
                        depth++;
                        continue;
                    }
                    e = null;
                }
                if (depth > 0) recoverState(n);
                else if ((index = i + baseSize) >= n) index = ++baseIndex;
            }
        }

        @SuppressWarnings("unchecked")
        private void recoverState(int n) {
            int len;
            while (depth > 0 && (index += (len = stackLength[depth - 1])) >= n) {
                n = len;
                depth--;
                index = stackIndex[depth];
                tab = stackTab[depth];
                stackTab[depth] = null;
            }
            if (depth == 0 && (index += baseSize) >= n) index = ++baseIndex;
        }
    }

    /** Keys, values or entries (as entries whose setValue puts into the map). remove removes the last given's key. */
    final class Iter<T> implements Iterator<T> {
        private final Traverser<K, V> traverser;
        private final int kind;
        private Node<K, V> lastReturned;

        Iter(int kind) {
            this.kind = kind;
            synchronized (ConcurrentHashMap.this) {
                traverser = new Traverser<>(table);
                traverser.advance();
            }
        }

        public boolean hasNext() {
            return traverser.next != null;
        }

        @SuppressWarnings("unchecked")
        public T next() {
            Node<K, V> p = traverser.next;
            if (p == null) throw new NoSuchElementException();
            lastReturned = p;
            synchronized (ConcurrentHashMap.this) {
                traverser.advance();
            }
            return kind == 0 ? (T) p.key : kind == 1 ? (T) p.val : (T) new MapEntry(p.key, p.val);
        }

        public void remove() {
            Node<K, V> p = lastReturned;
            if (p == null) throw new IllegalStateException();
            lastReturned = null;
            replaceNode(p.key, null, null);
        }
    }

    final class MapEntry implements Map.Entry<K, V> {
        final K key;
        V val;

        MapEntry(K key, V val) {
            this.key = key;
            this.val = val;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return val;
        }

        public int hashCode() {
            return key.hashCode() ^ val.hashCode();
        }

        public String toString() {
            return String.valueOf(key).concat("=").concat(String.valueOf(val));
        }

        public boolean equals(Object o) {
            Object k, v;
            Map.Entry<?, ?> e;
            return ((o instanceof Map.Entry) && (k = (e = (Map.Entry<?, ?>) o).getKey()) != null && (v = e.getValue()) != null && (k == key || k.equals(key))
                    && (v == val || v.equals(val)));
        }

        public V setValue(V value) {
            if (value == null) throw new NullPointerException();
            V v = val;
            val = value;
            put(key, value);
            return v;
        }
    }

    public KeySetView<K, V> keySet() {
        return new KeySetView<>(this, null);
    }

    public Collection<V> values() {
        return new AbstractCollection<V>() {
            public Iterator<V> iterator() {
                return new Iter<V>(1);
            }

            public int size() {
                return ConcurrentHashMap.this.size();
            }

            public boolean contains(Object o) {
                return containsValue(o);
            }

            public boolean remove(Object o) {
                if (o != null) {
                    for (Iterator<V> it = iterator(); it.hasNext(); ) {
                        if (o.equals(it.next())) {
                            it.remove();
                            return true;
                        }
                    }
                }
                return false;
            }

            public void clear() {
                ConcurrentHashMap.this.clear();
            }
        };
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>() {
            public Iterator<Map.Entry<K, V>> iterator() {
                return new Iter<Map.Entry<K, V>>(2);
            }

            public int size() {
                return ConcurrentHashMap.this.size();
            }

            public boolean contains(Object o) {
                Object k, v, r;
                Map.Entry<?, ?> e;
                return ((o instanceof Map.Entry) && (k = (e = (Map.Entry<?, ?>) o).getKey()) != null && (r = get(k)) != null && (v = e.getValue()) != null
                        && (v == r || v.equals(r)));
            }

            public boolean remove(Object o) {
                Object k, v;
                Map.Entry<?, ?> e;
                return ((o instanceof Map.Entry) && (k = (e = (Map.Entry<?, ?>) o).getKey()) != null && (v = e.getValue()) != null
                        && ConcurrentHashMap.this.remove(k, v));
            }

            public void clear() {
                ConcurrentHashMap.this.clear();
            }
        };
    }

    /** A key set: adding puts its value (newKeySet's TRUE), none for keySet(). */
    public static final class KeySetView<K, V> extends AbstractSet<K> implements java.io.Serializable {
        private final ConcurrentHashMap<K, V> map;
        private final V value;

        KeySetView(ConcurrentHashMap<K, V> map, V value) {
            this.map = map;
            this.value = value;
        }

        public Iterator<K> iterator() {
            return map.new Iter<K>(0);
        }

        public ConcurrentHashMap<K, V> getMap() {
            return map;
        }

        public V getMappedValue() {
            return value;
        }

        public int size() {
            return map.size();
        }

        public boolean contains(Object o) {
            return map.containsKey(o);
        }

        public boolean remove(Object o) {
            return map.remove(o) != null;
        }

        public void clear() {
            map.clear();
        }

        public boolean add(K e) {
            V v;
            if ((v = value) == null) throw new UnsupportedOperationException();
            return map.putVal(e, v, true) == null;
        }

        public boolean addAll(Collection<? extends K> c) {
            boolean added = false;
            V v;
            if ((v = value) == null) throw new UnsupportedOperationException();
            for (K e : c) {
                if (map.putVal(e, v, true) == null) added = true;
            }
            return added;
        }

        public boolean equals(Object o) {
            Set<?> c;
            return ((o instanceof Set) && ((c = (Set<?>) o) == this || (containsAll(c) && c.containsAll(this))));
        }
    }

    public synchronized int hashCode() {
        int h = 0;
        Traverser<K, V> it = new Traverser<>(table);
        for (Node<K, V> p; (p = it.advance()) != null; ) h += p.key.hashCode() ^ p.val.hashCode();
        return h;
    }

    public String toString() {
        Traverser<K, V> it;
        synchronized (this) {
            it = new Traverser<>(table);
        }
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        Node<K, V> p;
        if ((p = it.advance()) != null) {
            for (;;) {
                K k = p.key;
                V v = p.val;
                sb.append(k == this ? "(this Map)" : String.valueOf(k));
                sb.append('=');
                sb.append(v == this ? "(this Map)" : String.valueOf(v));
                if ((p = it.advance()) == null) break;
                sb.append(',').append(' ');
            }
        }
        return sb.append('}').toString();
    }

    public boolean equals(Object o) {
        if (o != this) {
            if (!(o instanceof Map)) return false;
            Map<?, ?> m = (Map<?, ?>) o;
            Traverser<K, V> it;
            synchronized (this) {
                it = new Traverser<>(table);
            }
            for (Node<K, V> p; (p = it.advance()) != null; ) {
                V val = p.val;
                Object v = m.get(p.key);
                if (v == null || (v != val && !v.equals(val))) return false;
            }
            for (Map.Entry<?, ?> e : m.entrySet()) {
                Object mk, mv, v;
                if ((mk = e.getKey()) == null || (mv = e.getValue()) == null || (v = get(mk)) == null || (mv != v && !mv.equals(v))) return false;
            }
        }
        return true;
    }
}
