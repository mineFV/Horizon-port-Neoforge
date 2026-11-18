package it.unimi.dsi.fastutil.objects;

import java.util.function.Consumer;

/** Not fastutil's: a collection's elements in its order, one at a time (it isn't split). */
final class HorizonObjectSpliterator<K> implements ObjectSpliterator<K> {
    private final java.util.Spliterator<K> s;

    HorizonObjectSpliterator(java.util.Spliterator<K> s) {
        this.s = s;
    }

    @Override
    public boolean tryAdvance(Consumer<? super K> action) {
        return s.tryAdvance(action);
    }

    @Override
    public java.util.Spliterator<K> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return s.estimateSize();
    }

    @Override
    public int characteristics() {
        return s.characteristics();
    }
}
