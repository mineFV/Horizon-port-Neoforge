package org.slf4j;

/** The stand-in for slf4j's Logger: its levels' methods without markers (those with markers aren't here). */
public interface Logger {
    String getName();

    boolean isTraceEnabled();

    void trace(String message);

    void trace(String format, Object arg);

    void trace(String format, Object arg1, Object arg2);

    void trace(String format, Object... args);

    void trace(String message, Throwable thrown);

    boolean isDebugEnabled();

    void debug(String message);

    void debug(String format, Object arg);

    void debug(String format, Object arg1, Object arg2);

    void debug(String format, Object... args);

    void debug(String message, Throwable thrown);

    boolean isInfoEnabled();

    void info(String message);

    void info(String format, Object arg);

    void info(String format, Object arg1, Object arg2);

    void info(String format, Object... args);

    void info(String message, Throwable thrown);

    boolean isWarnEnabled();

    void warn(String message);

    void warn(String format, Object arg);

    void warn(String format, Object arg1, Object arg2);

    void warn(String format, Object... args);

    void warn(String message, Throwable thrown);

    boolean isErrorEnabled();

    void error(String message);

    void error(String format, Object arg);

    void error(String format, Object arg1, Object arg2);

    void error(String format, Object... args);

    void error(String message, Throwable thrown);

}
