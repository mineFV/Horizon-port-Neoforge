package java.lang;

/** The model of java.lang.AssertionError. */
public class AssertionError extends Error {
    public AssertionError() {}

    public AssertionError(Object detailMessage) {
        super(String.valueOf(detailMessage));
        if (detailMessage instanceof Throwable t) initCause(t);
    }

    public AssertionError(String message, Throwable cause) {
        super(message, cause);
    }
}
