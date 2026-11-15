package java.util.concurrent;

import java.util.List;

/** The model of java.util.concurrent.ExecutorService: invokeAll and invokeAny aren't modelled. */
public interface ExecutorService extends Executor, AutoCloseable {
    void shutdown();

    List<Runnable> shutdownNow();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    <T> Future<T> submit(Callable<T> task);

    <T> Future<T> submit(Runnable task, T result);

    Future<?> submit(Runnable task);

    /** Shut down, then waits for the tasks to end, as the JDK's default. */
    @Override
    default void close() {
        shutdown();
        boolean terminated = isTerminated();
        while (!terminated) {
            try {
                terminated = awaitTermination(1L, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                shutdownNow();
            }
        }
    }
}
