package java.util.function;

/** The model of java.util.function.ToDoubleFunction. */
@FunctionalInterface
public interface ToDoubleFunction<T> {
    double applyAsDouble(T value);
}
