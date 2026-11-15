package java.util.concurrent;

/**
 * The model of java.util.concurrent.FutureTask: run computes once; get waits for the result, throws what the task
 * threw in an ExecutionException, or a CancellationException once cancelled. Its state is guarded by its monitor.
 */
public class FutureTask<V> implements RunnableFuture<V> {
    /** 0: new, 1: running, 2: done, 3: failed, 4: cancelled. */
    private int state;
    private Callable<V> callable;
    private Object outcome;

    public FutureTask(Callable<V> callable) {
        if (callable == null) throw new NullPointerException();
        this.callable = callable;
    }

    public FutureTask(Runnable runnable, V result) {
        this(Executors.callable(runnable, result));
    }

    public void run() {
        Callable<V> c;
        synchronized (this) {
            if (state != 0) return;
            state = 1;
            c = callable;
        }
        try {
            V result = c.call();
            set(2, result);
        } catch (Throwable ex) {
            set(3, ex);
        }
    }

    private synchronized void set(int s, Object value) {
        if (state != 1) return;
        state = s;
        outcome = value;
        callable = null;
        notifyAll();
        done();
    }

    protected void done() {}

    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (state != 0) return false;
        state = 4;
        callable = null;
        notifyAll();
        done();
        return true;
    }

    public synchronized boolean isCancelled() {
        return state == 4;
    }

    public synchronized boolean isDone() {
        return state >= 2;
    }

    public synchronized V get() throws InterruptedException, ExecutionException {
        while (state < 2) wait();
        return report();
    }

    public synchronized V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null) throw new NullPointerException();
        long end = System.nanoTime() + unit.toNanos(timeout);
        while (state < 2) {
            long left = (end - System.nanoTime()) / 1_000_000L;
            if (left <= 0) throw new TimeoutException();
            wait(left);
        }
        return report();
    }

    @SuppressWarnings("unchecked")
    private V report() throws ExecutionException {
        if (state == 2) return (V) outcome;
        if (state == 4) throw new CancellationException();
        throw new ExecutionException((Throwable) outcome);
    }

    public String toString() {
        String status;
        synchronized (this) {
            status = state == 2 ? "[Completed normally]" : state == 3 ? "[Completed exceptionally: ".concat(String.valueOf(outcome)).concat("]")
                    : state == 4 ? "[Cancelled]" : "[Not completed]";
        }
        return super.toString().concat(status);
    }
}
