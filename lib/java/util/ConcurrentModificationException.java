package java.util;

/** The model of java.util.ConcurrentModificationException: what a fail-fast iterator throws. */
public class ConcurrentModificationException extends RuntimeException {
    public ConcurrentModificationException() {
        super();
    }

    public ConcurrentModificationException(String message) {
        super(message);
    }

    public ConcurrentModificationException(Throwable cause) {
        super(cause);
    }

    public ConcurrentModificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
