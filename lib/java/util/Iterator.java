package java.util;

import java.util.function.Consumer;

/** The model of java.util.Iterator. */
public interface Iterator<E> {
    boolean hasNext();

    E next();

    default void remove() {
        throw new UnsupportedOperationException("remove");
    }

    default void forEachRemaining(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        while (hasNext()) action.accept(next());
    }
}
