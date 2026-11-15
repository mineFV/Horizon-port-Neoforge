package java.lang;

/**
 * The model of java.lang.Throwable: its message and its cause, as the API specifies them (a cause not yet given is
 * the throwable itself, so that initCause can tell). No stack trace is kept: printStackTrace and getStackTrace
 * aren't modelled, and fillInStackTrace does nothing.
 */
public class Throwable implements java.io.Serializable {
    private String detailMessage;
    private Throwable cause = this;
    /** The exceptions suppressed for this one (try-with-resources), none yet when null. */
    private java.util.ArrayList<Throwable> suppressedExceptions;
    /** Its fields' defaults are what the runtime's own exceptions (made without a constructor) need: suppression on. */
    private boolean suppressionOff;

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
        suppressionOff = !enableSuppression;
    }

    /** Not synchronized, as the JDK's is: the translation runs one thread. */
    public final void addSuppressed(Throwable exception) {
        if (exception == this) throw new IllegalArgumentException("Self-suppression not permitted", exception);
        java.util.Objects.requireNonNull(exception, "Cannot suppress a null exception.");
        if (suppressionOff) return;
        if (suppressedExceptions == null) suppressedExceptions = new java.util.ArrayList<>(1);
        suppressedExceptions.add(exception);
    }

    public final Throwable[] getSuppressed() {
        if (suppressedExceptions == null || suppressedExceptions.isEmpty()) return new Throwable[0];
        return suppressedExceptions.toArray(new Throwable[0]);
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
