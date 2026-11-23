package com.example.numbers;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Paths and folders (lib/README.md): what Path.of makes of texts, their parts, normalize, resolve, their order and
 * hash, the errors; Files making a folder. Texts that aren't ASCII depend on the system's charset: the test runs them
 * in several environments (unicode).
 */
public final class Folders {
    private Folders() {}

    static final String[] TEXTS = {"", "a", "a/b", "a//b", "a/b/", "/", "//", "/a", "/a/", "///a//b//", ".", "..", "a/./b", "a/../b",
        "../a", "/..", "/a/..", "a/b/../..", "a/b/../../..", " ", "a b/c", "config", "config/fml.toml", "x/./../y", "Mekanism"};

    static final String[] UNICODE = {"é/ü", "日本/語", "a/😀/b", "Ünïcödé//x/", "ü/../é"};

    public static int count() {
        return TEXTS.length;
    }

    public static String parts(int i) {
        return describe(Path.of(TEXTS[i]));
    }

    public static String pair(int i, int j) {
        Path a = Path.of(TEXTS[i]), b = Path.of(TEXTS[j]);
        return show(a.resolve(b)) + " " + show(a.resolve(TEXTS[j])) + " " + show(Path.of(TEXTS[i], TEXTS[j])) + " "
                + show(Paths.get(TEXTS[i], TEXTS[j], "z")) + " " + a.compareTo(b) + " " + a.equals(b) + " " + (a.hashCode() == b.hashCode());
    }

    public static String unicode() {
        StringBuilder out = new StringBuilder();
        for (String text : UNICODE) {
            Path p = Path.of(text);
            out.append(describe(p)).append(" ").append(p.compareTo(Path.of("é"))).append(" ").append(show(p.resolve("ä"))).append("; ");
        }
        return out.toString();
    }

    static String describe(Path p) {
        StringBuilder names = new StringBuilder();
        for (Path name : p) names.append('<').append(name).append('>');
        String nameAt;
        try {
            nameAt = show(p.getName(p.getNameCount()));
        } catch (IllegalArgumentException e) {
            nameAt = "throws " + e.getClass().getName() + " " + e.getMessage();
        }
        return "[" + p + "] " + show(p.getFileName()) + " " + show(p.getParent()) + " " + show(p.getRoot()) + " " + p.isAbsolute() + " "
                + p.getNameCount() + " " + show(p.normalize()) + " " + p.hashCode() + " " + p.getClass().getName() + " " + names + " " + nameAt
                + " " + p.equals(Path.of(p.toString())) + " " + p.equals(p.toString()) + " " + p.toAbsolutePath().isAbsolute() + " "
                + p.toAbsolutePath().equals(Path.of("").toAbsolutePath().resolve(p)) + " " + show(p.getFileName() == null ? null : p.getFileName().getFileName());
    }

    public static String errors() {
        StringBuilder out = new StringBuilder();
        for (String text : new String[] {"a\u0000b", "\u0000", "/x/\u0000"}) {
            try {
                out.append(show(Path.of(text)));
            } catch (java.nio.file.InvalidPathException e) {
                out.append(e.getClass().getName()).append(": ").append(e.getMessage()).append(" | ").append(e.getReason()).append(" | ")
                        .append(e.getIndex()).append(" | ").append(e.getInput().length()).append(" | ").append(e instanceof IllegalArgumentException);
            }
            out.append("; ");
        }
        return out.toString();
    }

    /** A folder made with its parents under base, twice, and what is said of it before and after. */
    public static String made(String base) throws java.io.IOException {
        Path root = Path.of(base);
        Path inner = root.resolve("made").resolve("inner/deep");
        String before = Files.isDirectory(inner) + " " + Files.isDirectory(root) + " " + Files.isDirectory(root.resolve("made"));
        Path made = Files.createDirectories(inner);
        Path again = Files.createDirectories(inner);
        return before + " | " + (made == inner) + " " + (again == inner) + " " + Files.isDirectory(inner) + " "
                + Files.isDirectory(inner, LinkOption.NOFOLLOW_LINKS) + " " + Files.isDirectory(inner.getParent()) + " "
                + Files.isDirectory(root.resolve("nothing"));
    }

    static String show(Path p) {
        return p == null ? "null" : "[" + p + "]";
    }
}
