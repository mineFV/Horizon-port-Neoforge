package java.lang;

/**
 * The model of java.lang.Enum: a constant's name and ordinal. Not modelled yet: getDeclaringClass, valueOf (both need
 * Class objects); compareTo doesn't check that both constants are of one enum, which generic code can't break.
 */
public abstract class Enum<E extends Enum<E>> implements Comparable<E>, java.io.Serializable {
    private final String name;
    private final int ordinal;

    protected Enum(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    public final String name() {
        return name;
    }

    public final int ordinal() {
        return ordinal;
    }

    public String toString() {
        return name;
    }

    public final boolean equals(Object other) {
        return this == other;
    }

    public final int hashCode() {
        return super.hashCode();
    }

    public final int compareTo(E o) {
        Enum<?> other = o;
        return ordinal - other.ordinal;
    }
}
