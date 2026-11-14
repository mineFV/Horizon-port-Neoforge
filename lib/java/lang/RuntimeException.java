package java.lang;

/** The model of java.lang.RuntimeException. */
public class RuntimeException extends Exception {
    public RuntimeException() {}

    public RuntimeException(String message) {
        super(message);
    }

    public RuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuntimeException(Throwable cause) {
        super(cause);
    }
}
