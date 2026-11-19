package it.unimi.dsi.fastutil.chars;

import it.unimi.dsi.fastutil.objects.AbstractObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/** The stand-in for fastutil's AbstractChar2ObjectMap: a map's views, text, equality and hash from its entries (observed, `standins`). */
public abstract class AbstractChar2ObjectMap<V> extends AbstractChar2ObjectFunction<V> implements Char2ObjectMap<V>, java.io.Serializable {
    protected AbstractChar2ObjectMap() {}

    @Override
    public boolean containsKey(char k) {
        for (Entry<V> e : char2ObjectEntrySet()) if (e.getCharKey() == k) return true;
        return false;
    }

    @Override
    public boolean containsValue(Object v) {
        for (Entry<V> e : char2ObjectEntrySet()) if (java.util.Objects.equals(e.getValue(), v)) return true;
        return false;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void putAll(java.util.Map<? extends Character, ? extends V> m) {
        for (java.util.Map.Entry<? extends Character, ? extends V> e : m.entrySet()) put(e.getKey(), e.getValue());
    }

    /** Its keys, as its entries give them. */
    @Override
    public CharSet keySet() {
        return new AbstractCharSet() {
            @Override
            public boolean contains(char k) {
                return containsKey(k);
            }

            @Override
            public int size() {
                return AbstractChar2ObjectMap.this.size();
            }

            @Override
            public void clear() {
                AbstractChar2ObjectMap.this.clear();
            }

            @Override
            public CharIterator iterator() {
                java.util.Iterator<Entry<V>> i = char2ObjectEntrySet().iterator();
                return new CharIterator() {
                    @Override
                    public char nextChar() {
                        return i.next().getCharKey();
                    }

                    @Override
                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    @Override
                    public void remove() {
                        i.remove();
                    }
                };
            }
        };
    }

    /** Its values, as its entries give them. */
    @Override
    public ObjectCollection<V> values() {
        return new AbstractObjectCollection<V>() {
            @Override
            public boolean contains(Object k) {
                return containsValue(k);
            }

            @Override
            public int size() {
                return AbstractChar2ObjectMap.this.size();
            }

            @Override
            public void clear() {
                AbstractChar2ObjectMap.this.clear();
            }

            @Override
            public ObjectIterator<V> iterator() {
                java.util.Iterator<Entry<V>> i = char2ObjectEntrySet().iterator();
                return new ObjectIterator<V>() {
                    @Override
                    public V next() {
                        return i.next().getValue();
                    }

                    @Override
                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    @Override
                    public void remove() {
                        i.remove();
                    }
                };
            }
        };
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (Entry<V> e : char2ObjectEntrySet()) h += e.hashCode();
        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof java.util.Map<?, ?> m)) return false;
        if (m.size() != size()) return false;
        return char2ObjectEntrySet().containsAll(m.entrySet());
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("{");
        boolean first = true;
        for (Entry<V> e : char2ObjectEntrySet()) {
            if (first) first = false;
            else s.append(", ");
            s.append(String.valueOf(e.getCharKey())).append("=>").append(this == e.getValue() ? "(this map)" : String.valueOf(e.getValue()));
        }
        return s.append('}').toString();
    }

    /** The stand-in for AbstractChar2ObjectMap.BasicEntry: a char key and a value. */
    public static class BasicEntry<V> implements Entry<V> {
        protected char key;
        protected V value;

        public BasicEntry() {}

        public BasicEntry(char key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public char getCharKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof java.util.Map.Entry<?, ?> e && e.getKey() instanceof Character c && key == c.charValue() && java.util.Objects.equals(value, e.getValue());
        }

        @Override
        public int hashCode() {
            return key ^ (value == null ? 0 : value.hashCode());
        }

        @Override
        public String toString() {
            return key + "->" + value;
        }
    }
}
