package java.util;

/** The model of java.util.SortedMap: headMap, tailMap and subMap aren't modelled. */
public interface SortedMap<K, V> extends SequencedMap<K, V> {
    Comparator<? super K> comparator();

    K firstKey();

    K lastKey();
}
