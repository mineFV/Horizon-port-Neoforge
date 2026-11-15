package java.util;

/**
 * The model of java.util.EnumSet: a set of one enum's constants, as bits by ordinal, iterated in their order. As the
 * JDK's, an iterator walks the set as it was when it began (it isn't fail-fast), and the set is a RegularEnumSet for an
 * enum of 64 constants or fewer, a JumboEnumSet above. clone is modelled as the copy copyOf makes.
 */
public abstract class EnumSet<E extends Enum<E>> extends AbstractSet<E> implements Cloneable, java.io.Serializable {
    final Class<E> elementType;
    final Enum<?>[] universe;
    /** The constants in the set, a bit for each ordinal. */
    long[] elements;

    EnumSet(Class<E> elementType, Enum<?>[] universe) {
        this.elementType = elementType;
        this.universe = universe;
        this.elements = new long[(universe.length + 63) >>> 6];
    }

    public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType) {
        Enum<?>[] universe = elementType.getEnumConstants();
        if (universe == null) throw new ClassCastException(String.valueOf(elementType).concat(" not an enum"));
        if (universe.length <= 64) return new RegularEnumSet<>(elementType, universe);
        return new JumboEnumSet<>(elementType, universe);
    }

    public static <E extends Enum<E>> EnumSet<E> allOf(Class<E> elementType) {
        EnumSet<E> result = noneOf(elementType);
        for (int i = 0; i < result.universe.length; i++) result.elements[i >>> 6] |= 1L << i;
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> copyOf(EnumSet<E> s) {
        return s.copy();
    }

    public static <E extends Enum<E>> EnumSet<E> copyOf(Collection<E> c) {
        if (c instanceof EnumSet<E> s) return s.copy();
        if (c.isEmpty()) throw new IllegalArgumentException("Collection is empty");
        Iterator<E> i = c.iterator();
        E first = i.next();
        EnumSet<E> result = EnumSet.of(first);
        while (i.hasNext()) result.add(i.next());
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> complementOf(EnumSet<E> s) {
        EnumSet<E> result = s.copy();
        for (int i = 0; i < result.universe.length; i++) result.elements[i >>> 6] ^= 1L << i;
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1) {
        EnumSet<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2) {
        EnumSet<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2, E e3) {
        EnumSet<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2, E e3, E e4) {
        EnumSet<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        result.add(e4);
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2, E e3, E e4, E e5) {
        EnumSet<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        result.add(e4);
        result.add(e5);
        return result;
    }

    @SafeVarargs
    public static <E extends Enum<E>> EnumSet<E> of(E first, E... rest) {
        EnumSet<E> result = noneOf(first.getDeclaringClass());
        result.add(first);
        for (E e : rest) result.add(e);
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> range(E from, E to) {
        if (from.compareTo(to) > 0) throw new IllegalArgumentException(String.valueOf(from).concat(" > ").concat(String.valueOf(to)));
        EnumSet<E> result = noneOf(from.getDeclaringClass());
        for (int i = from.ordinal(); i <= to.ordinal(); i++) result.elements[i >>> 6] |= 1L << i;
        return result;
    }

    /** What clone gives: another set of the same constants. */
    EnumSet<E> copy() {
        EnumSet<E> result = noneOf(elementType);
        for (int i = 0; i < elements.length; i++) result.elements[i] = elements[i];
        return result;
    }

    final void typeCheck(E e) {
        Class<?> eClass = e.getClass();
        if (eClass != elementType && eClass.getSuperclass() != elementType) {
            throw new ClassCastException(String.valueOf(eClass).concat(" != ").concat(String.valueOf(elementType)));
        }
    }

    private boolean isMember(Object o) {
        if (o == null) return false;
        Class<?> c = o.getClass();
        return c == elementType || c.getSuperclass() == elementType;
    }

    private boolean has(int ordinal) {
        return (elements[ordinal >>> 6] & (1L << ordinal)) != 0;
    }

    public int size() {
        int n = 0;
        for (long word : elements) n += Long.bitCount(word);
        return n;
    }

    public boolean isEmpty() {
        for (long word : elements) if (word != 0) return false;
        return true;
    }

    public boolean contains(Object e) {
        return isMember(e) && has(((Enum<?>) e).ordinal());
    }

    public boolean add(E e) {
        typeCheck(e);
        int o = e.ordinal();
        boolean was = has(o);
        elements[o >>> 6] |= 1L << o;
        return !was;
    }

    public boolean remove(Object e) {
        if (!isMember(e)) return false;
        int o = ((Enum<?>) e).ordinal();
        boolean was = has(o);
        elements[o >>> 6] &= ~(1L << o);
        return was;
    }

    public void clear() {
        for (int i = 0; i < elements.length; i++) elements[i] = 0;
    }

    public Iterator<E> iterator() {
        return new Iterator<E>() {
            /** The constants not given yet, as the set was when the walk began. */
            private final long[] unseen = Arrays.copyOf(elements, elements.length);
            private int last = -1;

            public boolean hasNext() {
                for (long word : unseen) if (word != 0) return true;
                return false;
            }

            @SuppressWarnings("unchecked")
            public E next() {
                for (int w = 0; w < unseen.length; w++) {
                    if (unseen[w] != 0) {
                        int bit = Long.numberOfTrailingZeros(unseen[w]);
                        unseen[w] &= ~(1L << bit);
                        last = (w << 6) + bit;
                        return (E) universe[last];
                    }
                }
                throw new NoSuchElementException();
            }

            public void remove() {
                if (last < 0) throw new IllegalStateException();
                elements[last >>> 6] &= ~(1L << last);
                last = -1;
            }
        };
    }
}
