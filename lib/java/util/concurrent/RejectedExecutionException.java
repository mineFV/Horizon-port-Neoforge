package java.util.concurrent;

/** The model of java.util.concurrent.RejectedExecutionException. */
public class RejectedExecutionException extends RuntimeException {
    public RejectedExecutionException() {}

    public RejectedExecutionException(String message) {
        super(message);
    }

    public RejectedExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public RejectedExecutionException(Throwable cause) {
        super(cause);
    }
}
