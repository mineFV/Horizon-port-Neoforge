package java.util.function;

/** The model of java.util.function.BiConsumer. */
@FunctionalInterface
public interface BiConsumer<T, U> {
    void accept(T t, U u);
}
