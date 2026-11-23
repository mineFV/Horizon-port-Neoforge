package net.neoforged.fml.loading;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The stand-in for FML's FMLPaths: the game's folders, under the one the server runs in, as Horizon's game folder is
 * the one it runs in by default. Observed in a server (`standins --horizon-src`, probe/ChunkProbe.java): GAMEDIR is
 * the current directory, the others its "mods", "config" and "config/fml.toml"; getOrCreateGameRelativePath makes the
 * folder and gives it. Not here: setup and loadAbsolutePaths, which FML's launcher runs.
 */
public enum FMLPaths {
    GAMEDIR(""),
    MODSDIR("mods"),
    CONFIGDIR("config"),
    FMLCONFIG("config", "fml.toml");

    private final Path relativePath;

    FMLPaths(String first, String... more) {
        this.relativePath = Path.of(first, more);
    }

    public Path relative() {
        return relativePath;
    }

    public Path get() {
        return Path.of("").toAbsolutePath().resolve(relativePath).normalize();
    }

    public static Path getOrCreateGameRelativePath(Path path) {
        Path made = GAMEDIR.get().resolve(path);
        if (!Files.isDirectory(made)) {
            try {
                Files.createDirectories(made);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
        return made;
    }
}
