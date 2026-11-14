package java.util.function;

/** The model of java.util.function.Predicate. */
@FunctionalInterface
public interface Predicate<T> {
    boolean test(T t);

    default Predicate<T> and(Predicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (t) -> test(t) && other.test(t);
    }

    default Predicate<T> negate() {
        return (t) -> !test(t);
    }

    default Predicate<T> or(Predicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (t) -> test(t) || other.test(t);
    }

    @SuppressWarnings("unchecked")
    static <T> Predicate<T> not(Predicate<? super T> target) {
        java.util.Objects.requireNonNull(target);
        return (Predicate<T>) target.negate();
    }
}
