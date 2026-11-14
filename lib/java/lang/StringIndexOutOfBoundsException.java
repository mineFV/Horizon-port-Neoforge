package java.lang;

/** The model of java.lang.StringIndexOutOfBoundsException. */
public class StringIndexOutOfBoundsException extends IndexOutOfBoundsException {
    public StringIndexOutOfBoundsException() {}

    public StringIndexOutOfBoundsException(String message) {
        super(message);
    }

    public StringIndexOutOfBoundsException(int index) {
        super("String index out of range: ".concat(Integer.toString(index)));
    }
}
