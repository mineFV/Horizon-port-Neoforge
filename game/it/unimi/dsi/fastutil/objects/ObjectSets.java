package it.unimi.dsi.fastutil.objects;

/** The stand-in for fastutil's ObjectSets: the empty set alone. */
public final class ObjectSets {
    private ObjectSets() {}

    /** A set of nothing: equal to any empty set, its removals refused, one for all. */
    public static class EmptySet<K> extends ObjectCollections.EmptyCollection<K> implements ObjectSet<K>, java.io.Serializable, Cloneable {
        protected EmptySet() {}

        @Override
        public boolean remove(Object ok) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object clone() {
            return EMPTY_SET;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof java.util.Set<?> s && s.isEmpty();
        }
    }

    @SuppressWarnings("rawtypes")
    public static final EmptySet EMPTY_SET = new EmptySet();

    @SuppressWarnings("unchecked")
    public static <K> ObjectSet<K> emptySet() {
        return EMPTY_SET;
    }
}
