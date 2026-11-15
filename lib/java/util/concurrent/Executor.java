package java.util.concurrent;

/** The model of java.util.concurrent.Executor. */
public interface Executor {
    void execute(Runnable command);
}
