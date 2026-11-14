package java.lang;

/** The model of java.lang.ArrayIndexOutOfBoundsException. */
public class ArrayIndexOutOfBoundsException extends IndexOutOfBoundsException {
    public ArrayIndexOutOfBoundsException() {}

    public ArrayIndexOutOfBoundsException(String message) {
        super(message);
    }

    public ArrayIndexOutOfBoundsException(int index) {
        super("Array index out of range: ".concat(Integer.toString(index)));
    }
}
