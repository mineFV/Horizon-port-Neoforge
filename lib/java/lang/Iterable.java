package java.lang;

import java.util.Iterator;
import java.util.function.Consumer;

/** The model of java.lang.Iterable. */
public interface Iterable<T> {
    Iterator<T> iterator();

    default void forEach(Consumer<? super T> action) {
        java.util.Objects.requireNonNull(action);
        for (T t : this) action.accept(t);
    }

    default java.util.Spliterator<T> spliterator() {
        return java.util.Spliterators.spliteratorUnknownSize(iterator(), 0);
    }
}
