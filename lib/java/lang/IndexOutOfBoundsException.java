package java.lang;

/** The model of java.lang.IndexOutOfBoundsException. */
public class IndexOutOfBoundsException extends RuntimeException {
    public IndexOutOfBoundsException() {}

    public IndexOutOfBoundsException(String message) {
        super(message);
    }

    public IndexOutOfBoundsException(int index) {
        super("Index out of range: ".concat(Integer.toString(index)));
    }

    public IndexOutOfBoundsException(long index) {
        super("Index out of range: ".concat(Long.toString(index)));
    }
}
