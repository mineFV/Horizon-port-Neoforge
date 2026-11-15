package java.util.concurrent;

/** The model of java.util.concurrent.RunnableFuture. */
public interface RunnableFuture<V> extends Runnable, Future<V> {
    void run();
}
