package java.util.concurrent;

/** The model of java.util.concurrent.CompletionException. */
public class CompletionException extends RuntimeException {
    protected CompletionException() {}

    protected CompletionException(String message) {
        super(message);
    }

    public CompletionException(String message, Throwable cause) {
        super(message, cause);
    }

    public CompletionException(Throwable cause) {
        super(cause);
    }
}
