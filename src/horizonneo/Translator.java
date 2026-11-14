package horizonneo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * Horizon-Translator's converter, horizon-port.jar, run as a program of its own. It has no licence
 * file, so it is only ever called, as its user would call it: nothing of it is linked or copied.
 */
final class Translator {
    /** The environment variable naming horizon-port.jar. */
    static final String VARIABLE = "HORIZON_PORT_JAR";

    /** Where the jar is in a Horizon-Translator clone, once built there. */
    private static final String IN_CLONE = "Horizon-Translator/mod-converter/build/horizon-port.jar";

    /** One run: its exit code and what it printed. */
    record Run(int exit, String out, String err) {}

    private final Path jar;

    private Translator(Path jar) {
        this.jar = jar;
    }

    Path jar() {
        return jar;
    }

    /**
     * horizon-port.jar: the one given (`--translator`), else the one HORIZON_PORT_JAR names, else the one
     * in a Horizon-Translator folder beside this tool or the current folder (or above them).
     */
    static Translator locate(String given) throws IOException {
        String named = given != null ? given : System.getenv(VARIABLE);
        if (named != null && !named.isBlank()) {
            Path jar = Path.of(named);
            if (!Files.isRegularFile(jar)) {
                throw new IOException("horizon-port.jar: " + jar + " isn't a file (from " + (given != null ? "--translator" : VARIABLE) + ")");
            }
            return new Translator(jar.toAbsolutePath());
        }
        List<Path> starts = new ArrayList<>();
        try {
            starts.add(Path.of(Translator.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath());
        } catch (URISyntaxException | RuntimeException e) {
            // No usable place for this code: the current folder is still tried.
        }
        starts.add(Path.of("").toAbsolutePath());
        for (Path start : starts) {
            for (Path dir = start; dir != null; dir = dir.getParent()) {
                Path jar = dir.resolve(IN_CLONE);
                if (Files.isRegularFile(jar)) return new Translator(jar);
            }
        }
        throw new IOException("can't find horizon-port.jar. Build it in Horizon-Translator's clone (bash mod-converter/build.sh), "
                + "then pass --translator <its build/horizon-port.jar>, or set " + VARIABLE + ", or keep that clone beside this tool");
    }

    /** Runs `java -jar horizon-port.jar <args>` with this JVM's own java, and waits for it at most `seconds`. */
    Run run(List<String> args, int seconds) throws IOException {
        return run(List.of(), args, seconds, null);
    }

    /**
     * The same, with options for its JVM (`-Xss...`, `-Dname=value`) and, when `folder` isn't null, run
     * in that folder: what it writes under a relative path lands there.
     */
    Run run(List<String> jvmOptions, List<String> args, int seconds, Path folder) throws IOException {
        return run(jvmOptions, args, seconds, folder, false);
    }

    /**
     * The same; with `together`, what it and the programs it starts print as errors comes in `out`,
     * in the order it was printed, and `err` is empty.
     */
    Run run(List<String> jvmOptions, List<String> args, int seconds, Path folder, boolean together) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(ProcessHandle.current().info().command().orElse(Path.of(System.getProperty("java.home"), "bin", "java").toString()));
        // Its output is read as UTF-8 here, whatever the terminal's own encoding.
        command.add("-Dstdout.encoding=UTF-8");
        command.add("-Dstderr.encoding=UTF-8");
        command.addAll(jvmOptions);
        command.add("-jar");
        command.add(jar.toString());
        command.addAll(args);
        ProcessBuilder builder = new ProcessBuilder(command);
        if (folder != null) builder.directory(folder.toFile());
        if (together) builder.redirectErrorStream(true);
        Process process = builder.start();
        try {
            process.getOutputStream().close();
            CompletableFuture<String> out = CompletableFuture.supplyAsync(() -> text(process.getInputStream()));
            CompletableFuture<String> err = CompletableFuture.supplyAsync(() -> text(process.getErrorStream()));
            if (!process.waitFor(seconds, TimeUnit.SECONDS)) {
                throw new IOException("horizon-port didn't finish within " + seconds + " seconds (" + String.join(" ", args) + ")");
            }
            return new Run(process.exitValue(), out.join(), err.join());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while horizon-port was running");
        } catch (CompletionException e) {
            throw new IOException("couldn't read what horizon-port printed: " + e.getCause().getMessage());
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
