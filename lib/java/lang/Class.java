package java.lang;

/**
 * The model of java.lang.Class: a type of the translation's world, by the runtime's id for it. There is one object
 * for each type (`Foo.class == foo.getClass()`), made by the runtime. Its names, supertypes and enum constants come
 * from tables the translator writes. A primitive type's object (int.class, Integer.TYPE) is one too. Not modelled:
 * reflection on members, annotations, getComponentType, getPackage, records' components.
 */
public final class Class<T> implements java.io.Serializable {
    private final int id;

    private Class(int id) {
        this.id = id;
    }

    public String getName() {
        return jName(id);
    }

    /** As the source names it: "" for an anonymous class, "String[]" for an array. */
    public String getSimpleName() {
        return jSimpleName(id);
    }

    /** null for a local or anonymous class, or an array of one. */
    public String getCanonicalName() {
        return jCanonicalName(id);
    }

    public String getTypeName() {
        String name = getName();
        if (!isArray()) return name;
        int dimensions = 0;
        String inner = name;
        while (inner.startsWith("[")) {
            inner = inner.substring(1);
            dimensions++;
        }
        String base = inner.startsWith("L") ? inner.substring(1, inner.length() - 1) : primitive(inner.charAt(0));
        for (int i = 0; i < dimensions; i++) base = base.concat("[]");
        return base;
    }

    static String primitive(char k) {
        return switch (k) {
            case 'Z' -> "boolean";
            case 'B' -> "byte";
            case 'C' -> "char";
            case 'S' -> "short";
            case 'I' -> "int";
            case 'J' -> "long";
            case 'F' -> "float";
            default -> "double";
        };
    }

    public boolean isInstance(Object obj) {
        return obj != null && jInstance(id, obj);
    }

    public boolean isAssignableFrom(Class<?> cls) {
        return jAssignable(id, cls.id);
    }

    @SuppressWarnings("unchecked")
    public T cast(Object obj) {
        if (obj != null && !isInstance(obj)) throw new ClassCastException("Cannot cast ".concat(obj.getClass().getName()).concat(" to ").concat(getName()));
        return (T) obj;
    }

    public boolean isInterface() {
        return (jFlags(id) & 1) != 0;
    }

    /** Only the enum's own class, not a constant's body (as the JDK's). */
    public boolean isEnum() {
        return (jFlags(id) & 2) != 0;
    }

    public boolean isArray() {
        return (jFlags(id) & 4) != 0;
    }

    public boolean isPrimitive() {
        return (jFlags(id) & 8) != 0;
    }

    /** null for Object, an interface; Object for an array. */
    @SuppressWarnings("unchecked")
    public Class<? super T> getSuperclass() {
        return (Class<? super T>) jSuper(id);
    }

    /** Its constants in order, a new array each time; null when it isn't an enum. */
    @SuppressWarnings("unchecked")
    public T[] getEnumConstants() {
        return (T[]) jEnumValues(id);
    }

    /**
     * getEnumConstants as an array of Enum, for Enum.valueOf, EnumMap and EnumSet: the JDK's own array is of the
     * enum's type, which a cast to the generic type would test against every array there is. Not the JDK's API: a
     * mod, built against the JDK, can't call it.
     */
    public Enum<?>[] jEnumUniverse() {
        return jEnumValues(id);
    }

    /** Assertions are off, as a JVM's are unless it is told otherwise. */
    public boolean desiredAssertionStatus() {
        return false;
    }

    public String toString() {
        return (isInterface() ? "interface " : isPrimitive() ? "" : "class ").concat(getName());
    }

    private static native String jName(int id);

    private static native String jSimpleName(int id);

    private static native String jCanonicalName(int id);

    private static native boolean jInstance(int id, Object obj);

    private static native boolean jAssignable(int id, int other);

    private static native int jFlags(int id);

    private static native Class<?> jSuper(int id);

    /** The enum's values(), or null when it isn't an enum: the translator asks for every enum's values() it may need. */
    private static native Enum<?>[] jEnumValues(int id);

    /** The object of a primitive type or void, by its descriptor: Integer.TYPE and the like. */
    static native Class<?> jPrimitive(char descriptor);
}
