package java.util.function;

/** The model of java.util.function.ObjIntConsumer. */
@FunctionalInterface
public interface ObjIntConsumer<T> {
    void accept(T t, int value);
}
