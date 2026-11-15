package java.util.concurrent;

/** The model of java.util.concurrent.Callable. */
@FunctionalInterface
public interface Callable<V> {
    V call() throws Exception;
}
