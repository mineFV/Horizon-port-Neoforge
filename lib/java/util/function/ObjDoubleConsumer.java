package java.util.function;

/** The model of java.util.function.ObjDoubleConsumer. */
@FunctionalInterface
public interface ObjDoubleConsumer<T> {
    void accept(T t, double value);
}
