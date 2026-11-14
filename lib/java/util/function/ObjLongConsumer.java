package java.util.function;

/** The model of java.util.function.ObjLongConsumer. */
@FunctionalInterface
public interface ObjLongConsumer<T> {
    void accept(T t, long value);
}
