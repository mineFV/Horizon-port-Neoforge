package it.unimi.dsi.fastutil.booleans;

import java.util.Objects;
import java.util.function.Consumer;

/** The stand-in for fastutil's BooleanConsumer: a Consumer of booleans, unboxing a Boolean given (observed against fastutil). */
@FunctionalInterface
public interface BooleanConsumer extends Consumer<Boolean> {
    void accept(boolean t);

    @Override
    default void accept(Boolean t) {
        accept(t.booleanValue());
    }

    default BooleanConsumer andThen(BooleanConsumer after) {
        Objects.requireNonNull(after);
        return t -> {
            accept(t);
            after.accept(t);
        };
    }

    @Override
    default Consumer<Boolean> andThen(Consumer<? super Boolean> after) {
        return Consumer.super.andThen(after);
    }
}
