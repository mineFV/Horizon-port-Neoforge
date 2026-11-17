package org.slf4j.helpers;

/**
 * The stand-in for slf4j's MessageFormatter, as its documentation describes it (checked by `standins`): each "{}" is
 * the next argument, "\{}" a plain "{}", "\\{}" a backslash then the argument; arrays are written with their elements;
 * a last argument that is a Throwable is the message's throwable, not an argument.
 */
public final class MessageFormatter {
    public MessageFormatter() {}

    public static final FormattingTuple format(String pattern, Object arg) {
        return arrayFormat(pattern, new Object[] {arg});
    }

    public static final FormattingTuple format(String pattern, Object arg1, Object arg2) {
        return arrayFormat(pattern, new Object[] {arg1, arg2});
    }

    public static final FormattingTuple arrayFormat(String pattern, Object[] args) {
        Throwable candidate = getThrowableCandidate(args);
        Object[] used = candidate != null ? trimmedCopy(args) : args;
        return arrayFormat(pattern, used, candidate);
    }

    public static Throwable getThrowableCandidate(Object[] args) {
        if (args == null || args.length == 0) return null;
        return args[args.length - 1] instanceof Throwable t ? t : null;
    }

    public static Object[] trimmedCopy(Object[] args) {
        if (args == null || args.length == 0) throw new IllegalStateException("non-sensical empty or null argument array");
        Object[] out = new Object[args.length - 1];
        System.arraycopy(args, 0, out, 0, out.length);
        return out;
    }

    public static final FormattingTuple arrayFormat(String pattern, Object[] args, Throwable thrown) {
        if (pattern == null) return new FormattingTuple(null, args, thrown);
        if (args == null) return new FormattingTuple(pattern);
        int i = 0;
        StringBuilder b = new StringBuilder(pattern.length() + 50);
        int L;
        for (L = 0; L < args.length; L++) {
            int j = pattern.indexOf("{}", i);
            if (j == -1) {
                if (i == 0) return new FormattingTuple(pattern, args, thrown);
                b.append(pattern, i, pattern.length());
                return new FormattingTuple(b.toString(), args, thrown);
            }
            if (escaped(pattern, j)) {
                if (!doubleEscaped(pattern, j)) {
                    L--;
                    b.append(pattern, i, j - 1).append('{');
                    i = j + 1;
                } else {
                    b.append(pattern, i, j - 1);
                    deepAppend(b, args[L], new java.util.ArrayList<>());
                    i = j + 2;
                }
            } else {
                b.append(pattern, i, j);
                deepAppend(b, args[L], new java.util.ArrayList<>());
                i = j + 2;
            }
        }
        b.append(pattern, i, pattern.length());
        return new FormattingTuple(b.toString(), args, thrown);
    }

    private static boolean escaped(String pattern, int at) {
        return at != 0 && pattern.charAt(at - 1) == '\\';
    }

    private static boolean doubleEscaped(String pattern, int at) {
        return at >= 2 && pattern.charAt(at - 2) == '\\';
    }

    private static void deepAppend(StringBuilder b, Object o, java.util.List<Object[]> seen) {
        if (o == null) {
            b.append("null");
            return;
        }
        if (!o.getClass().isArray()) {
            String text;
            try {
                text = o.toString();
            } catch (RuntimeException e) {
                text = "[FAILED toString()]";
            }
            b.append(text);
            return;
        }
        if (o instanceof boolean[] a) b.append(java.util.Arrays.toString(a));
        else if (o instanceof byte[] a) b.append(java.util.Arrays.toString(a));
        else if (o instanceof char[] a) b.append(java.util.Arrays.toString(a));
        else if (o instanceof short[] a) b.append(java.util.Arrays.toString(a));
        else if (o instanceof int[] a) b.append(java.util.Arrays.toString(a));
        else if (o instanceof long[] a) b.append(java.util.Arrays.toString(a));
        else if (o instanceof float[] a) b.append(java.util.Arrays.toString(a));
        else if (o instanceof double[] a) b.append(java.util.Arrays.toString(a));
        else {
            Object[] a = (Object[]) o;
            for (Object[] s : seen) {
                if (s == a) {
                    b.append("[...]");
                    return;
                }
            }
            seen.add(a);
            b.append('[');
            for (int k = 0; k < a.length; k++) {
                deepAppend(b, a[k], seen);
                if (k != a.length - 1) b.append(", ");
            }
            b.append(']');
            seen.remove(seen.size() - 1);
        }
    }
}
