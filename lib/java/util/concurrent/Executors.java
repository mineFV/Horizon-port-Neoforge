package java.util.concurrent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * The model of java.util.concurrent.Executors: fixed pools of threads (a single one among them), their threads
 * named "pool-N-thread-M" and started as tasks come, as the JDK's. Not modelled: the scheduled and cached pools,
 * work stealing, invokeAll.
 */
public class Executors {
    private Executors() {}

    public static ExecutorService newSingleThreadExecutor() {
        return new Pool(1, defaultThreadFactory());
    }

    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        if (threadFactory == null) throw new NullPointerException();
        return new Pool(1, threadFactory);
    }

    public static ExecutorService newFixedThreadPool(int nThreads) {
        if (nThreads <= 0) throw new IllegalArgumentException();
        return new Pool(nThreads, defaultThreadFactory());
    }

    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        if (nThreads <= 0) throw new IllegalArgumentException();
        if (threadFactory == null) throw new NullPointerException();
        return new Pool(nThreads, threadFactory);
    }

    public static <T> Callable<T> callable(Runnable task, T result) {
        if (task == null) throw new NullPointerException();
        return () -> {
            task.run();
            return result;
        };
    }

    public static Callable<Object> callable(Runnable task) {
        return callable(task, null);
    }

    private static int poolNumber = 1;

    private static synchronized int nextPoolNumber() {
        return poolNumber++;
    }

    public static ThreadFactory defaultThreadFactory() {
        return new DefaultThreadFactory();
    }

    private static final class DefaultThreadFactory implements ThreadFactory {
        private final String namePrefix = "pool-".concat(Integer.toString(nextPoolNumber())).concat("-thread-");
        private int threadNumber = 1;

        public Thread newThread(Runnable r) {
            int number;
            synchronized (this) {
                number = threadNumber++;
            }
            Thread t = new Thread(r, namePrefix.concat(Integer.toString(number)));
            if (t.isDaemon()) t.setDaemon(false);
            return t;
        }
    }

    /**
     * A fixed pool: a new thread for each task until there are nThreads of them, the tasks after queued; its
     * threads take tasks until the pool is shut down and the queue empty. A task that throws (by execute) ends its
     * thread, which another replaces, as the JDK's do.
     */
    static final class Pool implements ExecutorService {
        private final int size;
        private final ThreadFactory factory;
        private final ArrayDeque<Runnable> queue = new ArrayDeque<>();
        private int workers;
        private boolean shutdown;

        Pool(int size, ThreadFactory factory) {
            this.size = size;
            this.factory = factory;
        }

        public void execute(Runnable command) {
            if (command == null) throw new NullPointerException();
            synchronized (this) {
                if (shutdown) throw new RejectedExecutionException("Task ".concat(String.valueOf(command)).concat(" rejected from ").concat(String.valueOf(this)));
                if (workers < size) {
                    workers++;
                    startWorker(command);
                    return;
                }
                queue.addLast(command);
                notifyAll();
            }
        }

        private void startWorker(Runnable first) {
            Thread t = factory.newThread(() -> work(first));
            if (t == null) throw new RejectedExecutionException();
            t.start();
        }

        private void work(Runnable first) {
            Runnable task = first;
            boolean failed = true;
            try {
                while (task != null) {
                    task.run();
                    task = take();
                }
                failed = false;
            } finally {
                synchronized (this) {
                    workers--;
                    if (failed && !shutdown && workers < size && !queue.isEmpty()) {
                        workers++;
                        startWorker(queue.pollFirst());
                    }
                    notifyAll();
                }
            }
        }

        /** The next task, or null once shut down with nothing left. */
        private synchronized Runnable take() {
            while (queue.isEmpty()) {
                if (shutdown) return null;
                try {
                    wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }
            return queue.pollFirst();
        }

        public synchronized void shutdown() {
            shutdown = true;
            notifyAll();
        }

        public synchronized List<Runnable> shutdownNow() {
            shutdown = true;
            List<Runnable> left = new ArrayList<>(queue);
            queue.clear();
            notifyAll();
            return left;
        }

        public synchronized boolean isShutdown() {
            return shutdown;
        }

        public synchronized boolean isTerminated() {
            return shutdown && workers == 0;
        }

        public synchronized boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            long end = System.nanoTime() + unit.toNanos(timeout);
            while (!(shutdown && workers == 0)) {
                long left = (end - System.nanoTime()) / 1_000_000L;
                if (left <= 0) return false;
                wait(left);
            }
            return true;
        }

        public <T> Future<T> submit(Callable<T> task) {
            if (task == null) throw new NullPointerException();
            FutureTask<T> f = new FutureTask<>(task);
            execute(f);
            return f;
        }

        public <T> Future<T> submit(Runnable task, T result) {
            if (task == null) throw new NullPointerException();
            FutureTask<T> f = new FutureTask<>(task, result);
            execute(f);
            return f;
        }

        public Future<?> submit(Runnable task) {
            if (task == null) throw new NullPointerException();
            FutureTask<Object> f = new FutureTask<>(task, null);
            execute(f);
            return f;
        }
    }
}
