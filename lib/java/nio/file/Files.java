package java.nio.file;

/**
 * The model of java.nio.file.Files: whether a path is a directory, and making one with its parents. Where the system
 * refuses (a file in the way, no permission...), the JDK's exceptions aren't made: refused, saying so. Not modelled:
 * the rest (reading, writing, listing, attributes).
 */
public final class Files {
    private Files() {}

    public static boolean isDirectory(Path path, LinkOption... options) {
        return jIsDirectory(path.toAbsolutePath().toString(), options.length > 0);
    }

    public static Path createDirectories(Path dir, java.nio.file.attribute.FileAttribute<?>... attrs) throws java.io.IOException {
        if (attrs.length > 0) throw new UnsupportedOperationException("Files.createDirectories with attributes isn't modelled (horizon-port-neoforge)");
        String problem = jCreateDirectories(dir.toAbsolutePath().toString());
        if (problem != null) {
            throw new UnsupportedOperationException("making the directory " + dir + " failed, which isn't modelled (horizon-port-neoforge): " + problem);
        }
        return dir;
    }

    private static native boolean jIsDirectory(String path, boolean noFollow);

    /** null once the directory is there, or why it couldn't be made. */
    private static native String jCreateDirectories(String path);
}
