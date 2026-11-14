package java.lang;

/** The model of java.lang.Boolean: its two objects, as valueOf gives them. */
public final class Boolean implements java.io.Serializable, Comparable<Boolean> {
    public static final Boolean TRUE = new Boolean(true);
    public static final Boolean FALSE = new Boolean(false);
    private final boolean value;

    private Boolean(boolean value) {
        this.value = value;
    }

    public static Boolean valueOf(boolean b) {
        return b ? TRUE : FALSE;
    }

    public boolean booleanValue() {
        return value;
    }

    public static String toString(boolean b) {
        return b ? "true" : "false";
    }

    public String toString() {
        return value ? "true" : "false";
    }

    public static int hashCode(boolean value) {
        return value ? 1231 : 1237;
    }

    public int hashCode() {
        return hashCode(value);
    }

    public boolean equals(Object obj) {
        return obj instanceof Boolean b && value == b.value;
    }

    public static int compare(boolean x, boolean y) {
        return x == y ? 0 : (x ? 1 : -1);
    }

    public int compareTo(Boolean b) {
        return compare(value, b.value);
    }

    public static boolean logicalAnd(boolean a, boolean b) {
        return a && b;
    }

    public static boolean logicalOr(boolean a, boolean b) {
        return a || b;
    }

    public static boolean logicalXor(boolean a, boolean b) {
        return a ^ b;
    }
}
