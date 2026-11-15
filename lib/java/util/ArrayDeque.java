package java.util;

import java.util.function.Consumer;

/**
 * The model of java.util.ArrayDeque, laid out as the JDK's, where a program can see it through an iterator that
 * runs while the deque changes: a circular array (17 slots at first, or the size asked for plus one), the first
 * element at head, the next free slot at tail; full, it grows by its size plus two under 64 slots, by half above,
 * the elements from head moved to the end; removing inside moves the shorter side. Its iterators visit as many
 * elements as there were when they started, from where head was, and find a null slot a
 * ConcurrentModificationException, as the JDK's do.
 */
public class ArrayDeque<E> extends AbstractCollection<E> implements Deque<E>, Cloneable, java.io.Serializable {
    transient Object[] elements;
    transient int head;
    transient int tail;

    public ArrayDeque() {
        elements = new Object[16 + 1];
    }

    public ArrayDeque(int numElements) {
        elements = new Object[(numElements < 1) ? 1 : (numElements == Integer.MAX_VALUE) ? Integer.MAX_VALUE : numElements + 1];
    }

    public ArrayDeque(Collection<? extends E> c) {
        this(c.size());
        for (E e : c) addLast(e);
    }

    static int inc(int i, int modulus) {
        return ++i >= modulus ? 0 : i;
    }

    static int dec(int i, int modulus) {
        return --i < 0 ? modulus - 1 : i;
    }

    /** How many slots from j up to i, around the end. */
    static int sub(int i, int j, int modulus) {
        int d = i - j;
        return d < 0 ? d + modulus : d;
    }

    @SuppressWarnings("unchecked")
    static <E> E elementAt(Object[] es, int i) {
        return (E) es[i];
    }

    static <E> E nonNullElementAt(Object[] es, int i) {
        E e = elementAt(es, i);
        if (e == null) throw new ConcurrentModificationException();
        return e;
    }

    private void grow() {
        int oldCapacity = elements.length;
        int jump = (oldCapacity < 64) ? (oldCapacity + 2) : (oldCapacity >> 1);
        int newCapacity = oldCapacity + jump;
        if (newCapacity - (Integer.MAX_VALUE - 8) > 0 || jump < 1) throw new IllegalStateException("Sorry, deque too big");
        Object[] es = new Object[newCapacity];
        for (int k = 0; k < oldCapacity; k++) es[k] = elements[k];
        elements = es;
        if (tail < head || (tail == head && es[head] != null)) {
            // The elements from head on move to the new end.
            int newSpace = newCapacity - oldCapacity;
            for (int k = oldCapacity - 1; k >= head; k--) es[k + newSpace] = es[k];
            for (int k = head; k < head + newSpace; k++) es[k] = null;
            head += newSpace;
        }
    }

    public void addFirst(E e) {
        if (e == null) throw new NullPointerException();
        Object[] es = elements;
        es[head = dec(head, es.length)] = e;
        if (head == tail) grow();
    }

    public void addLast(E e) {
        if (e == null) throw new NullPointerException();
        Object[] es = elements;
        es[tail] = e;
        if (head == (tail = inc(tail, es.length))) grow();
    }

    public boolean addAll(Collection<? extends E> c) {
        int s = size();
        for (E e : c) addLast(e);
        return size() > s;
    }

    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    public E removeFirst() {
        E e = pollFirst();
        if (e == null) throw new NoSuchElementException();
        return e;
    }

    public E removeLast() {
        E e = pollLast();
        if (e == null) throw new NoSuchElementException();
        return e;
    }

    public E pollFirst() {
        Object[] es = elements;
        int h = head;
        E e = elementAt(es, h);
        if (e != null) {
            es[h] = null;
            head = inc(h, es.length);
        }
        return e;
    }

    public E pollLast() {
        Object[] es = elements;
        int t = dec(tail, es.length);
        E e = elementAt(es, t);
        if (e != null) es[tail = t] = null;
        return e;
    }

    public E getFirst() {
        E e = elementAt(elements, head);
        if (e == null) throw new NoSuchElementException();
        return e;
    }

    public E getLast() {
        Object[] es = elements;
        E e = elementAt(es, dec(tail, es.length));
        if (e == null) throw new NoSuchElementException();
        return e;
    }

    public E peekFirst() {
        return elementAt(elements, head);
    }

    public E peekLast() {
        Object[] es = elements;
        return elementAt(es, dec(tail, es.length));
    }

    public boolean removeFirstOccurrence(Object o) {
        if (o != null) {
            Object[] es = elements;
            for (int i = head, n = size(); n > 0; i = inc(i, es.length), n--) {
                if (o.equals(es[i])) {
                    delete(i);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean removeLastOccurrence(Object o) {
        if (o != null) {
            Object[] es = elements;
            for (int i = dec(tail, es.length), n = size(); n > 0; i = dec(i, es.length), n--) {
                if (o.equals(es[i])) {
                    delete(i);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean add(E e) {
        addLast(e);
        return true;
    }

    public boolean offer(E e) {
        return offerLast(e);
    }

    public E remove() {
        return removeFirst();
    }

    public E poll() {
        return pollFirst();
    }

    public E element() {
        return getFirst();
    }

    public E peek() {
        return peekFirst();
    }

    public void push(E e) {
        addFirst(e);
    }

    public E pop() {
        return removeFirst();
    }

    /**
     * Removes the element at i by moving the shorter side over it: true when the elements after it moved back (an
     * iterator going forwards then looks at i again).
     */
    boolean delete(int i) {
        Object[] es = elements;
        int capacity = es.length;
        int h = head, t = tail;
        int front = sub(i, h, capacity);
        int back = sub(t, i, capacity) - 1;
        if (front < back) {
            for (int k = i, n = front; n > 0; n--) {
                int from = dec(k, capacity);
                es[k] = es[from];
                k = from;
            }
            es[h] = null;
            head = inc(h, capacity);
            return false;
        }
        tail = dec(t, capacity);
        for (int k = i, n = back; n > 0; n--) {
            int from = inc(k, capacity);
            es[k] = es[from];
            k = from;
        }
        es[tail] = null;
        return true;
    }

    public int size() {
        return sub(tail, head, elements.length);
    }

    public boolean isEmpty() {
        return head == tail;
    }

    public Iterator<E> iterator() {
        return new DeqIterator();
    }

    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    private class DeqIterator implements Iterator<E> {
        int cursor;
        int remaining = size();
        int lastRet = -1;

        DeqIterator() {
            cursor = head;
        }

        public final boolean hasNext() {
            return remaining > 0;
        }

        public E next() {
            if (remaining <= 0) throw new NoSuchElementException();
            Object[] es = elements;
            E e = nonNullElementAt(es, cursor);
            cursor = inc(lastRet = cursor, es.length);
            remaining--;
            return e;
        }

        public final void remove() {
            if (lastRet < 0) throw new IllegalStateException();
            postDelete(delete(lastRet));
            lastRet = -1;
        }

        /** Going forwards: when the elements after moved back, the next one is where the removed one was. */
        void postDelete(boolean shiftedBack) {
            if (shiftedBack) cursor = dec(cursor, elements.length);
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            while (hasNext()) action.accept(next());
        }
    }

    private class DescendingIterator extends DeqIterator {
        DescendingIterator() {
            cursor = dec(tail, elements.length);
        }

        public final E next() {
            if (remaining <= 0) throw new NoSuchElementException();
            Object[] es = elements;
            E e = nonNullElementAt(es, cursor);
            cursor = dec(lastRet = cursor, es.length);
            remaining--;
            return e;
        }

        /** Going backwards: when the elements before moved forwards, the next one is where the removed one was. */
        @Override
        void postDelete(boolean shiftedBack) {
            if (!shiftedBack) cursor = inc(cursor, elements.length);
        }
    }

    public boolean contains(Object o) {
        if (o != null) {
            Object[] es = elements;
            for (int i = head, n = size(); n > 0; i = inc(i, es.length), n--) if (o.equals(es[i])) return true;
        }
        return false;
    }

    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    public void clear() {
        Object[] es = elements;
        for (int i = head, n = size(); n > 0; i = inc(i, es.length), n--) es[i] = null;
        head = tail = 0;
    }

    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        Object[] es = elements;
        for (int i = head, n = size(); n > 0; i = inc(i, es.length), n--) action.accept(elementAt(es, i));
    }
}
