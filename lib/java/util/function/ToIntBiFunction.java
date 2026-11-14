package java.util.function;

/** The model of java.util.function.ToIntBiFunction. */
@FunctionalInterface
public interface ToIntBiFunction<T, U> {
    int applyAsInt(T t, U u);
}
