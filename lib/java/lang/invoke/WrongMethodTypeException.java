package java.lang.invoke;

/** The model of java.lang.invoke.WrongMethodTypeException. */
public class WrongMethodTypeException extends RuntimeException {
    public WrongMethodTypeException() {}

    public WrongMethodTypeException(String message) {
        super(message);
    }
}
