package it.unimi.dsi.fastutil.chars;

/** The stand-in for fastutil's AbstractCharCollection: what a collection of chars does from its iterator, and its text {a, b} (observed). */
public abstract class AbstractCharCollection extends java.util.AbstractCollection<Character> implements CharCollection {
    protected AbstractCharCollection() {}

    @Override
    public abstract CharIterator iterator();

    @Override
    public boolean add(char k) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(char k) {
        CharIterator i = iterator();
        while (i.hasNext()) if (k == i.nextChar()) return true;
        return false;
    }

    @Override
    public boolean rem(char k) {
        CharIterator i = iterator();
        while (i.hasNext()) {
            if (k == i.nextChar()) {
                i.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean addAll(java.util.Collection<? extends Character> c) {
        return super.addAll(c);
    }

    @Override
    public boolean containsAll(java.util.Collection<?> c) {
        return super.containsAll(c);
    }

    @Override
    public boolean removeAll(java.util.Collection<?> c) {
        return super.removeAll(c);
    }

    @Override
    public boolean retainAll(java.util.Collection<?> c) {
        return super.retainAll(c);
    }

    @Override
    public boolean add(Character key) {
        return CharCollection.super.add(key);
    }

    @Override
    public boolean contains(Object key) {
        return CharCollection.super.contains(key);
    }

    @Override
    public boolean remove(Object key) {
        return CharCollection.super.remove(key);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("{");
        CharIterator i = iterator();
        boolean first = true;
        while (i.hasNext()) {
            if (first) first = false;
            else s.append(", ");
            s.append(String.valueOf(i.nextChar()));
        }
        return s.append('}').toString();
    }
}
