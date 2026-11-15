package java.util;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * The model of java.util.ArrayList: its elements in an array that grows, modCount for fail-fast iterators, the JDK's
 * messages. How much the array grows by isn't observable, so it isn't the JDK's. clone and listIterator aren't
 * modelled.
 */
public class ArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
    private static final Object[] EMPTY = {};

    transient Object[] elementData;
    private int size;

    public ArrayList(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("Illegal Capacity: ".concat(Integer.toString(initialCapacity)));
        this.elementData = initialCapacity == 0 ? EMPTY : new Object[initialCapacity];
    }

    public ArrayList() {
        this.elementData = EMPTY;
    }

    public ArrayList(Collection<? extends E> c) {
        Object[] a = c.toArray();
        size = a.length;
        elementData = size == 0 ? EMPTY : Arrays.copyOfObjects(a, size);
    }

    public void trimToSize() {
        modCount++;
    }

    public void ensureCapacity(int minCapacity) {
        if (minCapacity > elementData.length) {
            modCount++;
            grow(minCapacity);
        }
    }

    private Object[] grow(int minCapacity) {
        int capacity = Math.max(minCapacity, elementData.length + (elementData.length >> 1) + 1);
        Object[] bigger = new Object[Math.max(capacity, 10)];
        for (int i = 0; i < size; i++) bigger[i] = elementData[i];
        return elementData = bigger;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    public int indexOf(Object o) {
        Object[] es = elementData;
        if (o == null) {
            for (int i = 0; i < size; i++) if (es[i] == null) return i;
        } else {
            for (int i = 0; i < size; i++) if (o.equals(es[i])) return i;
        }
        return -1;
    }

    public int lastIndexOf(Object o) {
        Object[] es = elementData;
        if (o == null) {
            for (int i = size - 1; i >= 0; i--) if (es[i] == null) return i;
        } else {
            for (int i = size - 1; i >= 0; i--) if (o.equals(es[i])) return i;
        }
        return -1;
    }

    public Object[] toArray() {
        return Arrays.copyOfObjects(elementData, size);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        T[] r = a.length >= size ? a : (T[]) Arrays.newArrayLike(a, size);
        for (int i = 0; i < size; i++) r[i] = (T) elementData[i];
        if (r.length > size) r[size] = null;
        return r;
    }

    @SuppressWarnings("unchecked")
    E elementData(int index) {
        return (E) elementData[index];
    }

    public E get(int index) {
        Objects.checkIndex(index, size);
        return elementData(index);
    }

    public E getFirst() {
        if (size == 0) throw new NoSuchElementException();
        return elementData(0);
    }

    public E getLast() {
        if (size == 0) throw new NoSuchElementException();
        return elementData(size - 1);
    }

    public E set(int index, E element) {
        Objects.checkIndex(index, size);
        E oldValue = elementData(index);
        elementData[index] = element;
        return oldValue;
    }

    public boolean add(E e) {
        modCount++;
        if (size == elementData.length) grow(size + 1);
        elementData[size] = e;
        size = size + 1;
        return true;
    }

    public void add(int index, E element) {
        rangeCheckForAdd(index);
        modCount++;
        if (size == elementData.length) grow(size + 1);
        for (int i = size; i > index; i--) elementData[i] = elementData[i - 1];
        elementData[index] = element;
        size = size + 1;
    }

    public void addFirst(E element) {
        add(0, element);
    }

    public void addLast(E element) {
        add(element);
    }

    public E remove(int index) {
        Objects.checkIndex(index, size);
        E oldValue = elementData(index);
        fastRemove(index);
        return oldValue;
    }

    public E removeFirst() {
        if (size == 0) throw new NoSuchElementException();
        E oldValue = elementData(0);
        fastRemove(0);
        return oldValue;
    }

    public E removeLast() {
        if (size == 0) throw new NoSuchElementException();
        int last = size - 1;
        E oldValue = elementData(last);
        fastRemove(last);
        return oldValue;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof List)) return false;
        final int expectedModCount = modCount;
        boolean equal = super.equals(o);
        checkForComodification(expectedModCount);
        return equal;
    }

    public int hashCode() {
        int expectedModCount = modCount;
        int hash = 1;
        for (int i = 0; i < size; i++) {
            Object e = elementData[i];
            hash = 31 * hash + (e == null ? 0 : e.hashCode());
        }
        checkForComodification(expectedModCount);
        return hash;
    }

    private void checkForComodification(int expectedModCount) {
        if (modCount != expectedModCount) throw new ConcurrentModificationException();
    }

    public boolean remove(Object o) {
        int i = indexOf(o);
        if (i < 0) return false;
        fastRemove(i);
        return true;
    }

    private void fastRemove(int i) {
        modCount++;
        int newSize = size - 1;
        for (int j = i; j < newSize; j++) elementData[j] = elementData[j + 1];
        elementData[newSize] = null;
        size = newSize;
    }

    public void clear() {
        modCount++;
        for (int i = 0; i < size; i++) elementData[i] = null;
        size = 0;
    }

    public boolean addAll(Collection<? extends E> c) {
        Object[] a = c.toArray();
        modCount++;
        int numNew = a.length;
        if (numNew == 0) return false;
        if (numNew > elementData.length - size) grow(size + numNew);
        for (int i = 0; i < numNew; i++) elementData[size + i] = a[i];
        size = size + numNew;
        return true;
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);
        Object[] a = c.toArray();
        modCount++;
        int numNew = a.length;
        if (numNew == 0) return false;
        if (numNew > elementData.length - size) grow(size + numNew);
        for (int i = size - 1; i >= index; i--) elementData[i + numNew] = elementData[i];
        for (int i = 0; i < numNew; i++) elementData[index + i] = a[i];
        size = size + numNew;
        return true;
    }

    protected void removeRange(int fromIndex, int toIndex) {
        if (fromIndex > toIndex) throw new IndexOutOfBoundsException(rangeMsg(fromIndex, toIndex));
        modCount++;
        int removed = toIndex - fromIndex;
        for (int i = toIndex; i < size; i++) elementData[i - removed] = elementData[i];
        for (int i = size - removed; i < size; i++) elementData[i] = null;
        size = size - removed;
    }

    private void rangeCheckForAdd(int index) {
        if (index > size || index < 0) throw new IndexOutOfBoundsException(outOfBoundsMsg(index, size));
    }

    private static String rangeMsg(int fromIndex, int toIndex) {
        return "From Index: ".concat(Integer.toString(fromIndex)).concat(" > To Index: ").concat(Integer.toString(toIndex));
    }

    public boolean removeAll(Collection<?> c) {
        return batchRemove(c, false);
    }

    public boolean retainAll(Collection<?> c) {
        return batchRemove(c, true);
    }

    /** Keeps the elements c contains (complement) or doesn't: c is asked about each element once, in order. */
    private boolean batchRemove(Collection<?> c, boolean complement) {
        Objects.requireNonNull(c);
        int w = 0;
        int r = 0;
        try {
            for (; r < size; r++) {
                Object e = elementData[r];
                if (c.contains(e) == complement) elementData[w++] = e;
            }
        } catch (Throwable t) {
            // c.contains threw: the elements not asked about yet are kept, as the JDK keeps them.
            for (; r < size; r++) elementData[w++] = elementData[r];
            shrinkTo(w);
            throw t;
        }
        return shrinkTo(w);
    }

    private boolean shrinkTo(int w) {
        if (w == size) return false;
        modCount += size - w;
        for (int i = w; i < size; i++) elementData[i] = null;
        size = w;
        return true;
    }

    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        final int expectedModCount = modCount;
        final int size = this.size;
        for (int i = 0; modCount == expectedModCount && i < size; i++) action.accept(elementData(i));
        if (modCount != expectedModCount) throw new ConcurrentModificationException();
    }

    /** The filter is asked about every element before any is removed, as the JDK does. */
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        int expectedModCount = modCount;
        final int end = size;
        boolean[] dead = new boolean[end];
        boolean any = false;
        for (int i = 0; modCount == expectedModCount && i < end; i++) {
            if (filter.test(elementData(i))) {
                dead[i] = true;
                any = true;
            }
        }
        if (modCount != expectedModCount) throw new ConcurrentModificationException();
        if (!any) return false;
        modCount++;
        int w = 0;
        for (int i = 0; i < end; i++) if (!dead[i]) elementData[w++] = elementData[i];
        for (int i = w; i < end; i++) elementData[i] = null;
        size = w;
        return true;
    }

    public void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final int expectedModCount = modCount;
        for (int i = 0; modCount == expectedModCount && i < size; i++) elementData[i] = operator.apply(elementData(i));
        if (modCount != expectedModCount) throw new ConcurrentModificationException();
        modCount++;
    }

    @SuppressWarnings("unchecked")
    public void sort(Comparator<? super E> c) {
        final int expectedModCount = modCount;
        Arrays.sort((E[]) elementData, 0, size, c);
        if (modCount != expectedModCount) throw new ConcurrentModificationException();
        modCount++;
    }

    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        int cursor;
        int lastRet = -1;
        int expectedModCount = modCount;

        public boolean hasNext() {
            return cursor != size;
        }

        @SuppressWarnings("unchecked")
        public E next() {
            checkForComodification();
            int i = cursor;
            if (i >= size) throw new NoSuchElementException();
            Object[] es = ArrayList.this.elementData;
            if (i >= es.length) throw new ConcurrentModificationException();
            cursor = i + 1;
            return (E) es[lastRet = i];
        }

        public void remove() {
            if (lastRet < 0) throw new IllegalStateException();
            checkForComodification();
            ArrayList.this.remove(lastRet);
            cursor = lastRet;
            lastRet = -1;
            expectedModCount = modCount;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            final int size = ArrayList.this.size;
            int i = cursor;
            if (i < size) {
                for (; i < size && modCount == expectedModCount; i++) action.accept(elementData(i));
                cursor = i;
                lastRet = i - 1;
                checkForComodification();
            }
        }

        final void checkForComodification() {
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new ArrayListSpliterator(0, -1, 0);
    }

    /**
     * Binds to the size and modCount when the traversal starts; forEachRemaining reads the array it had then, and
     * checks modCount once all is done; tryAdvance checks it after each element.
     */
    final class ArrayListSpliterator implements Spliterator<E> {
        private int index;
        private int fence;
        private int expectedModCount;

        ArrayListSpliterator(int origin, int fence, int expectedModCount) {
            this.index = origin;
            this.fence = fence;
            this.expectedModCount = expectedModCount;
        }

        private int getFence() {
            int hi;
            if ((hi = fence) < 0) {
                expectedModCount = modCount;
                hi = fence = size;
            }
            return hi;
        }

        public Spliterator<E> trySplit() {
            return null;
        }

        @SuppressWarnings("unchecked")
        public boolean tryAdvance(Consumer<? super E> action) {
            if (action == null) throw new NullPointerException();
            int hi = getFence(), i = index;
            if (i < hi) {
                index = i + 1;
                E e = (E) elementData[i];
                action.accept(e);
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super E> action) {
            int i, hi, mc;
            Object[] a;
            if (action == null) throw new NullPointerException();
            if ((a = elementData) != null) {
                if ((hi = fence) < 0) {
                    mc = modCount;
                    hi = size;
                } else {
                    mc = expectedModCount;
                }
                if ((i = index) >= 0 && (index = hi) <= a.length) {
                    for (; i < hi; ++i) {
                        E e = (E) a[i];
                        action.accept(e);
                    }
                    if (modCount == mc) return;
                }
            }
            throw new ConcurrentModificationException();
        }

        public long estimateSize() {
            return getFence() - index;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }
}
