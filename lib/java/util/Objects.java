package java.util;

/** The model of java.util.Objects, with the JDK's messages. */
public final class Objects {
    private Objects() {}

    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    public static int hashCode(Object o) {
        return o != null ? o.hashCode() : 0;
    }

    /** Arrays.hashCode of the values: 31 times the hash so far, plus each one's. */
    public static int hash(Object... values) {
        if (values == null) return 0;
        int result = 1;
        for (Object element : values) result = 31 * result + (element == null ? 0 : element.hashCode());
        return result;
    }

    public static String toString(Object o) {
        return String.valueOf(o);
    }

    public static String toString(Object o, String nullDefault) {
        return (o != null) ? o.toString() : nullDefault;
    }

    public static boolean isNull(Object obj) {
        return obj == null;
    }

    public static boolean nonNull(Object obj) {
        return obj != null;
    }

    public static <T> T requireNonNull(T obj) {
        if (obj == null) throw new NullPointerException();
        return obj;
    }

    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null) throw new NullPointerException(message);
        return obj;
    }

    public static <T> T requireNonNull(T obj, java.util.function.Supplier<String> messageSupplier) {
        if (obj == null) throw new NullPointerException(messageSupplier == null ? null : messageSupplier.get());
        return obj;
    }

    public static <T> T requireNonNullElse(T obj, T defaultObj) {
        return (obj != null) ? obj : requireNonNull(defaultObj, "defaultObj");
    }

    public static int checkIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index ".concat(Integer.toString(index)).concat(" out of bounds for length ").concat(Integer.toString(length)));
        }
        return index;
    }
}
