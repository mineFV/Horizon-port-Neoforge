package java.lang;

/**
 * The model of java.lang.Thread: a thread of the system's for each one started, which runs run() as its current
 * thread; join, sleep, currentThread, names, daemons, thread locals. A thread the translation didn't start (the
 * game's) gets an object the first time it asks for its current thread, named as the system names it. An exception
 * run() lets out is printed to the standard error as the JDK's default handler does (without the stack trace, which
 * isn't kept). Ids count from 1 for the first thread asked about: a JVM's own threads take ids of their own, so no
 * program can count on them. Not modelled: interrupts, priorities, groups, handlers, virtual threads.
 */
public class Thread implements Runnable {
    private static int threadInitNumber;
    private static long threadSeqNumber;

    private final long tid;
    private volatile String name;
    private final Runnable target;
    private boolean daemon;
    /** 0: new, 1: alive, 2: terminated. */
    private int state;
    private java.util.IdentityHashMap<ThreadLocal<?>, Object> locals;

    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }

    private static synchronized long nextThreadId() {
        return ++threadSeqNumber;
    }

    public Thread() {
        this(null, "Thread-".concat(Integer.toString(nextThreadNum())));
    }

    public Thread(Runnable task) {
        this(task, "Thread-".concat(Integer.toString(nextThreadNum())));
    }

    public Thread(String name) {
        this(null, name);
    }

    public Thread(Runnable task, String name) {
        if (name == null) throw new NullPointerException("'name' is null");
        this.name = name;
        this.target = task;
        this.tid = nextThreadId();
        Thread parent = currentThread();
        this.daemon = parent != null && parent.daemon;
    }

    /** The object of a thread the translation didn't start, alive, named as the system names it. */
    static Thread jForeign() {
        Thread t = new Thread(null, jSystemName(), false);
        t.state = 1;
        return t;
    }

    private Thread(Runnable task, String name, boolean inherit) {
        this.name = name;
        this.target = task;
        this.tid = nextThreadId();
    }

    public static native Thread currentThread();

    public void run() {
        Runnable task = target;
        if (task != null) task.run();
    }

    public void start() {
        synchronized (this) {
            if (state != 0) throw new IllegalThreadStateException();
            state = 1;
        }
        jStart();
    }

    /** What the started thread runs: run(), then the thread dead and its joiners notified. */
    static void jRunThread(Thread t) {
        try {
            t.run();
        } catch (Throwable e) {
            jPrintError("Exception in thread \"".concat(t.getName()).concat("\" ").concat(e.toString()));
        } finally {
            synchronized (t) {
                t.state = 2;
                t.notifyAll();
            }
        }
    }

    public final boolean isAlive() {
        synchronized (this) {
            return state == 1;
        }
    }

    public final void join() throws InterruptedException {
        synchronized (this) {
            while (state == 1) wait(0);
        }
    }

    public final void join(long millis) throws InterruptedException {
        if (millis < 0) throw new IllegalArgumentException("timeout value is negative");
        if (millis == 0) {
            join();
            return;
        }
        long end = System.nanoTime() + millis * 1_000_000L;
        synchronized (this) {
            while (state == 1) {
                long left = (end - System.nanoTime()) / 1_000_000L;
                if (left <= 0) break;
                wait(left);
            }
        }
    }

    public static void sleep(long millis) throws InterruptedException {
        if (millis < 0) throw new IllegalArgumentException("timeout value is negative");
        jSleep(millis);
    }

    public static void onSpinWait() {}

    public static void yield() {}

    public final String getName() {
        return name;
    }

    public final synchronized void setName(String name) {
        if (name == null) throw new NullPointerException("'name' is null");
        this.name = name;
    }

    public final boolean isDaemon() {
        return daemon;
    }

    public final void setDaemon(boolean on) {
        if (isAlive()) throw new IllegalThreadStateException();
        daemon = on;
    }

    @Deprecated
    public long getId() {
        return tid;
    }

    public final long threadId() {
        return tid;
    }

    public final int getPriority() {
        return 5;
    }

    public String toString() {
        return "Thread[#".concat(Long.toString(tid)).concat(",").concat(getName()).concat(",5,main]");
    }

    /** This thread's ThreadLocal values. */
    java.util.IdentityHashMap<ThreadLocal<?>, Object> jLocals() {
        if (locals == null) locals = new java.util.IdentityHashMap<>();
        return locals;
    }

    private native void jStart();

    private static native String jSystemName();

    private static native void jSleep(long millis);

    private static native void jPrintError(String text);
}
