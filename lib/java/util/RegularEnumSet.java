package java.util;

/** The model's EnumSet for an enum of 64 constants or fewer: the JDK's class name; the bits are EnumSet's own. */
final class RegularEnumSet<E extends Enum<E>> extends EnumSet<E> {
    RegularEnumSet(Class<E> elementType, Enum<?>[] universe) {
        super(elementType, universe);
    }
}
