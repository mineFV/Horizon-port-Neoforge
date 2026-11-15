package java.util.concurrent;

/** The model of java.util.concurrent.CancellationException. */
public class CancellationException extends IllegalStateException {
    public CancellationException() {}

    public CancellationException(String message) {
        super(message);
    }
}
