package java.util.concurrent;

/** The model of java.util.concurrent.ThreadFactory. */
public interface ThreadFactory {
    Thread newThread(Runnable r);
}
