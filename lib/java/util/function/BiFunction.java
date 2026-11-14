package java.util.function;

/** The model of java.util.function.BiFunction. */
@FunctionalInterface
public interface BiFunction<T, U, R> {
    R apply(T t, U u);
}
