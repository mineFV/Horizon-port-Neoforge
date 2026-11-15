package java.util.function;

/** The model of java.util.function.ToDoubleBiFunction. */
@FunctionalInterface
public interface ToDoubleBiFunction<T, U> {
    double applyAsDouble(T t, U u);
}
