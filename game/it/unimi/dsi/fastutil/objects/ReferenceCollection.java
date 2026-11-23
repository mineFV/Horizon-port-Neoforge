package it.unimi.dsi.fastutil.objects;

/** The stand-in for fastutil's ReferenceCollection. */
public interface ReferenceCollection<K> extends java.util.Collection<K>, ObjectIterable<K> {
    @Override
    ObjectIterator<K> iterator();
}
