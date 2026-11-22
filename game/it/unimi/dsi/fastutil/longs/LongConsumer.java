package it.unimi.dsi.fastutil.longs;

/** The stand-in for fastutil's LongConsumer: a consumer of longs, and of boxed ones as fastutil gives them. */
@FunctionalInterface
public interface LongConsumer extends java.util.function.Consumer<Long>, java.util.function.LongConsumer {
    @Override
    default void accept(Long t) {
        accept(t.longValue());
    }
}
