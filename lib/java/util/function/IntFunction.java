package java.util.function;

/** The model of java.util.function.IntFunction. */
@FunctionalInterface
public interface IntFunction<R> {
    R apply(int value);
}
