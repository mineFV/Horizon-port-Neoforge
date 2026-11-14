package java.lang;

/** The model of java.lang.NoSuchMethodError. */
public class NoSuchMethodError extends IncompatibleClassChangeError {
    public NoSuchMethodError() {}

    public NoSuchMethodError(String message) {
        super(message);
    }
}
