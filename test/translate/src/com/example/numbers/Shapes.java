package com.example.numbers;

/**
 * Made up for the translator's tests: objects, inheritance, interfaces with default methods, fields and static
 * fields with their initializers, arrays, lambdas, casts and null. Each public static method builds what it needs
 * and answers with a number, so that `NumbersCheck` and `numbers_check.rs` can compare them.
 */
public final class Shapes {
    private Shapes() {}

    /** Something with an area, and a default method on top. */
    public interface Area {
        int area();

        default int twiceArea() {
            return area() * 2;
        }
    }

    /** A functional interface of the mod's own, for lambdas. */
    public interface IntOp {
        int apply(int x);
    }

    public interface Maker {
        Shape make(int size);
    }

    public abstract static class Shape implements Area {
        static int made;
        protected final int size;

        Shape(int size) {
            this.size = size;
            made++;
        }

        abstract int sides();

        int perimeter() {
            return sides() * size;
        }
    }

    public static class Square extends Shape {
        Square(int size) {
            super(size);
        }

        @Override
        int sides() {
            return 4;
        }

        @Override
        public int area() {
            return size * size;
        }
    }

    public static class Triangle extends Shape {
        final int height;

        Triangle(int size, int height) {
            super(size);
            this.height = height;
        }

        @Override
        int sides() {
            return 3;
        }

        @Override
        public int area() {
            return size * height / 2;
        }

        @Override
        public int twiceArea() {
            return size * height;
        }
    }

    /** A subclass that overrides one method and inherits the rest. */
    public static class BigSquare extends Square {
        BigSquare(int size) {
            super(size * 10);
        }

        @Override
        int perimeter() {
            return super.perimeter() + 1;
        }
    }

    /** A singly linked list of ints. */
    static final class Node {
        final int value;
        Node next;

        Node(int value, Node next) {
            this.value = value;
            this.next = next;
        }
    }

    static final class Counter {
        static final int START = 100;
        static long total = 7;
        static int[] table;

        static {
            table = new int[8];
            for (int i = 0; i < table.length; i++) table[i] = i * i + START;
            total += table[3];
        }
    }

    // ---- what the checks call ----

    public static int areas(int n) {
        Area[] all = new Area[] {new Square(n), new Triangle(n, n + 1), new BigSquare(n)};
        int sum = 0;
        for (Area a : all) sum = sum * 31 + a.area() + a.twiceArea();
        return sum;
    }

    public static int perimeters(int n) {
        Shape[] all = {new Square(n), new Triangle(n, 2), new BigSquare(n)};
        int sum = 0;
        for (Shape s : all) sum += s.perimeter();
        return sum;
    }

    public static int made(int n) {
        int before = Shape.made;
        for (int i = 0; i < n; i++) new Square(i);
        return Shape.made - before;
    }

    public static int list(int n) {
        Node head = null;
        for (int i = 0; i < n; i++) head = new Node(i * 3 - 7, head);
        int sum = 0, count = 0;
        for (Node at = head; at != null; at = at.next) {
            sum += at.value;
            count++;
        }
        if (head != null && head.next != null) head.next = head.next.next;
        int after = 0;
        for (Node at = head; at != null; at = at.next) after++;
        return sum * 1000 + count * 10 + after;
    }

    public static long statics(int i) {
        return Counter.total * 1000 + Counter.table[i & 7] + Counter.START;
    }

    public static int lambdas(int x) {
        int k = x * 2;
        IntOp twice = v -> v * 2;
        IntOp plusK = v -> v + k;
        IntOp ref = Shapes::square;
        Maker maker = Square::new;
        return twice.apply(x) + plusK.apply(x) * 3 + ref.apply(x) + maker.make(x).area();
    }

    static int square(int v) {
        return v * v;
    }

    public static int casts(int n) {
        Object o = n % 2 == 0 ? new Square(n) : new Triangle(n, 4);
        int r = 0;
        if (o instanceof Square) r += 1;
        if (o instanceof Shape) r += 10;
        if (o instanceof Area) r += 100;
        if (o instanceof Triangle t) r += t.height * 1000;
        Object nothing = n > 1000 ? o : null;
        if (nothing instanceof Shape) r += 10000;
        Shape s = (Shape) o;
        return r + s.sides();
    }

    public static int arrays(int n) {
        long[] longs = new long[n + 1];
        byte[] bytes = new byte[4];
        boolean[] flags = new boolean[3];
        char[] chars = new char[2];
        double[][] grid = new double[2][];
        grid[0] = new double[] {1.5, n};
        grid[1] = new double[3];
        for (int i = 0; i < longs.length; i++) longs[i] = (long) i << 33;
        bytes[1] = (byte) (n * 77);
        flags[2] = n % 3 == 0;
        chars[0] = (char) (n + 65);
        int r = (int) (longs[n] >> 30) + bytes[1] + (flags[2] ? 5 : 0) + chars[0];
        return r + grid.length * 100 + grid[1].length * 1000 + (int) (grid[0][0] + grid[0][1]);
    }

    /** A class with a supertype outside the jar, whose own supertypes can't be read: a type test on it isn't certain. */
    static final class Task implements Runnable {
        @Override
        public void run() {}
    }

    public static int tasks(int n) {
        Object o = n > 0 ? new Task() : null;
        return o instanceof Comparable ? 1 : 0;
    }

    /** An interface nothing implements: its dispatcher has no class to match. */
    public interface Never {
        int never(int x);
    }

    public static int callNever(Never n, int x) {
        return n == null ? -x : n.never(x);
    }

    public static int same(int n) {
        Square a = new Square(n), b = new Square(n);
        Square c = a;
        return (a == b ? 1 : 0) + (a == c ? 10 : 0) + (a != null ? 100 : 0);
    }
}
