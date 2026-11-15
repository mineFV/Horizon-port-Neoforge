package java.lang;

/**
 * The model of java.lang.Enum: a constant's name and ordinal, its class, valueOf. compareTo doesn't check that both
 * constants are of one enum, which generic code can't break.
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

    @SuppressWarnings("unchecked")
    public final Class<E> getDeclaringClass() {
        Class<?> clazz = getClass();
        Class<?> zuper = clazz.getSuperclass();
        return (Class<E>) ((zuper == Enum.class) ? clazz : zuper);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T valueOf(Class<T> enumClass, String name) {
        Enum<?>[] universe = enumClass.jEnumUniverse();
        if (universe == null) throw new IllegalArgumentException(enumClass.getName().concat(" is not an enum class"));
        if (name == null) throw new NullPointerException("Name is null");
        for (Enum<?> constant : universe) if (constant.name().equals(name)) return (T) constant;
        throw new IllegalArgumentException("No enum constant ".concat(String.valueOf(enumClass.getCanonicalName())).concat(".").concat(name));
    }

    public final int compareTo(E o) {
        Enum<?> other = o;
        return ordinal - other.ordinal;
    }
}
