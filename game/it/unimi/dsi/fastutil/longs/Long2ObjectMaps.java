package it.unimi.dsi.fastutil.longs;

/** The stand-in for fastutil's Long2ObjectMaps: a map's fast iterator (its other helpers aren't here yet). */
public final class Long2ObjectMaps {
    private Long2ObjectMaps() {}

    /** Observed: the entries' fast iterator when they have one, else their iterator. */
    public static <V> it.unimi.dsi.fastutil.objects.ObjectIterator<Long2ObjectMap.Entry<V>> fastIterator(Long2ObjectMap<V> map) {
        it.unimi.dsi.fastutil.objects.ObjectSet<Long2ObjectMap.Entry<V>> entries = map.long2ObjectEntrySet();
        return entries instanceof Long2ObjectMap.FastEntrySet<V> f ? f.fastIterator() : entries.iterator();
    }
}
