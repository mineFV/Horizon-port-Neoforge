package horizonneo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * Horizon's own two tools for a mod's content, run as programs of their own (both MIT, built from
 * Horizon's source with `cargo build --release -p horizon-port-pack -p cargo-horizon-mod`):
 *
 * - `horizon-port-pack check <jar> <out>` carries a jar's data and assets over into a folder content
 *   mod, as Horizon's launcher does when it installs a Java mod;
 * - `cargo-horizon-mod horizon-mod build` packs such a folder as a `.hmod`, with the checks the game
 *   makes when it reads the content, and `... check <file>` runs the registry's checks on it.
 *
 * This tool never builds them: that is cargo's job, and the user's to start.
 */
final class HorizonTools {
    /** The environment variable naming the folder that holds both. */
    static final String VARIABLE = "HORIZON_TOOLS";

    /** How to get them, for messages. */
    static final String HOW = "cargo build --release -p horizon-port-pack -p cargo-horizon-mod, in Horizon's source";

    /** One run: its exit code and what it printed. */
    record Run(int exit, String out, String err) {}

    private final Path portPack, horizonMod;

    private HorizonTools(Path portPack, Path horizonMod) {
        this.portPack = portPack;
        this.horizonMod = horizonMod;
    }

    /** The folder both are in. */
    Path folder() {
        return portPack.getParent();
    }

    /**
     * The tools: in the folder given (`--horizon-tools`), else the one HORIZON_TOOLS names, else where
     * cargo builds them in the Horizon source (`target/release`, then `target/debug`). null when
     * they aren't built there; a folder that was named and doesn't hold them is an error.
     */
    static HorizonTools locate(String given, Path horizonSrc) throws IOException {
        String named = given != null ? given : System.getenv(VARIABLE);
        if (named != null && !named.isBlank()) {
            HorizonTools in = in(Path.of(named));
            if (in == null) {
                throw new IOException((given != null ? "--horizon-tools" : VARIABLE) + ": " + named + " doesn't hold horizon-port-pack and cargo-horizon-mod (" + HOW + ")");
            }
            return in;
        }
        if (horizonSrc == null || !Files.isDirectory(horizonSrc)) return null;
        for (String build : List.of("release", "debug")) {
            HorizonTools in = in(horizonSrc.resolve("target").resolve(build));
            if (in != null) return in;
        }
        return null;
    }

    private static HorizonTools in(Path folder) {
        String exe = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win") ? ".exe" : "";
        Path portPack = folder.resolve("horizon-port-pack" + exe), horizonMod = folder.resolve("cargo-horizon-mod" + exe);
        return Files.isRegularFile(portPack) && Files.isRegularFile(horizonMod) ? new HorizonTools(portPack.toAbsolutePath(), horizonMod.toAbsolutePath()) : null;
    }

    /** `horizon-port-pack <args>`. */
    Run portPack(List<String> args, int seconds) throws IOException {
        return run(portPack, args, null, seconds);
    }

    /** `cargo horizon-mod <args>`, in that folder. */
    Run horizonMod(List<String> args, Path folder, int seconds) throws IOException {
        List<String> all = new ArrayList<>(List.of("horizon-mod"));
        all.addAll(args);
        return run(horizonMod, all, folder, seconds);
    }

    private static Run run(Path program, List<String> args, Path folder, int seconds) throws IOException {
        List<String> command = new ArrayList<>(List.of(program.toString()));
        command.addAll(args);
        ProcessBuilder builder = new ProcessBuilder(command);
        if (folder != null) builder.directory(folder.toFile());
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new IOException(program.getFileName() + " couldn't be started: " + e.getMessage());
        }
        try {
            process.getOutputStream().close();
            CompletableFuture<String> out = CompletableFuture.supplyAsync(() -> text(process.getInputStream()));
            CompletableFuture<String> err = CompletableFuture.supplyAsync(() -> text(process.getErrorStream()));
            if (!process.waitFor(seconds, TimeUnit.SECONDS)) throw new IOException(program.getFileName() + " didn't finish within " + seconds + " seconds");
            return new Run(process.exitValue(), out.join(), err.join());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while " + program.getFileName() + " was running");
        } catch (CompletionException e) {
            throw new IOException("couldn't read what " + program.getFileName() + " printed: " + e.getCause().getMessage());
        } finally {
            process.destroyForcibly();
        }
    }

    private static String text(InputStream in) {
        try (in) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
