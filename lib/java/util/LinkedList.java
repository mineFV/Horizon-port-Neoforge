package java.util;

import java.util.function.Consumer;

/**
 * The model of java.util.LinkedList: a doubly linked list, a List and a Deque. Its iterator is fail-fast as the
 * JDK's: a structural change not made through it is a ConcurrentModificationException at the next next() or
 * remove(). Not modelled: listIterator, descendingIterator, reversed.
 */
public class LinkedList<E> extends AbstractSequentialList<E> implements List<E>, Deque<E>, Cloneable, java.io.Serializable {
    transient int size = 0;
    transient Node<E> first;
    transient Node<E> last;

    public LinkedList() {}

    public LinkedList(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    private static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;

        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }

    private void linkFirst(E e) {
        Node<E> f = first;
        Node<E> newNode = new Node<>(null, e, f);
        first = newNode;
        if (f == null) last = newNode;
        else f.prev = newNode;
        size++;
        modCount++;
    }

    void linkLast(E e) {
        Node<E> l = last;
        Node<E> newNode = new Node<>(l, e, null);
        last = newNode;
        if (l == null) first = newNode;
        else l.next = newNode;
        size++;
        modCount++;
    }

    void linkBefore(E e, Node<E> succ) {
        Node<E> pred = succ.prev;
        Node<E> newNode = new Node<>(pred, e, succ);
        succ.prev = newNode;
        if (pred == null) first = newNode;
        else pred.next = newNode;
        size++;
        modCount++;
    }

    E unlink(Node<E> x) {
        E element = x.item;
        Node<E> next = x.next;
        Node<E> prev = x.prev;
        if (prev == null) {
            first = next;
        } else {
            prev.next = next;
            x.prev = null;
        }
        if (next == null) {
            last = prev;
        } else {
            next.prev = prev;
            x.next = null;
        }
        x.item = null;
        size--;
        modCount++;
        return element;
    }

    public E getFirst() {
        if (first == null) throw new NoSuchElementException();
        return first.item;
    }

    public E getLast() {
        if (last == null) throw new NoSuchElementException();
        return last.item;
    }

    public E removeFirst() {
        if (first == null) throw new NoSuchElementException();
        return unlink(first);
    }

    public E removeLast() {
        if (last == null) throw new NoSuchElementException();
        return unlink(last);
    }

    public void addFirst(E e) {
        linkFirst(e);
    }

    public void addLast(E e) {
        linkLast(e);
    }

    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    public int size() {
        return size;
    }

    public boolean add(E e) {
        linkLast(e);
        return true;
    }

    public boolean remove(Object o) {
        for (Node<E> x = first; x != null; x = x.next) {
            if (Objects.equals(o, x.item)) {
                unlink(x);
                return true;
            }
        }
        return false;
    }

    public boolean addAll(Collection<? extends E> c) {
        return addAll(size, c);
    }

    /** The collection's elements as toArray gives them, linked in at index. */
    public boolean addAll(int index, Collection<? extends E> c) {
        checkPositionIndex(index);
        Object[] a = c.toArray();
        if (a.length == 0) return false;
        Node<E> succ = index == size ? null : node(index);
        for (Object o : a) {
            @SuppressWarnings("unchecked")
            E e = (E) o;
            if (succ == null) linkLastQuiet(e);
            else linkBeforeQuiet(e, succ);
        }
        modCount++;
        return true;
    }

    private void linkLastQuiet(E e) {
        linkLast(e);
        modCount--;
    }

    private void linkBeforeQuiet(E e, Node<E> succ) {
        linkBefore(e, succ);
        modCount--;
    }

    public void clear() {
        for (Node<E> x = first; x != null; ) {
            Node<E> next = x.next;
            x.item = null;
            x.next = null;
            x.prev = null;
            x = next;
        }
        first = last = null;
        size = 0;
        modCount++;
    }

    public E get(int index) {
        checkElementIndex(index);
        return node(index).item;
    }

    public E set(int index, E element) {
        checkElementIndex(index);
        Node<E> x = node(index);
        E oldVal = x.item;
        x.item = element;
        return oldVal;
    }

    public void add(int index, E element) {
        checkPositionIndex(index);
        if (index == size) linkLast(element);
        else linkBefore(element, node(index));
    }

    public E remove(int index) {
        checkElementIndex(index);
        return unlink(node(index));
    }

    private void checkElementIndex(int index) {
        if (!(index >= 0 && index < size)) throw new IndexOutOfBoundsException("Index: ".concat(Integer.toString(index)).concat(", Size: ").concat(Integer.toString(size)));
    }

    private void checkPositionIndex(int index) {
        if (!(index >= 0 && index <= size)) throw new IndexOutOfBoundsException("Index: ".concat(Integer.toString(index)).concat(", Size: ").concat(Integer.toString(size)));
    }

    Node<E> node(int index) {
        if (index < (size >> 1)) {
            Node<E> x = first;
            for (int i = 0; i < index; i++) x = x.next;
            return x;
        }
        Node<E> x = last;
        for (int i = size - 1; i > index; i--) x = x.prev;
        return x;
    }

    public int indexOf(Object o) {
        int index = 0;
        for (Node<E> x = first; x != null; x = x.next) {
            if (Objects.equals(o, x.item)) return index;
            index++;
        }
        return -1;
    }

    public int lastIndexOf(Object o) {
        int index = size;
        for (Node<E> x = last; x != null; x = x.prev) {
            index--;
            if (Objects.equals(o, x.item)) return index;
        }
        return -1;
    }

    public E peek() {
        return first == null ? null : first.item;
    }

    public E element() {
        return getFirst();
    }

    public E poll() {
        return first == null ? null : unlink(first);
    }

    public E remove() {
        return removeFirst();
    }

    public boolean offer(E e) {
        return add(e);
    }

    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    public E peekFirst() {
        return first == null ? null : first.item;
    }

    public E peekLast() {
        return last == null ? null : last.item;
    }

    public E pollFirst() {
        return first == null ? null : unlink(first);
    }

    public E pollLast() {
        return last == null ? null : unlink(last);
    }

    public void push(E e) {
        addFirst(e);
    }

    public E pop() {
        return removeFirst();
    }

    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }

    public boolean removeLastOccurrence(Object o) {
        for (Node<E> x = last; x != null; x = x.prev) {
            if (Objects.equals(o, x.item)) {
                unlink(x);
                return true;
            }
        }
        return false;
    }

    public Iterator<E> iterator() {
        return new Itr();
    }

    public Iterator<E> descendingIterator() {
        throw new UnsupportedOperationException("not modelled");
    }

    private class Itr implements Iterator<E> {
        private Node<E> lastReturned;
        private Node<E> next = first;
        private int nextIndex;
        private int expectedModCount = modCount;

        public boolean hasNext() {
            return nextIndex < size;
        }

        public E next() {
            checkForComodification();
            if (!hasNext()) throw new NoSuchElementException();
            lastReturned = next;
            next = next.next;
            nextIndex++;
            return lastReturned.item;
        }

        public void remove() {
            checkForComodification();
            if (lastReturned == null) throw new IllegalStateException();
            unlink(lastReturned);
            nextIndex--;
            lastReturned = null;
            expectedModCount++;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            while (modCount == expectedModCount && nextIndex < size) {
                action.accept(next.item);
                lastReturned = next;
                next = next.next;
                nextIndex++;
            }
            checkForComodification();
        }

        final void checkForComodification() {
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
        }
    }

    public Object[] toArray() {
        Object[] result = new Object[size];
        int i = 0;
        for (Node<E> x = first; x != null; x = x.next) result[i++] = x.item;
        return result;
    }
}
