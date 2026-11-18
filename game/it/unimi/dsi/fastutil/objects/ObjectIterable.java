package it.unimi.dsi.fastutil.objects;

/** The stand-in for fastutil's ObjectIterable. */
public interface ObjectIterable<K> extends Iterable<K> {
    @Override
    ObjectIterator<K> iterator();
}
