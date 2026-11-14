package java.lang;

/** The model of java.lang.MatchException (a switch on a sealed type that matched nothing). */
public final class MatchException extends RuntimeException {
    public MatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
