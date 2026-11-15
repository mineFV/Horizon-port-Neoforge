package java.lang;

/** The model of java.lang.ClassNotFoundException. */
public class ClassNotFoundException extends ReflectiveOperationException {
    public ClassNotFoundException() {}

    public ClassNotFoundException(String message) {
        super(message);
    }

    public ClassNotFoundException(String s, Throwable ex) {
        super(s, ex);
    }
}
