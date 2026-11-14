package java.lang;

/** The model of java.lang.IncompatibleClassChangeError. */
public class IncompatibleClassChangeError extends LinkageError {
    public IncompatibleClassChangeError() {}

    public IncompatibleClassChangeError(String message) {
        super(message);
    }
}
