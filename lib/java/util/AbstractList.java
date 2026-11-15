package java.util;

/**
 * The model of java.util.AbstractList: everything from get(int) and size(), with modCount for fail-fast iterators.
 * listIterator isn't modelled; indexOf and lastIndexOf go by index.
 */
public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {
    protected transient int modCount = 0;

    protected AbstractList() {}

    public boolean add(E e) {
        add(size(), e);
        return true;
    }

    public abstract E get(int index);

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
        for (int i = 0, n = size(); i < n; i++) {
            if (o == null ? get(i) == null : o.equals(get(i))) return i;
        }
        return -1;
    }

    public int lastIndexOf(Object o) {
        for (int i = size() - 1; i >= 0; i--) {
            if (o == null ? get(i) == null : o.equals(get(i))) return i;
        }
        return -1;
    }

    public void clear() {
        removeRange(0, size());
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);
        boolean modified = false;
        for (E e : c) {
            add(index++, e);
            modified = true;
        }
        return modified;
    }

    public Iterator<E> iterator() {
        return new Itr();
    }

    public List<E> subList(int fromIndex, int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size());
        return new SubList<>(this, fromIndex, toIndex);
    }

    static void subListRangeCheck(int fromIndex, int toIndex, int size) {
        if (fromIndex < 0) throw new IndexOutOfBoundsException("fromIndex = ".concat(Integer.toString(fromIndex)));
        if (toIndex > size) throw new IndexOutOfBoundsException("toIndex = ".concat(Integer.toString(toIndex)));
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(".concat(Integer.toString(fromIndex)).concat(") > toIndex(").concat(Integer.toString(toIndex)).concat(")"));
        }
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof List)) return false;
        Iterator<E> e1 = iterator();
        Iterator<?> e2 = ((List<?>) o).iterator();
        while (e1.hasNext() && e2.hasNext()) {
            E o1 = e1.next();
            Object o2 = e2.next();
            if (!(o1 == null ? o2 == null : o1.equals(o2))) return false;
        }
        return !(e1.hasNext() || e2.hasNext());
    }

    public int hashCode() {
        int hashCode = 1;
        for (E e : this) hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
        return hashCode;
    }

    protected void removeRange(int fromIndex, int toIndex) {
        for (int i = fromIndex; i < toIndex; i++) remove(fromIndex);
    }

    private void rangeCheckForAdd(int index) {
        if (index < 0 || index > size()) throw new IndexOutOfBoundsException(outOfBoundsMsg(index, size()));
    }

    static String outOfBoundsMsg(int index, int size) {
        return "Index: ".concat(Integer.toString(index)).concat(", Size: ").concat(Integer.toString(size));
    }

    private class Itr implements Iterator<E> {
        int cursor = 0;
        int lastRet = -1;
        int expectedModCount = modCount;

        public boolean hasNext() {
            return cursor != size();
        }

        public E next() {
            checkForComodification();
            int i = cursor;
            if (i >= size()) throw new NoSuchElementException();
            E next = get(i);
            lastRet = i;
            cursor = i + 1;
            return next;
        }

        public void remove() {
            if (lastRet < 0) throw new IllegalStateException();
            checkForComodification();
            AbstractList.this.remove(lastRet);
            if (lastRet < cursor) cursor--;
            lastRet = -1;
            expectedModCount = modCount;
        }

        final void checkForComodification() {
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
        }
    }

    /** A view of part of a list: what it changes, it changes in the list. */
    private static final class SubList<E> extends AbstractList<E> implements RandomAccess {
        private final AbstractList<E> root;
        private final int offset;
        private int size;

        SubList(AbstractList<E> root, int fromIndex, int toIndex) {
            this.root = root;
            this.offset = fromIndex;
            this.size = toIndex - fromIndex;
            this.modCount = root.modCount;
        }

        public E get(int index) {
            Objects.checkIndex(index, size);
            checkForComodification();
            return root.get(offset + index);
        }

        public E set(int index, E element) {
            Objects.checkIndex(index, size);
            checkForComodification();
            return root.set(offset + index, element);
        }

        public int size() {
            checkForComodification();
            return size;
        }

        public void add(int index, E element) {
            if (index < 0 || index > size) throw new IndexOutOfBoundsException(outOfBoundsMsg(index, size));
            checkForComodification();
            root.add(offset + index, element);
            this.modCount = root.modCount;
            size++;
        }

        public E remove(int index) {
            Objects.checkIndex(index, size);
            checkForComodification();
            E result = root.remove(offset + index);
            this.modCount = root.modCount;
            size--;
            return result;
        }

        private void checkForComodification() {
            if (root.modCount != this.modCount) throw new ConcurrentModificationException();
        }
    }

    /**
     * A list's spliterator by index: its size and modCount are taken when the traversal starts, an index gone out of
     * bounds is a ConcurrentModificationException, and modCount is checked after each element (tryAdvance) or after
     * them all (forEachRemaining).
     */
    static final class RandomAccessSpliterator<E> implements Spliterator<E> {
        private final List<E> list;
        private int index;
        private int fence;
        private final AbstractList<E> alist;
        private int expectedModCount;

        RandomAccessSpliterator(List<E> list) {
            this.list = list;
            this.index = 0;
            this.fence = -1;
            this.alist = list instanceof AbstractList ? (AbstractList<E>) list : null;
            this.expectedModCount = alist != null ? alist.modCount : 0;
        }

        private int getFence() {
            int hi;
            List<E> lst = list;
            if ((hi = fence) < 0) {
                if (alist != null) expectedModCount = alist.modCount;
                hi = fence = lst.size();
            }
            return hi;
        }

        public Spliterator<E> trySplit() {
            return null;
        }

        public boolean tryAdvance(java.util.function.Consumer<? super E> action) {
            if (action == null) throw new NullPointerException();
            int hi = getFence(), i = index;
            if (i < hi) {
                index = i + 1;
                action.accept(get(list, i));
                checkAbstractListModCount(alist, expectedModCount);
                return true;
            }
            return false;
        }

        public void forEachRemaining(java.util.function.Consumer<? super E> action) {
            Objects.requireNonNull(action);
            List<E> lst = list;
            int hi = getFence();
            int i = index;
            index = hi;
            for (; i < hi; i++) action.accept(get(lst, i));
            checkAbstractListModCount(alist, expectedModCount);
        }

        public long estimateSize() {
            return getFence() - index;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }

        private static <E> E get(List<E> list, int i) {
            try {
                return list.get(i);
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        static void checkAbstractListModCount(AbstractList<?> alist, int expectedModCount) {
            if (alist != null && alist.modCount != expectedModCount) throw new ConcurrentModificationException();
        }
    }
}
