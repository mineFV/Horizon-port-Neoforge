package java.lang;

/** The model of java.lang.NoSuchFieldError. */
public class NoSuchFieldError extends IncompatibleClassChangeError {
    public NoSuchFieldError() {}

    public NoSuchFieldError(String message) {
        super(message);
    }
}
