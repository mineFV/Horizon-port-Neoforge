package it.unimi.dsi.fastutil;

/** The stand-in for fastutil's Hash: its load factors and initial size. */
public interface Hash {
    int DEFAULT_INITIAL_SIZE = 16;
    float DEFAULT_LOAD_FACTOR = .75f;
    float FAST_LOAD_FACTOR = .5f;
    float VERY_FAST_LOAD_FACTOR = .25f;
}
