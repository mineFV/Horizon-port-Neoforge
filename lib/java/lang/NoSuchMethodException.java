package java.lang;

/** The model of java.lang.NoSuchMethodException. */
public class NoSuchMethodException extends ReflectiveOperationException {
    public NoSuchMethodException() {}

    public NoSuchMethodException(String message) {
        super(message);
    }
}
