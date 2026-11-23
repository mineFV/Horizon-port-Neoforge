package it.unimi.dsi.fastutil.objects;

/** The stand-in for fastutil's ObjectBidirectionalIterator: back(n) goes back n elements, or as many as there are. */
public interface ObjectBidirectionalIterator<K> extends ObjectIterator<K>, it.unimi.dsi.fastutil.BidirectionalIterator<K> {
    default int back(int n) {
        int i = n;
        while (i-- != 0 && hasPrevious()) previous();
        return n - i - 1;
    }

    @Override
    default int skip(int n) {
        return ObjectIterator.super.skip(n);
    }
}
