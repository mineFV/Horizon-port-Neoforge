package sun.nio.fs;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * The model of the JDK's sun.nio.fs.UnixPath, the Path of Linux and macOS, observed (2026-11-23): its text with each
 * run of slashes one and no slash at the end but the root's; its names, parent and root by the slashes (the empty
 * path one name, the root none); normalize dropping "." and a name before ".."; resolve; toAbsolutePath against the
 * current directory; its order byte by byte and its hash over its bytes, which are the text's in the system's charset
 * (LC_CTYPE's). A text that isn't ASCII is followed where that charset is UTF-8 (refused where it isn't, or can't be
 * told); another system than a Unix one is refused.
 */
public final class UnixPath implements Path {
    private final String path;

    private UnixPath(String path) {
        this.path = path;
    }

    /** Path.of: the parts that aren't empty after the first, joined by slashes, then normalized and checked. */
    public static Path jOf(String first, String... more) {
        if (!jUnix()) throw new UnsupportedOperationException("a path of another system than a Unix one isn't modelled (horizon-port-neoforge)");
        String text = first;
        if (more.length > 0) {
            StringBuilder b = new StringBuilder(first);
            for (String segment : more) {
                if (segment.isEmpty()) continue;
                if (b.length() > 0) b.append('/');
                b.append(segment);
            }
            text = b.toString();
        }
        return new UnixPath(checked(text));
    }

    /** The text with its runs of slashes made one and no slash at the end (but the root's), checked. */
    private static String checked(String input) {
        StringBuilder b = new StringBuilder(input.length());
        char previous = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == 0) throw new InvalidPathException(input, "Nul character not allowed");
            if (c == '/' && previous == '/') continue;
            b.append(c);
            previous = c;
        }
        if (b.length() > 1 && b.charAt(b.length() - 1) == '/') b.setLength(b.length() - 1);
        String text = b.toString();
        ascii(text);
        return text;
    }

    /** A text that isn't ASCII is the JDK's only where the system's charset is UTF-8 (and the text is whole). */
    private static void ascii(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 0x80) continue;
            if (jCharset() != 1) {
                throw new UnsupportedOperationException("a path that isn't ASCII, where the system's charset isn't UTF-8 or can't be told, isn't modelled (horizon-port-neoforge)");
            }
            if (Character.isHighSurrogate(c) && i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
                i++;
            } else if (Character.isSurrogate(c)) {
                throw new UnsupportedOperationException("a path with half a surrogate pair isn't modelled (horizon-port-neoforge)");
            }
        }
    }

    private byte[] bytes() {
        return path.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /** The names: the empty path's is itself, the root has none. */
    private String[] names() {
        if (path.isEmpty()) return new String[] {""};
        String rest = path.startsWith("/") ? path.substring(1) : path;
        if (rest.isEmpty()) return new String[0];
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        int start = 0;
        for (int i = rest.indexOf('/'); i >= 0; i = rest.indexOf('/', start)) {
            names.add(rest.substring(start, i));
            start = i + 1;
        }
        names.add(rest.substring(start));
        return names.toArray(new String[0]);
    }

    @Override
    public boolean isAbsolute() {
        return path.startsWith("/");
    }

    @Override
    public Path getRoot() {
        return isAbsolute() ? new UnixPath("/") : null;
    }

    @Override
    public Path getFileName() {
        if (path.isEmpty()) return this;
        String[] names = names();
        return names.length == 0 ? null : new UnixPath(names[names.length - 1]);
    }

    @Override
    public Path getParent() {
        String[] names = names();
        if (names.length == 0) return null;
        if (names.length == 1) return isAbsolute() ? new UnixPath("/") : null;
        return new UnixPath(path.substring(0, path.lastIndexOf('/')));
    }

    @Override
    public int getNameCount() {
        return names().length;
    }

    @Override
    public Path getName(int index) {
        String[] names = names();
        if (index < 0 || index >= names.length) throw new IllegalArgumentException();
        return new UnixPath(names[index]);
    }

    @Override
    public Path normalize() {
        java.util.ArrayList<String> kept = new java.util.ArrayList<>();
        for (String name : names()) {
            if (name.equals(".") || name.isEmpty()) continue;
            if (name.equals("..")) {
                if (!kept.isEmpty() && !kept.get(kept.size() - 1).equals("..")) {
                    kept.remove(kept.size() - 1);
                    continue;
                }
                if (isAbsolute()) continue;
            }
            kept.add(name);
        }
        String joined = String.join("/", kept);
        return new UnixPath(isAbsolute() ? "/" + joined : joined);
    }

    @Override
    public Path resolve(Path other) {
        UnixPath o = (UnixPath) other;
        if (o.isAbsolute()) return o;
        if (o.path.isEmpty()) return this;
        if (path.isEmpty()) return o;
        return new UnixPath(path.equals("/") ? "/" + o.path : path + "/" + o.path);
    }

    @Override
    public Path toAbsolutePath() {
        if (isAbsolute()) return this;
        String directory = jCurrentDirectory();
        if (directory == null) throw new UnsupportedOperationException("the current directory isn't a UTF-8 text (horizon-port-neoforge)");
        return new UnixPath(checked(directory)).resolve(this);
    }

    @Override
    public java.util.Iterator<Path> iterator() {
        java.util.ArrayList<Path> names = new java.util.ArrayList<>();
        for (String name : names()) names.add(new UnixPath(name));
        return names.iterator();
    }

    /** Byte by byte, unsigned: the first two that differ, else the lengths. */
    @Override
    public int compareTo(Path other) {
        byte[] a = bytes(), b = ((UnixPath) other).bytes();
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int x = a[i] & 0xff, y = b[i] & 0xff;
            if (x != y) return x - y;
        }
        return a.length - b.length;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof UnixPath p && compareTo(p) == 0;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (byte b : bytes()) h = 31 * h + (b & 0xff);
        return h;
    }

    @Override
    public String toString() {
        return path;
    }

    /** Whether the system is a Unix one (Linux, macOS). */
    private static native boolean jUnix();

    /** The system's charset as the JDK reads it (LC_CTYPE's codeset): 1 for UTF-8, 0 for another, 2 where it can't be told. */
    private static native int jCharset();

    /** The current directory as a UTF-8 text; null where it isn't one. */
    private static native String jCurrentDirectory();
}
