package java.lang;

/**
 * The model of java.lang.Throwable: its message and its cause, as the API specifies them (a cause not yet given is
 * the throwable itself, so that initCause can tell). No stack trace is kept: printStackTrace and getStackTrace
 * aren't modelled, and fillInStackTrace does nothing.
 */
public class Throwable implements java.io.Serializable {
    private String detailMessage;
    private Throwable cause = this;

    public Throwable() {}

    public Throwable(String message) {
        detailMessage = message;
    }

    public Throwable(String message, Throwable cause) {
        detailMessage = message;
        this.cause = cause;
    }

    public Throwable(Throwable cause) {
        detailMessage = cause == null ? null : cause.toString();
        this.cause = cause;
    }

    protected Throwable(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        detailMessage = message;
        this.cause = cause;
    }

    public String getMessage() {
        return detailMessage;
    }

    public String getLocalizedMessage() {
        return getMessage();
    }

    public Throwable getCause() {
        return cause == this ? null : cause;
    }

    public Throwable initCause(Throwable cause) {
        if (this.cause != this) {
            throw new IllegalStateException("Can't overwrite cause with ".concat(cause == null ? "a null" : cause.toString()), this);
        }
        if (cause == this) throw new IllegalArgumentException("Self-causation not permitted", this);
        this.cause = cause;
        return this;
    }

    public Throwable fillInStackTrace() {
        return this;
    }

    public String toString() {
        String s = jClassName();
        String message = getLocalizedMessage();
        return message != null ? s.concat(": ").concat(message) : s;
    }
}
