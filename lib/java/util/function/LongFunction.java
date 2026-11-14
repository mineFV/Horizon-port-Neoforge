package java.util.function;

/** The model of java.util.function.LongFunction. */
@FunctionalInterface
public interface LongFunction<R> {
    R apply(long value);
}
