package com.example.numbers;

/**
 * Made up for the translator's tests: static methods of numbers that use each kind of instruction the first step
 * translates (and a few it refuses). `NumbersCheck` runs them in Java, `numbers_check.rs` their translation in Rust,
 * on the same inputs: the outputs must be the same.
 */
public final class Numbers {
    private Numbers() {}

    // ---- int and long arithmetic, which wraps ----

    public static int mix(int a, int b) {
        return a * 31 + b ^ (a >>> 3) - (b << 5) >> 2;
    }

    public static long lmix(long a, int s) {
        return (a << s) ^ (a >>> s) + a / 3 - a % 7 | (a >> 60);
    }

    public static int div(int a, int b) {
        return b == 0 ? 0 : a / b + a % b;
    }

    public static int shl(int a, int s) {
        return a << s;
    }

    public static long ushr(long a, int s) {
        return a >>> s;
    }

    public static int neg(int a) {
        return -a & ~a;
    }

    // ---- loops and branches ----

    public static int sumTo(int n) {
        int s = 0;
        for (int i = 0; i < n; i++) s += i * i;
        return s;
    }

    public static long collatz(long n) {
        long steps = 0;
        while (n > 1 && steps < 1000) {
            n = (n % 2 == 0) ? n / 2 : 3 * n + 1;
            steps++;
        }
        return steps;
    }

    public static int sign(int x) {
        return x > 0 ? x > 10 ? 2 : 1 : x < 0 ? -1 : 0;
    }

    public static int incBoth(int a) {
        int b = a++;
        return a * 10 + b;
    }

    public static int cmpL(long a, long b) {
        return a < b ? -1 : a == b ? 0 : 1;
    }

    // ---- switches ----

    public static int days(int month) {
        switch (month) {
            case 1: case 3: case 5: case 7: case 8: case 10: case 12:
                return 31;
            case 2:
                return 28;
            case 4: case 6: case 9: case 11:
                return 30;
            default:
                return -1;
        }
    }

    public static int sparse(int k) {
        switch (k) {
            case -100:
                return 1;
            case 7:
                return 2;
            case 1000000:
                return 3;
            default:
                return 0;
        }
    }

    // ---- floats, NaN and negative zero ----

    public static boolean less(float a, float b) {
        return a < b;
    }

    public static boolean atLeast(double a, double b) {
        return a >= b;
    }

    public static float frem(float a, float b) {
        return a % b;
    }

    public static double poly(double x) {
        return ((3.5 * x - 2.25) * x + 1e-3) / (x == 0 ? 1 : x);
    }

    public static float fmin(float a, float b) {
        return Math.min(a, b);
    }

    public static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ---- conversions ----

    public static int f2i(float f) {
        return (int) f;
    }

    public static long d2l(double d) {
        return (long) d;
    }

    public static byte toByte(int x) {
        return (byte) x;
    }

    public static char toChar(int x) {
        return (char) x;
    }

    public static short toShort(int x) {
        return (short) x;
    }

    public static float l2f(long x) {
        return (float) x;
    }

    // ---- Math ----

    public static int roundF(float f) {
        return Math.round(f);
    }

    public static long roundD(double d) {
        return Math.round(d);
    }

    public static int floorMod(int a, int b) {
        return Math.floorMod(a, b);
    }

    public static long floorDiv(long a, long b) {
        return Math.floorDiv(a, b);
    }

    public static double sig(double d) {
        return Math.signum(d);
    }

    public static int iabs(int a) {
        return Math.abs(a);
    }

    // ---- calls, booleans and chars at the edges ----

    public static int add1(int a) {
        return a + 1;
    }

    public static int twice(int a) {
        return add1(add1(a));
    }

    public static long fact(int n) {
        return n <= 1 ? 1 : n * fact(n - 1);
    }

    public static int pick(boolean c, int a, int b) {
        return c ? a : b;
    }

    public static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    // ---- refused, each for its own reason ----

    public static String name(int x) {
        return "n" + x;
    }

    public int instance() {
        return 1;
    }

    public static int safe(int a) {
        try {
            return 10 / a;
        } catch (ArithmeticException e) {
            return 0;
        }
    }

    public static int callsSafe(int a) {
        return safe(a) + 1;
    }
}
