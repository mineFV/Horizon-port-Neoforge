package java.util;

/**
 * The model of the JDK's java.util.Comparators: natural order as one object. The JDK's is an enum; this one is a
 * class, since the model's Enum has no valueOf(Class, String) (it needs Class objects).
 */
final class Comparators {
    private Comparators() {}

    static final class NaturalOrderComparator implements Comparator<Comparable<Object>> {
        static final NaturalOrderComparator INSTANCE = new NaturalOrderComparator();

        private NaturalOrderComparator() {}

        public int compare(Comparable<Object> c1, Comparable<Object> c2) {
            return c1.compareTo(c2);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public Comparator<Comparable<Object>> reversed() {
            return (Comparator) Collections.reverseOrder();
        }
    }
}
