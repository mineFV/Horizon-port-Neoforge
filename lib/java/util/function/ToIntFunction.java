package java.util.function;

/** The model of java.util.function.ToIntFunction. */
@FunctionalInterface
public interface ToIntFunction<T> {
    int applyAsInt(T value);
}
