package it.unimi.dsi.fastutil.objects;

/** The stand-in for fastutil's ReferenceSet. */
public interface ReferenceSet<K> extends ReferenceCollection<K>, java.util.Set<K> {
    @Override
    ObjectIterator<K> iterator();
}
