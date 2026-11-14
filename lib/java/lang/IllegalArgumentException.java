package java.lang;

/** The model of java.lang.IllegalArgumentException. */
public class IllegalArgumentException extends RuntimeException {
    public IllegalArgumentException() {}

    public IllegalArgumentException(String message) {
        super(message);
    }

    public IllegalArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalArgumentException(Throwable cause) {
        super(cause);
    }
}
