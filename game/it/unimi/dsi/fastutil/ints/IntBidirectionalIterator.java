package it.unimi.dsi.fastutil.ints;

/** The stand-in for fastutil's IntBidirectionalIterator. */
public interface IntBidirectionalIterator extends IntIterator, it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator<Integer> {
    int previousInt();

    @Override
    default Integer previous() {
        return Integer.valueOf(previousInt());
    }

    @Override
    default int back(int n) {
        int i = n;
        while (i-- != 0 && hasPrevious()) previousInt();
        return n - i - 1;
    }

    @Override
    default int skip(int n) {
        return IntIterator.super.skip(n);
    }
}
