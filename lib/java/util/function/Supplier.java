package java.util.function;

/** The model of java.util.function.Supplier. */
@FunctionalInterface
public interface Supplier<T> {
    T get();
}
