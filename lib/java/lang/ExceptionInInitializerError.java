package java.lang;

/** The model of java.lang.ExceptionInInitializerError: what a class initializer threw is its cause. */
public class ExceptionInInitializerError extends LinkageError {
    public ExceptionInInitializerError() {
        initCause(null);
    }

    public ExceptionInInitializerError(Throwable thrown) {
        super(null, thrown);
    }

    public ExceptionInInitializerError(String s) {
        super(s, null);
    }

    public Throwable getException() {
        return getCause();
    }
}
