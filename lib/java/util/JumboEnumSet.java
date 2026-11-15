package java.util;

/** The model's EnumSet for an enum of more than 64 constants: the JDK's class name; the bits are EnumSet's own. */
final class JumboEnumSet<E extends Enum<E>> extends EnumSet<E> {
    JumboEnumSet(Class<E> elementType, Enum<?>[] universe) {
        super(elementType, universe);
    }
}
