package com.example.numbers;

/**
 * Made up for the translator's tests: exceptions (thrown, caught, finally, from the JVM itself, from a class
 * initializer) and texts (literals, joins of every kind of value, String's methods, numbers as text). Each public
 * static method answers with a text or a number, so that `NumbersCheck` and `numbers_check.rs` can compare them.
 */
public final class Texts {
    private Texts() {}

    /** An exception of the mod's own. */
    public static class Problem extends RuntimeException {
        final int code;

        Problem(String message, int code) {
            super(message);
            this.code = code;
        }

        @Override
        public String getMessage() {
            return super.getMessage() + " (code " + code + ")";
        }
    }

    /** A class whose initializer throws when its trigger is set. */
    static final class Fragile {
        static int trigger;
        static final int VALUE;

        static {
            if (trigger > 0) throw new IllegalStateException("fragile " + trigger);
            VALUE = 7;
        }
    }

    static final class Breaks {
        static final int VALUE = compute();

        static int compute() {
            throw new Problem("broken initializer", 3);
        }
    }

    static final class Named {
        final String name;

        Named(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Named[" + name + "]";
        }
    }

    // ---- exceptions ----

    public static String divide(int a, int b) {
        try {
            return "ok " + (a / b);
        } catch (ArithmeticException e) {
            return "caught " + e.getMessage();
        }
    }

    public static int finallyCounts(int n) {
        int count = 0;
        for (int i = 0; i < n; i++) {
            try {
                if (i % 3 == 0) throw new Problem("third", i);
                count += 1;
            } catch (Problem p) {
                count += 10 * p.code;
            } finally {
                count += 100;
            }
        }
        return count;
    }

    @SuppressWarnings("finally")
    public static int finallyReturns(int n) {
        try {
            if (n > 2) throw new IllegalArgumentException("big");
            return n;
        } finally {
            if (n > 5) return -1;
        }
    }

    public static String jvm(int which) {
        try {
            switch (which) {
                case 0 -> {
                    int[] a = new int[2];
                    return "" + a[which + 5];
                }
                case 1 -> {
                    Object[] a = new String[1];
                    a[0] = new Named("x");
                    return "stored";
                }
                case 2 -> {
                    Named n = null;
                    return n.name;
                }
                case 3 -> {
                    Object o = new Named("y");
                    return ((String) o);
                }
                case 4 -> {
                    return "" + new int[which - 10].length;
                }
                case 5 -> {
                    return "" + 10 % (which - 5);
                }
                default -> {
                    return "nothing";
                }
            }
        } catch (NullPointerException e) {
            // Its message is the JDK's, built from the bytecode: the translation's has none (lib/README.md).
            return "a NullPointerException";
        } catch (ClassCastException e) {
            return "a ClassCastException";
        } catch (RuntimeException e) {
            return e.toString();
        }
    }

    /** What isn't caught goes out of the method: the checks see it thrown. */
    public static int uncaught(int n) {
        if (n > 3) throw new IllegalArgumentException("too many: " + n);
        return n * 2;
    }

    public static String causes(int n) {
        RuntimeException outer = new RuntimeException("outer", new IllegalStateException("inner " + n));
        Throwable t = new RuntimeException(outer);
        String s = t.getMessage() + " | " + t.getCause().getMessage() + " | " + outer.getCause();
        try {
            outer.initCause(new Error());
        } catch (IllegalStateException e) {
            s += " | " + e.getMessage();
        }
        Problem p = new Problem("p", n);
        p.initCause(null);
        return s + " | " + p + " | " + p.getCause();
    }

    public static String initializers(int n) {
        String s;
        try {
            s = "breaks " + Breaks.VALUE;
        } catch (ExceptionInInitializerError e) {
            s = "first: " + e.getCause().getMessage();
        }
        try {
            s += " | again " + Breaks.VALUE;
        } catch (NoClassDefFoundError e) {
            s += " | again: " + e.getMessage();
        }
        return s + " | fragile " + Fragile.VALUE;
    }

    public static String rethrow(int n) {
        try {
            try {
                throw new Problem("first", n);
            } catch (Problem p) {
                throw new IllegalStateException("wrapped", p);
            }
        } catch (IllegalStateException e) {
            return e.getMessage() + " <- " + e.getCause().getMessage();
        }
    }

    // ---- texts ----

    public static String join(int n) {
        long l = (long) n * 100_000_000_007L;
        char c = (char) ('A' + n);
        boolean b = n % 2 == 0;
        String nothing = null;
        Named named = new Named("n" + n);
        return "int " + n + ", long " + l + ", char " + c + ", bool " + b + ", null " + nothing + ", float " + (n / 3f) + ", double " + (n / 7.0)
                + ", object " + named + ", byte " + (byte) (n * 50) + ", short " + (short) (n * 3000);
    }

    public static String literals(int n) {
        String a = "same", b = "same";
        String c = new String("same");
        String d = ("sa" + (n >= 0 ? "me" : "")).intern();
        return (a == b) + " " + (a == c) + " " + a.equals(c) + " " + (a == d) + " " + "same".hashCode() + " " + "".hashCode();
    }

    public static String methods(int n) {
        String s = "  Hello, translated world  ";
        String t = s.trim();
        return t.length() + " " + t.charAt(n % t.length()) + " " + t.indexOf('o') + " " + t.indexOf("world") + " " + t.lastIndexOf('o') + " "
                + t.substring(7) + " " + t.substring(0, 5) + " " + t.replace('o', '0') + " " + t.startsWith("Hell") + " " + t.endsWith("world")
                + " " + t.contains("trans") + " " + "ab".repeat(n % 4) + " " + "abc".compareTo("abd") + " " + "b".compareTo("abc") + " " + t.hashCode();
    }

    public static String outOfBounds(int n) {
        try {
            return "" + "abc".charAt(n);
        } catch (StringIndexOutOfBoundsException e) {
            String s = e.getMessage();
            try {
                return s + " | " + "abc".substring(n, 1);
            } catch (StringIndexOutOfBoundsException f) {
                return s + " | " + f.getMessage();
            }
        }
    }

    public static String doubleText(double d) {
        return "" + d;
    }

    public static String floatText(float f) {
        return String.valueOf(f);
    }
}
