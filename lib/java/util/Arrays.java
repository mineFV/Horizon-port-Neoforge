package java.util;

import java.util.function.IntFunction;

/**
 * The model of java.util.Arrays: asList, fill, hashCode, equals, toString, copyOf, copyOfRange and sort, with the
 * JDK's messages. Sorting gives the JDK's result (a stable merge sort gives what the JDK's sorts give: a unique order
 * for primitives, -0.0 before 0.0 and NaN last; a stable one for objects) but asks a comparator in another order.
 * Streams, binarySearch, deep*, parallel* and the primitive setAll aren't modelled.
 */
public final class Arrays {
    private Arrays() {}

    /** A new array of the same class as a (a String[] for a String[]), of this length, all null. */
    static native Object[] newArrayLike(Object[] a, int length);

    /** A copy as an Object[] (what toArray gives), of this length, cut or padded with null. */
    static Object[] copyOfObjects(Object[] original, int newLength) {
        if (newLength < 0) throw new NegativeArraySizeException(Integer.toString(newLength));
        Object[] copy = new Object[newLength];
        for (int i = 0, n = Math.min(original.length, newLength); i < n; i++) copy[i] = original[i];
        return copy;
    }

    static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(".concat(Integer.toString(fromIndex)).concat(") > toIndex(").concat(Integer.toString(toIndex)).concat(")"));
        }
        if (fromIndex < 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: ".concat(Integer.toString(fromIndex)));
        if (toIndex > arrayLength) throw new ArrayIndexOutOfBoundsException("Array index out of range: ".concat(Integer.toString(toIndex)));
    }

    /** What copyOfRange checks, with the messages of the JDK's System.arraycopy, which it goes through. */
    static int copyRangeLength(int length, int from, int to, String type) {
        int newLength = to - from;
        if (newLength < 0) throw new IllegalArgumentException(Integer.toString(from).concat(" > ").concat(Integer.toString(to)));
        if (from < 0) {
            throw new ArrayIndexOutOfBoundsException("arraycopy: source index ".concat(Integer.toString(from)).concat(" out of bounds for ").concat(type)
                    .concat("[").concat(Integer.toString(length)).concat("]"));
        }
        int copied = Math.min(length - from, newLength);
        if (copied < 0) throw new ArrayIndexOutOfBoundsException("arraycopy: length ".concat(Integer.toString(copied)).concat(" is negative"));
        return copied;
    }

    @SafeVarargs
    public static <T> List<T> asList(T... a) {
        return new ArrayList<>(a);
    }

    /** The list asList gives: the array itself, whose size can't change. */
    private static class ArrayList<E> extends AbstractList<E> implements RandomAccess, java.io.Serializable {
        private final E[] a;

        ArrayList(E[] array) {
            a = Objects.requireNonNull(array);
        }

        public int size() {
            return a.length;
        }

        public Object[] toArray() {
            return copyOfObjects(a, a.length);
        }

        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] b) {
            int size = size();
            T[] r = b.length >= size ? b : (T[]) newArrayLike(b, size);
            for (int i = 0; i < size; i++) r[i] = (T) a[i];
            if (r.length > size) r[size] = null;
            return r;
        }

        public E get(int index) {
            return a[index];
        }

        public E set(int index, E element) {
            E oldValue = a[index];
            a[index] = element;
            return oldValue;
        }

        public int indexOf(Object o) {
            E[] a = this.a;
            if (o == null) {
                for (int i = 0; i < a.length; i++) if (a[i] == null) return i;
            } else {
                for (int i = 0; i < a.length; i++) if (o.equals(a[i])) return i;
            }
            return -1;
        }

        public boolean contains(Object o) {
            return indexOf(o) >= 0;
        }

        public void forEach(java.util.function.Consumer<? super E> action) {
            Objects.requireNonNull(action);
            for (E e : a) action.accept(e);
        }

        public void replaceAll(java.util.function.UnaryOperator<E> operator) {
            Objects.requireNonNull(operator);
            E[] a = this.a;
            for (int i = 0; i < a.length; i++) a[i] = operator.apply(a[i]);
        }

        public void sort(Comparator<? super E> c) {
            Arrays.sort(a, c);
        }

        public Iterator<E> iterator() {
            return new ArrayItr<>(a);
        }
    }

    /** Its remove is Iterator's default: UnsupportedOperationException("remove"). */
    private static final class ArrayItr<E> implements Iterator<E> {
        private int cursor;
        private final E[] a;

        ArrayItr(E[] a) {
            this.a = a;
        }

        public boolean hasNext() {
            return cursor < a.length;
        }

        public E next() {
            int i = cursor;
            if (i >= a.length) throw new NoSuchElementException();
            cursor = i + 1;
            return a[i];
        }
    }

    public static void fill(int[] a, int val) {
        for (int i = 0, len = a.length; i < len; i++) a[i] = val;
    }

    public static void fill(int[] a, int fromIndex, int toIndex, int val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static int hashCode(int[] a) {
        if (a == null) return 0;
        int result = 1;
        for (int e : a) result = 31 * result + e;
        return result;
    }

    public static boolean equals(int[] a, int[] a2) {
        if (a == a2) return true;
        if (a == null || a2 == null) return false;
        int length = a.length;
        if (a2.length != length) return false;
        for (int i = 0; i < length; i++) if (a[i] != a2[i]) return false;
        return true;
    }

    public static String toString(int[] a) {
        if (a == null) return "null";
        int iMax = a.length - 1;
        if (iMax == -1) return "[]";
        String b = "[";
        for (int i = 0;; i++) {
            b = b.concat(String.valueOf(a[i]));
            if (i == iMax) return b.concat("]");
            b = b.concat(", ");
        }
    }

    public static int[] copyOf(int[] original, int newLength) {
        if (newLength < 0) throw new NegativeArraySizeException(Integer.toString(newLength));
        int[] copy = new int[newLength];
        for (int i = 0, n = Math.min(original.length, newLength); i < n; i++) copy[i] = original[i];
        return copy;
    }

    public static int[] copyOfRange(int[] original, int from, int to) {
        int copied = copyRangeLength(original.length, from, to, "int");
        int[] copy = new int[to - from];
        for (int i = 0; i < copied; i++) copy[i] = original[from + i];
        return copy;
    }

    public static void sort(int[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(int[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        if (toIndex - fromIndex < 2) return;
        int[] work = new int[toIndex - fromIndex];
        mergeSort(a, work, fromIndex, toIndex);
    }

    private static void mergeSort(int[] a, int[] work, int from, int to) {
        if (to - from < 2) return;
        int mid = (from + to) >>> 1;
        mergeSort(a, work, from, mid);
        mergeSort(a, work, mid, to);
        int i = from, j = mid, k = 0;
        while (i < mid && j < to) {
            int x = a[j], y = a[i];
            if (x < y) work[k++] = a[j++];
            else work[k++] = a[i++];
        }
        while (i < mid) work[k++] = a[i++];
        while (j < to) work[k++] = a[j++];
        for (int m = 0; m < k; m++) a[from + m] = work[m];
    }

    public static void fill(long[] a, long val) {
        for (int i = 0, len = a.length; i < len; i++) a[i] = val;
    }

    public static void fill(long[] a, int fromIndex, int toIndex, long val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static int hashCode(long[] a) {
        if (a == null) return 0;
        int result = 1;
        for (long e : a) result = 31 * result + (int) (e ^ (e >>> 32));
        return result;
    }

    public static boolean equals(long[] a, long[] a2) {
        if (a == a2) return true;
        if (a == null || a2 == null) return false;
        int length = a.length;
        if (a2.length != length) return false;
        for (int i = 0; i < length; i++) if (a[i] != a2[i]) return false;
        return true;
    }

    public static String toString(long[] a) {
        if (a == null) return "null";
        int iMax = a.length - 1;
        if (iMax == -1) return "[]";
        String b = "[";
        for (int i = 0;; i++) {
            b = b.concat(String.valueOf(a[i]));
            if (i == iMax) return b.concat("]");
            b = b.concat(", ");
        }
    }

    public static long[] copyOf(long[] original, int newLength) {
        if (newLength < 0) throw new NegativeArraySizeException(Integer.toString(newLength));
        long[] copy = new long[newLength];
        for (int i = 0, n = Math.min(original.length, newLength); i < n; i++) copy[i] = original[i];
        return copy;
    }

    public static long[] copyOfRange(long[] original, int from, int to) {
        int copied = copyRangeLength(original.length, from, to, "long");
        long[] copy = new long[to - from];
        for (int i = 0; i < copied; i++) copy[i] = original[from + i];
        return copy;
    }

    public static void sort(long[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(long[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        if (toIndex - fromIndex < 2) return;
        long[] work = new long[toIndex - fromIndex];
        mergeSort(a, work, fromIndex, toIndex);
    }

    private static void mergeSort(long[] a, long[] work, int from, int to) {
        if (to - from < 2) return;
        int mid = (from + to) >>> 1;
        mergeSort(a, work, from, mid);
        mergeSort(a, work, mid, to);
        int i = from, j = mid, k = 0;
        while (i < mid && j < to) {
            long x = a[j], y = a[i];
            if (x < y) work[k++] = a[j++];
            else work[k++] = a[i++];
        }
        while (i < mid) work[k++] = a[i++];
        while (j < to) work[k++] = a[j++];
        for (int m = 0; m < k; m++) a[from + m] = work[m];
    }

    public static void fill(short[] a, short val) {
        for (int i = 0, len = a.length; i < len; i++) a[i] = val;
    }

    public static void fill(short[] a, int fromIndex, int toIndex, short val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static int hashCode(short[] a) {
        if (a == null) return 0;
        int result = 1;
        for (short e : a) result = 31 * result + e;
        return result;
    }

    public static boolean equals(short[] a, short[] a2) {
        if (a == a2) return true;
        if (a == null || a2 == null) return false;
        int length = a.length;
        if (a2.length != length) return false;
        for (int i = 0; i < length; i++) if (a[i] != a2[i]) return false;
        return true;
    }

    public static String toString(short[] a) {
        if (a == null) return "null";
        int iMax = a.length - 1;
        if (iMax == -1) return "[]";
        String b = "[";
        for (int i = 0;; i++) {
            b = b.concat(String.valueOf(a[i]));
            if (i == iMax) return b.concat("]");
            b = b.concat(", ");
        }
    }

    public static short[] copyOf(short[] original, int newLength) {
        if (newLength < 0) throw new NegativeArraySizeException(Integer.toString(newLength));
        short[] copy = new short[newLength];
        for (int i = 0, n = Math.min(original.length, newLength); i < n; i++) copy[i] = original[i];
        return copy;
    }

    public static short[] copyOfRange(short[] original, int from, int to) {
        int copied = copyRangeLength(original.length, from, to, "short");
        short[] copy = new short[to - from];
        for (int i = 0; i < copied; i++) copy[i] = original[from + i];
        return copy;
    }

    public static void sort(short[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(short[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        if (toIndex - fromIndex < 2) return;
        short[] work = new short[toIndex - fromIndex];
        mergeSort(a, work, fromIndex, toIndex);
    }

    private static void mergeSort(short[] a, short[] work, int from, int to) {
        if (to - from < 2) return;
        int mid = (from + to) >>> 1;
        mergeSort(a, work, from, mid);
        mergeSort(a, work, mid, to);
        int i = from, j = mid, k = 0;
        while (i < mid && j < to) {
            short x = a[j], y = a[i];
            if (x < y) work[k++] = a[j++];
            else work[k++] = a[i++];
        }
        while (i < mid) work[k++] = a[i++];
        while (j < to) work[k++] = a[j++];
        for (int m = 0; m < k; m++) a[from + m] = work[m];
    }

    public static void fill(char[] a, char val) {
        for (int i = 0, len = a.length; i < len; i++) a[i] = val;
    }

    public static void fill(char[] a, int fromIndex, int toIndex, char val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static int hashCode(char[] a) {
        if (a == null) return 0;
        int result = 1;
        for (char e : a) result = 31 * result + e;
        return result;
    }

    public static boolean equals(char[] a, char[] a2) {
        if (a == a2) return true;
        if (a == null || a2 == null) return false;
        int length = a.length;
        if (a2.length != length) return false;
        for (int i = 0; i < length; i++) if (a[i] != a2[i]) return false;
        return true;
    }

    public static String toString(char[] a) {
        if (a == null) return "null";
        int iMax = a.length - 1;
        if (iMax == -1) return "[]";
        String b = "[";
        for (int i = 0;; i++) {
            b = b.concat(String.valueOf(a[i]));
            if (i == iMax) return b.concat("]");
            b = b.concat(", ");
        }
    }

    public static char[] copyOf(char[] original, int newLength) {
        if (newLength < 0) throw new NegativeArraySizeException(Integer.toString(newLength));
        char[] copy = new char[newLength];
        for (int i = 0, n = Math.min(original.length, newLength); i < n; i++) copy[i] = original[i];
        return copy;
    }

    public static char[] copyOfRange(char[] original, int from, int to) {
        int copied = copyRangeLength(original.length, from, to, "char");
        char[] copy = new char[to - from];
        for (int i = 0; i < copied; i++) copy[i] = original[from + i];
        return copy;
    }

    public static void sort(char[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(char[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        if (toIndex - fromIndex < 2) return;
        char[] work = new char[toIndex - fromIndex];
        mergeSort(a, work, fromIndex, toIndex);
    }

    private static void mergeSort(char[] a, char[] work, int from, int to) {
        if (to - from < 2) return;
        int mid = (from + to) >>> 1;
        mergeSort(a, work, from, mid);
        mergeSort(a, work, mid, to);
        int i = from, j = mid, k = 0;
        while (i < mid && j < to) {
            char x = a[j], y = a[i];
            if (x < y) work[k++] = a[j++];
            else work[k++] = a[i++];
        }
        while (i < mid) work[k++] = a[i++];
        while (j < to) work[k++] = a[j++];
        for (int m = 0; m < k; m++) a[from + m] = work[m];
    }

    public static void fill(byte[] a, byte val) {
        for (int i = 0, len = a.length; i < len; i++) a[i] = val;
    }

    public static void fill(byte[] a, int fromIndex, int toIndex, byte val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static int hashCode(byte[] a) {
        if (a == null) return 0;
        int result = 1;
        for (byte e : a) result = 31 * result + e;
        return result;
    }

    public static boolean equals(byte[] a, byte[] a2) {
        if (a == a2) return true;
        if (a == null || a2 == null) return false;
        int length = a.length;
        if (a2.length != length) return false;
        for (int i = 0; i < length; i++) if (a[i] != a2[i]) return false;
        return true;
    }

    public static String toString(byte[] a) {
        if (a == null) return "null";
        int iMax = a.length - 1;
        if (iMax == -1) return "[]";
        String b = "[";
        for (int i = 0;; i++) {
            b = b.concat(String.valueOf(a[i]));
            if (i == iMax) return b.concat("]");
            b = b.concat(", ");
        }
    }

    public static byte[] copyOf(byte[] original, int newLength) {
        if (newLength < 0) throw new NegativeArraySizeException(Integer.toString(newLength));
        byte[] copy = new byte[newLength];
        for (int i = 0, n = Math.min(original.length, newLength); i < n; i++) copy[i] = original[i];
        return copy;
    }

    public static byte[] copyOfRange(byte[] original, int from, int to) {
        int copied = copyRangeLength(original.length, from, to, "byte");
        byte[] copy = new byte[to - from];
        for (int i = 0; i < copied; i++) copy[i] = original[from + i];
        return copy;
    }

    public static void sort(byte[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(byte[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        if (toIndex - fromIndex < 2) return;
        byte[] work = new byte[toIndex - fromIndex];
        mergeSort(a, work, fromIndex, toIndex);
    }

    private static void mergeSort(byte[] a, byte[] work, int from, int to) {
        if (to - from < 2) return;
        int mid = (from + to) >>> 1;
        mergeSort(a, work, from, mid);
        mergeSort(a, work, mid, to);
        int i = from, j = mid, k = 0;
        while (i < mid && j < to) {
            byte x = a[j], y = a[i];
            if (x < y) work[k++] = a[j++];
            else work[k++] = a[i++];
        }
        while (i < mid) work[k++] = a[i++];
        while (j < to) work[k++] = a[j++];
        for (int m = 0; m < k; m++) a[from + m] = work[m];
    }

    public static void fill(boolean[] a, boolean val) {
        for (int i = 0, len = a.length; i < len; i++) a[i] = val;
    }

    public static void fill(boolean[] a, int fromIndex, int toIndex, boolean val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static int hashCode(boolean[] a) {
        if (a == null) return 0;
        int result = 1;
        for (boolean e : a) result = 31 * result + (e ? 1231 : 1237);
        return result;
    }

    public static boolean equals(boolean[] a, boolean[] a2) {
        if (a == a2) return true;
        if (a == null || a2 == null) return false;
        int length = a.length;
        if (a2.length != length) return false;
        for (int i = 0; i < length; i++) if (a[i] != a2[i]) return false;
        return true;
    }

    public static String toString(boolean[] a) {
        if (a == null) return "null";
        int iMax = a.length - 1;
        if (iMax == -1) return "[]";
        String b = "[";
        for (int i = 0;; i++) {
            b = b.concat(String.valueOf(a[i]));
            if (i == iMax) return b.concat("]");
            b = b.concat(", ");
        }
    }

    public static boolean[] copyOf(boolean[] original, int newLength) {
        if (newLength < 0) throw new NegativeArraySizeException(Integer.toString(newLength));
        boolean[] copy = new boolean[newLength];
        for (int i = 0, n = Math.min(original.length, newLength); i < n; i++) copy[i] = original[i];
        return copy;
    }

    public static boolean[] copyOfRange(boolean[] original, int from, int to) {
        int copied = copyRangeLength(original.length, from, to, "boolean");
        boolean[] copy = new boolean[to - from];
        for (int i = 0; i < copied; i++) copy[i] = original[from + i];
        return copy;
    }

    public static void fill(float[] a, float val) {
        for (int i = 0, len = a.length; i < len; i++) a[i] = val;
    }

    public static void fill(float[] a, int fromIndex, int toIndex, float val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static int hashCode(float[] a) {
        if (a == null) return 0;
        int result = 1;
        for (float e : a) result = 31 * result + Float.floatToIntBits(e);
        return result;
    }

    public static boolean equals(float[] a, float[] a2) {
        if (a == a2) return true;
        if (a == null || a2 == null) return false;
        int length = a.length;
        if (a2.length != length) return false;
        for (int i = 0; i < length; i++) if (Float.floatToIntBits(a[i]) != Float.floatToIntBits(a2[i])) return false;
        return true;
    }

    public static String toString(float[] a) {
        if (a == null) return "null";
        int iMax = a.length - 1;
        if (iMax == -1) return "[]";
        String b = "[";
        for (int i = 0;; i++) {
            b = b.concat(String.valueOf(a[i]));
            if (i == iMax) return b.concat("]");
            b = b.concat(", ");
        }
    }

    public static float[] copyOf(float[] original, int newLength) {
        if (newLength < 0) throw new NegativeArraySizeException(Integer.toString(newLength));
        float[] copy = new float[newLength];
        for (int i = 0, n = Math.min(original.length, newLength); i < n; i++) copy[i] = original[i];
        return copy;
    }

    public static float[] copyOfRange(float[] original, int from, int to) {
        int copied = copyRangeLength(original.length, from, to, "float");
        float[] copy = new float[to - from];
        for (int i = 0; i < copied; i++) copy[i] = original[from + i];
        return copy;
    }

    public static void sort(float[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(float[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        if (toIndex - fromIndex < 2) return;
        float[] work = new float[toIndex - fromIndex];
        mergeSort(a, work, fromIndex, toIndex);
    }

    private static void mergeSort(float[] a, float[] work, int from, int to) {
        if (to - from < 2) return;
        int mid = (from + to) >>> 1;
        mergeSort(a, work, from, mid);
        mergeSort(a, work, mid, to);
        int i = from, j = mid, k = 0;
        while (i < mid && j < to) {
            float x = a[j], y = a[i];
            if (Float.compare(x, y) < 0) work[k++] = a[j++];
            else work[k++] = a[i++];
        }
        while (i < mid) work[k++] = a[i++];
        while (j < to) work[k++] = a[j++];
        for (int m = 0; m < k; m++) a[from + m] = work[m];
    }

    public static void fill(double[] a, double val) {
        for (int i = 0, len = a.length; i < len; i++) a[i] = val;
    }

    public static void fill(double[] a, int fromIndex, int toIndex, double val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static int hashCode(double[] a) {
        if (a == null) return 0;
        int result = 1;
        for (double e : a) result = 31 * result + (int) (Double.doubleToLongBits(e) ^ (Double.doubleToLongBits(e) >>> 32));
        return result;
    }

    public static boolean equals(double[] a, double[] a2) {
        if (a == a2) return true;
        if (a == null || a2 == null) return false;
        int length = a.length;
        if (a2.length != length) return false;
        for (int i = 0; i < length; i++) if (Double.doubleToLongBits(a[i]) != Double.doubleToLongBits(a2[i])) return false;
        return true;
    }

    public static String toString(double[] a) {
        if (a == null) return "null";
        int iMax = a.length - 1;
        if (iMax == -1) return "[]";
        String b = "[";
        for (int i = 0;; i++) {
            b = b.concat(String.valueOf(a[i]));
            if (i == iMax) return b.concat("]");
            b = b.concat(", ");
        }
    }

    public static double[] copyOf(double[] original, int newLength) {
        if (newLength < 0) throw new NegativeArraySizeException(Integer.toString(newLength));
        double[] copy = new double[newLength];
        for (int i = 0, n = Math.min(original.length, newLength); i < n; i++) copy[i] = original[i];
        return copy;
    }

    public static double[] copyOfRange(double[] original, int from, int to) {
        int copied = copyRangeLength(original.length, from, to, "double");
        double[] copy = new double[to - from];
        for (int i = 0; i < copied; i++) copy[i] = original[from + i];
        return copy;
    }

    public static void sort(double[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(double[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        if (toIndex - fromIndex < 2) return;
        double[] work = new double[toIndex - fromIndex];
        mergeSort(a, work, fromIndex, toIndex);
    }

    private static void mergeSort(double[] a, double[] work, int from, int to) {
        if (to - from < 2) return;
        int mid = (from + to) >>> 1;
        mergeSort(a, work, from, mid);
        mergeSort(a, work, mid, to);
        int i = from, j = mid, k = 0;
        while (i < mid && j < to) {
            double x = a[j], y = a[i];
            if (Double.compare(x, y) < 0) work[k++] = a[j++];
            else work[k++] = a[i++];
        }
        while (i < mid) work[k++] = a[i++];
        while (j < to) work[k++] = a[j++];
        for (int m = 0; m < k; m++) a[from + m] = work[m];
    }

    public static void fill(Object[] a, Object val) {
        for (int i = 0, len = a.length; i < len; i++) a[i] = val;
    }

    public static void fill(Object[] a, int fromIndex, int toIndex, Object val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    public static int hashCode(Object[] a) {
        if (a == null) return 0;
        int result = 1;
        for (Object element : a) result = 31 * result + (element == null ? 0 : element.hashCode());
        return result;
    }

    public static boolean equals(Object[] a, Object[] a2) {
        if (a == a2) return true;
        if (a == null || a2 == null) return false;
        int length = a.length;
        if (a2.length != length) return false;
        for (int i = 0; i < length; i++) if (!Objects.equals(a[i], a2[i])) return false;
        return true;
    }

    public static String toString(Object[] a) {
        if (a == null) return "null";
        int iMax = a.length - 1;
        if (iMax == -1) return "[]";
        String b = "[";
        for (int i = 0;; i++) {
            b = b.concat(String.valueOf(a[i]));
            if (i == iMax) return b.concat("]");
            b = b.concat(", ");
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] copyOf(T[] original, int newLength) {
        if (newLength < 0) throw new NegativeArraySizeException(Integer.toString(newLength));
        T[] copy = (T[]) newArrayLike(original, newLength);
        for (int i = 0, n = Math.min(original.length, newLength); i < n; i++) copy[i] = original[i];
        return copy;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] copyOfRange(T[] original, int from, int to) {
        int copied = copyRangeLength(original.length, from, to, "object array");
        T[] copy = (T[]) newArrayLike(original, to - from);
        for (int i = 0; i < copied; i++) copy[i] = original[from + i];
        return copy;
    }

    public static void sort(Object[] a) {
        sort(a, 0, a.length, null);
    }

    public static void sort(Object[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex, null);
    }

    public static <T> void sort(T[] a, Comparator<? super T> c) {
        sort(a, 0, a.length, c);
    }

    /** Stable; with no comparator, the elements' compareTo (ClassCastException when one isn't Comparable). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> void sort(T[] a, int fromIndex, int toIndex, Comparator<? super T> c) {
        rangeCheck(a.length, fromIndex, toIndex);
        if (toIndex - fromIndex < 2) return;
        Comparator<Object> cmp = c != null ? (Comparator) c : (Comparator) Comparators.NaturalOrderComparator.INSTANCE;
        Object[] work = new Object[toIndex - fromIndex];
        mergeSort(a, work, fromIndex, toIndex, cmp);
    }

    private static void mergeSort(Object[] a, Object[] work, int from, int to, Comparator<Object> c) {
        if (to - from < 2) return;
        int mid = (from + to) >>> 1;
        mergeSort(a, work, from, mid, c);
        mergeSort(a, work, mid, to, c);
        int i = from, j = mid, k = 0;
        while (i < mid && j < to) {
            if (c.compare(a[j], a[i]) < 0) work[k++] = a[j++];
            else work[k++] = a[i++];
        }
        while (i < mid) work[k++] = a[i++];
        while (j < to) work[k++] = a[j++];
        for (int m = 0; m < k; m++) a[from + m] = work[m];
    }

    public static <T> void setAll(T[] array, IntFunction<? extends T> generator) {
        Objects.requireNonNull(generator);
        for (int i = 0; i < array.length; i++) array[i] = generator.apply(i);
    }
}
