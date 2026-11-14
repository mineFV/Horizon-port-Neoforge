package java.util.function;

/** The model of java.util.function.LongBinaryOperator. */
@FunctionalInterface
public interface LongBinaryOperator {
    long applyAsLong(long left, long right);
}
