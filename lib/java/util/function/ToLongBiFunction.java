package java.util.function;

/** The model of java.util.function.ToLongBiFunction. */
@FunctionalInterface
public interface ToLongBiFunction<T, U> {
    long applyAsLong(T t, U u);
}
