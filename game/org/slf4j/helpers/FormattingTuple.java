package org.slf4j.helpers;

/** The stand-in for slf4j's FormattingTuple: a formatted message, its arguments and its throwable. */
public class FormattingTuple {
    public static FormattingTuple NULL = new FormattingTuple(null);

    private final String message;
    private final Object[] argArray;
    private final Throwable throwable;

    public FormattingTuple(String message) {
        this(message, null, null);
    }

    public FormattingTuple(String message, Object[] argArray, Throwable throwable) {
        this.message = message;
        this.throwable = throwable;
        this.argArray = argArray;
    }

    public String getMessage() {
        return message;
    }

    public Object[] getArgArray() {
        return argArray;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
