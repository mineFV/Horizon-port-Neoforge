package java.lang;

/** The model of java.lang.LinkageError. */
public class LinkageError extends Error {
    public LinkageError() {}

    public LinkageError(String message) {
        super(message);
    }

    public LinkageError(String message, Throwable cause) {
        super(message, cause);
    }
}
