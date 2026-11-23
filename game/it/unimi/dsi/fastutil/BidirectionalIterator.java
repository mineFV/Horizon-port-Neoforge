package it.unimi.dsi.fastutil;

/** The stand-in for fastutil's BidirectionalIterator: an iterator that goes back too. */
public interface BidirectionalIterator<K> extends java.util.Iterator<K> {
    K previous();

    boolean hasPrevious();
}
