package java.util.function;

/** The model of java.util.function.BiPredicate. */
@FunctionalInterface
public interface BiPredicate<T, U> {
    boolean test(T t, U u);
}
