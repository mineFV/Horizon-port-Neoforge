package java.util.function;

/** The model of java.util.function.ToLongFunction. */
@FunctionalInterface
public interface ToLongFunction<T> {
    long applyAsLong(T value);
}
