package java.nio.file;

/**
 * The model of java.nio.file.Path: a path of the default file system, as the JDK's on Linux and macOS (sun.nio.fs
 * .UnixPath, observed). Not modelled: Windows' paths (refused there), the file system, watching, URIs, File,
 * subpath, startsWith, endsWith, relativize.
 */
public interface Path extends Comparable<Path>, Iterable<Path> {
    static Path of(String first, String... more) {
        return sun.nio.fs.UnixPath.jOf(first, more);
    }

    boolean isAbsolute();

    Path getRoot();

    Path getFileName();

    Path getParent();

    int getNameCount();

    Path getName(int index);

    Path normalize();

    Path resolve(Path other);

    default Path resolve(String other) {
        return resolve(of(other));
    }

    Path toAbsolutePath();

    java.util.Iterator<Path> iterator();

    int compareTo(Path other);

    boolean equals(Object other);

    int hashCode();

    String toString();
}
