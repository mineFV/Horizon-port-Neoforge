package it.unimi.dsi.fastutil.objects;

/** The stand-in for fastutil's ObjectSet. */
public interface ObjectSet<K> extends ObjectCollection<K>, java.util.Set<K> {
    @Override
    ObjectIterator<K> iterator();
}
