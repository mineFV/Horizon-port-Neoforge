package com.example.numbers;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Made up for the translator's tests: records, enums, boxes, Objects and java.util.function, the library model's
 * second part. Each public static method answers with a text or a number, so that `NumbersCheck` and
 * `numbers_check.rs` can compare them.
 */
public final class Boxes {
    private Boxes() {}

    public record Point(int x, long y, float f, double d, boolean b, char c, byte by, short s) {}

    public record Named(String name, Point at, Object extra) {}

    public record Empty() {}

    public enum Color {
        RED,
        GREEN {
            @Override
            public String toString() {
                return "green!";
            }
        },
        BLUE;

        int shade() {
            return ordinal() * 10;
        }
    }

    // ---- records ----

    public static String records(int n) {
        Point p = new Point(n, n * 3L, n / 2f, -0.0, n % 2 == 0, (char) ('a' + n), (byte) (n * 40), (short) (n * 1000));
        Point q = new Point(n, n * 3L, n / 2f, 0.0, n % 2 == 0, (char) ('a' + n), (byte) (n * 40), (short) (n * 1000));
        Point same = new Point(n, n * 3L, n / 2f, -0.0, n % 2 == 0, (char) ('a' + n), (byte) (n * 40), (short) (n * 1000));
        Named a = new Named("a" + n, p, n > 5 ? null : Integer.valueOf(n));
        Named b = new Named("a" + n, same, n > 5 ? null : Integer.valueOf(n));
        Point nan = new Point(0, 0, Float.NaN, Double.NaN, false, 'x', (byte) 0, (short) 0);
        return p + " | " + p.hashCode() + " " + p.equals(q) + " " + p.equals(same) + " " + p.equals(null) + " | " + a + " " + a.hashCode() + " "
                + a.equals(b) + " | " + new Empty() + " " + new Empty().hashCode() + " " + new Empty().equals(new Empty()) + " | " + nan.equals(nan) + " "
                + nan.hashCode();
    }

    // ---- enums ----

    public static String enums(int n) {
        Color[] all = Color.values();
        Color c = all[n % all.length];
        String kind = switch (c) {
            case RED -> "warm";
            case GREEN -> "fresh";
            case BLUE -> "cold";
        };
        return c.name() + " " + c + " " + c.ordinal() + " " + c.shade() + " " + kind + " " + c.compareTo(Color.GREEN) + " " + (all != Color.values()) + " "
                + c.equals(Color.RED) + " " + all.length;
    }

    // ---- boxes ----

    public static String boxes(int n) {
        Integer a = n * 20, b = n * 20;
        Long la = (long) n * 20, lb = (long) n * 20;
        Character ca = (char) (n * 20), cb = (char) (n * 20);
        Boolean t = n > 3;
        Double d = n / 4.0;
        Float f = -n / 8f;
        return (a == b) + " " + a.equals(b) + " " + (la == lb) + " " + (ca == cb) + " " + (t == Boolean.valueOf(n > 3)) + " " + a.hashCode() + " "
                + la.hashCode() + " " + d.hashCode() + " " + f.hashCode() + " " + t.hashCode() + " " + Integer.compare(n, 5) + " " + Long.compare(-n, n)
                + " " + Double.compare(0.0, -0.0) + " " + Float.compare(Float.NaN, 1f) + " " + d.equals(n / 4.0) + " " + Double.valueOf(Double.NaN).equals(Double.NaN)
                + " " + (a + b) + " " + Integer.toHexString(-n) + " " + Long.toBinaryString(n) + " " + Short.compare((short) n, (short) -n) + " " + d + " " + f;
    }

    public static String bits(int n) {
        float f = n / 3f;
        double d = n / 7.0;
        return Float.floatToIntBits(f) + " " + Double.doubleToLongBits(d) + " " + Float.intBitsToFloat(0x40490fdb + n) + " " + Double.longBitsToDouble(0x400921fb54442d18L + n)
                + " " + Float.floatToIntBits(Float.NaN) + " " + Double.isNaN(d / 0 * 0) + " " + Float.isInfinite(f / 0);
    }

    // ---- Objects ----

    public static String objects(int n) {
        Point p = n > 4 ? null : new Point(n, 0, 0, 0, false, 'z', (byte) 0, (short) 0);
        String s = Objects.toString(p, "none") + " " + Objects.hashCode(p) + " " + Objects.equals(p, p) + " " + Objects.isNull(p) + " "
                + Objects.hash(n, "x", null, (long) n) + " " + Objects.requireNonNullElse(p == null ? null : "has", "default");
        try {
            Objects.requireNonNull(p, "p is missing");
            s += " required";
        } catch (NullPointerException e) {
            s += " " + e.getMessage();
        }
        try {
            Objects.checkIndex(n, 4);
            s += " in";
        } catch (IndexOutOfBoundsException e) {
            s += " " + e.getMessage();
        }
        return s;
    }

    // ---- java.util.function, and boxing in lambdas ----

    public static String functions(int n) {
        Supplier<Integer> boxed = () -> n * 2;
        Function<Integer, Integer> plus = x -> x + n;
        Function<Integer, Integer> twice = x -> x * 2;
        Predicate<String> empty = String::isEmpty;
        IntFunction<Integer> valueOf = Integer::valueOf;
        ToIntFunction<String> length = String::length;
        BinaryOperator<Integer> sum = Integer::sum;
        Function<Object, String> text = String::valueOf;
        String word = "w".repeat(n % 3);
        return boxed.get() + " " + plus.andThen(twice).apply(n) + " " + plus.compose(twice).apply(n) + " " + Function.<Integer>identity().apply(n) + " "
                + empty.test(word) + " " + empty.negate().test(word) + " " + empty.or(s -> s.length() > 1).test(word) + " " + Predicate.not(empty).test(word) + " "
                + (valueOf.apply(n) == valueOf.apply(n)) + " " + length.applyAsInt(word) + " " + sum.apply(n, 40) + " " + text.apply(n > 2 ? Color.BLUE : null);
    }
}
