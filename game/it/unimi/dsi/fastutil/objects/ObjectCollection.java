package it.unimi.dsi.fastutil.objects;

/** The stand-in for fastutil's ObjectCollection. */
public interface ObjectCollection<K> extends java.util.Collection<K>, ObjectIterable<K> {
    @Override
    ObjectIterator<K> iterator();
}
