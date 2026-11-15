package java.lang;

/** The model of java.lang.TypeNotPresentException. */
public class TypeNotPresentException extends RuntimeException {
    private final String typeName;

    public TypeNotPresentException(String typeName, Throwable cause) {
        super("Type ".concat(typeName).concat(" not present"), cause);
        this.typeName = typeName;
    }

    public String typeName() {
        return typeName;
    }
}
