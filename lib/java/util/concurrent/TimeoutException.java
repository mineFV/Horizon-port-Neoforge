package java.util.concurrent;

/** The model of java.util.concurrent.TimeoutException. */
public class TimeoutException extends Exception {
    public TimeoutException() {}

    public TimeoutException(String message) {
        super(message);
    }
}
