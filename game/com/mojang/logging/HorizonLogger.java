package com.mojang.logging;

import org.slf4j.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * Not the game's: the logger LogUtils gives. Every level is on, as in a server (observed, `standins --horizon-src`);
 * info, warn and error go to Horizon's output (HorizonLog.write, decision 5) as a server's console has them, trace and
 * debug nowhere (a server writes them to its debug.log), each message as slf4j formats it. Its name (its caller's
 * class, which the game finds on the stack) isn't modelled.
 */
public final class HorizonLogger implements Logger {
    @Override
    public String getName() {
        throw new UnsupportedOperationException("a logger's name (its caller's class) isn't modelled (horizon-port-neoforge)");
    }

    private static void write(String level, FormattingTuple t) {
        if (level.equals("TRACE") || level.equals("DEBUG")) return;
        String text = t.getMessage();
        if (t.getThrowable() != null) text = text + "\n" + t.getThrowable();
        HorizonLog.write(level, text);
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public void trace(String message) {
        if (isTraceEnabled()) write("TRACE", new FormattingTuple(message));
    }

    @Override
    public void trace(String format, Object arg) {
        if (isTraceEnabled()) write("TRACE", MessageFormatter.format(format, arg));
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if (isTraceEnabled()) write("TRACE", MessageFormatter.format(format, arg1, arg2));
    }

    @Override
    public void trace(String format, Object... args) {
        if (isTraceEnabled()) write("TRACE", MessageFormatter.arrayFormat(format, args));
    }

    @Override
    public void trace(String message, Throwable thrown) {
        if (isTraceEnabled()) write("TRACE", new FormattingTuple(message, null, thrown));
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String message) {
        if (isDebugEnabled()) write("DEBUG", new FormattingTuple(message));
    }

    @Override
    public void debug(String format, Object arg) {
        if (isDebugEnabled()) write("DEBUG", MessageFormatter.format(format, arg));
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (isDebugEnabled()) write("DEBUG", MessageFormatter.format(format, arg1, arg2));
    }

    @Override
    public void debug(String format, Object... args) {
        if (isDebugEnabled()) write("DEBUG", MessageFormatter.arrayFormat(format, args));
    }

    @Override
    public void debug(String message, Throwable thrown) {
        if (isDebugEnabled()) write("DEBUG", new FormattingTuple(message, null, thrown));
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String message) {
        if (isInfoEnabled()) write("INFO", new FormattingTuple(message));
    }

    @Override
    public void info(String format, Object arg) {
        if (isInfoEnabled()) write("INFO", MessageFormatter.format(format, arg));
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (isInfoEnabled()) write("INFO", MessageFormatter.format(format, arg1, arg2));
    }

    @Override
    public void info(String format, Object... args) {
        if (isInfoEnabled()) write("INFO", MessageFormatter.arrayFormat(format, args));
    }

    @Override
    public void info(String message, Throwable thrown) {
        if (isInfoEnabled()) write("INFO", new FormattingTuple(message, null, thrown));
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String message) {
        if (isWarnEnabled()) write("WARN", new FormattingTuple(message));
    }

    @Override
    public void warn(String format, Object arg) {
        if (isWarnEnabled()) write("WARN", MessageFormatter.format(format, arg));
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (isWarnEnabled()) write("WARN", MessageFormatter.format(format, arg1, arg2));
    }

    @Override
    public void warn(String format, Object... args) {
        if (isWarnEnabled()) write("WARN", MessageFormatter.arrayFormat(format, args));
    }

    @Override
    public void warn(String message, Throwable thrown) {
        if (isWarnEnabled()) write("WARN", new FormattingTuple(message, null, thrown));
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String message) {
        if (isErrorEnabled()) write("ERROR", new FormattingTuple(message));
    }

    @Override
    public void error(String format, Object arg) {
        if (isErrorEnabled()) write("ERROR", MessageFormatter.format(format, arg));
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (isErrorEnabled()) write("ERROR", MessageFormatter.format(format, arg1, arg2));
    }

    @Override
    public void error(String format, Object... args) {
        if (isErrorEnabled()) write("ERROR", MessageFormatter.arrayFormat(format, args));
    }

    @Override
    public void error(String message, Throwable thrown) {
        if (isErrorEnabled()) write("ERROR", new FormattingTuple(message, null, thrown));
    }

}
