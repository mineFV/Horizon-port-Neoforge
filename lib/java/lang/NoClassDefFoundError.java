package java.lang;

/** The model of java.lang.NoClassDefFoundError. */
public class NoClassDefFoundError extends LinkageError {
    public NoClassDefFoundError() {}

    public NoClassDefFoundError(String message) {
        super(message);
    }
}
