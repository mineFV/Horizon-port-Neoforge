package java.lang;

/** The model of java.lang.Void: no object, only the class object of void. */
public final class Void {
    @SuppressWarnings("unchecked")
    public static final Class<Void> TYPE = (Class<Void>) Class.jPrimitive('V');

    private Void() {
    }
}
