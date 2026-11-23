package java.nio.file.attribute;

/** The model of java.nio.file.attribute.FileAttribute: a type (Files refuses one given). */
public interface FileAttribute<T> {
    String name();

    T value();
}
