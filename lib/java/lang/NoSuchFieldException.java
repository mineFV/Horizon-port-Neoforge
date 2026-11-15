package java.lang;

/** The model of java.lang.NoSuchFieldException. */
public class NoSuchFieldException extends ReflectiveOperationException {
    public NoSuchFieldException() {}

    public NoSuchFieldException(String message) {
        super(message);
    }
}
