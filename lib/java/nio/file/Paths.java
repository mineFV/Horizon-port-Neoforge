package java.nio.file;

/** The model of java.nio.file.Paths: Path.of under its older name. */
public final class Paths {
    private Paths() {}

    public static Path get(String first, String... more) {
        return Path.of(first, more);
    }
}
