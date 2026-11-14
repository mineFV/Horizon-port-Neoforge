package horizonneo;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * `dump`: the mod's real facts, read in NeoForge by Horizon's own oracle (`tools/neoforge_oracle.py` in
 * Horizon's source, MIT, run unchanged). It starts a NeoForge server headless with the mod and its exporter
 * and writes every block, item, recipe and tag of the mod's namespaces, and vanilla's blocks for templates,
 * in the format `horizon-port-pack carry` reads (`port --facts`).
 *
 * The oracle installs NeoForge itself under `<horizon>/.local/neoforge-oracle/<version>` by downloading it.
 * This never lets it: the server the maintainer installed is linked there first, for its version, so
 * nothing is downloaded. The oracle writes eula=true in its own run folder; this checks first that the
 * maintainer's server says it, and stops otherwise: Minecraft's EULA is theirs to accept, never this tool's.
 *
 * This runs the original mod's Java, in NeoForge: the one thing CLAUDE.md's rule 2 allows, at the
 * maintainer's request.
 */
final class Dump {
    private Dump() {}

    /** How long the oracle may take: it builds its exporter, starts the server and dumps. */
    static final int SECONDS = 15 * 60;
    static final String ORACLE = "tools/neoforge_oracle.py", HOME = ".local/neoforge-oracle";

    record Options(Path server, Path horizonSrc, Path out, String python) {}

    static int run(ModJar jar, NeoForgeFrontend frontend, Options o, PrintStream out, PrintStream err) throws IOException {
        List<String> ids = frontend.toml().mods().stream().map(ModsToml.Mod::modId).toList();
        Path server = o.server().toAbsolutePath().normalize(), horizon = o.horizonSrc().toAbsolutePath().normalize();
        out.println("Dumping " + String.join(", ", ids) + " (" + jar.path().getFileName() + ") with Horizon's NeoForge oracle, in the server " + server);

        Path args = argsFile(server);
        if (args == null) {
            out.println("  problem: " + server + " isn't a NeoForge server: there is no libraries/net/neoforged/neoforge/<version>/" + argsName()
                    + " (made by NeoForge's installer: java -jar neoforge-<version>-installer.jar --install-server <folder>)");
            return Main.FAILED;
        }
        String version = args.getParent().getFileName().toString();
        if (!eulaAccepted(server)) {
            out.println("  problem: " + server.resolve("eula.txt") + " doesn't say eula=true. Minecraft's EULA is yours to read and accept "
                    + "(https://aka.ms/MinecraftEULA): write eula=true in that file yourself; this tool never does.");
            return Main.FAILED;
        }
        out.println("  ok: NeoForge " + version + ", and its EULA is accepted");
        Path oracle = horizon.resolve(ORACLE);
        if (!Files.isRegularFile(oracle)) {
            out.println("  problem: " + horizon + " has no " + ORACLE + ": it is Horizon's NeoForge oracle, in Horizon's source");
            return Main.FAILED;
        }
        String linked = link(horizon.resolve(HOME).resolve(version), server);
        if (linked != null) {
            out.println("  problem: " + linked);
            return Main.FAILED;
        }
        out.println("  ok: the oracle uses this server for NeoForge " + version + " (" + horizon.resolve(HOME).resolve(version) + "), so it downloads nothing");

        Path facts = (o.out() == null ? Path.of("out", ids.get(0) + "-facts.json") : o.out()).toAbsolutePath().normalize();
        Files.createDirectories(facts.getParent());
        Files.deleteIfExists(facts);
        Path log = facts.resolveSibling(facts.getFileName().toString().replaceAll("\\.json$", "") + ".log");
        Path work = Files.createTempDirectory("horizon-dump-");
        try {
            Path jars = work.resolve("jars");
            Files.createDirectories(jars);
            Files.copy(jar.path(), jars.resolve(jar.path().getFileName()));
            List<String> command = new ArrayList<>(List.of(o.python() == null ? "python3" : o.python(), oracle.toString(), "export",
                    "--jars", jars.toString(), "--namespaces", String.join(",", ids), "--out", facts.toString()));
            ProcessBuilder builder = new ProcessBuilder(command).directory(horizon.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
            builder.environment().put("HORIZON_NEOFORGE", version);
            out.println("  note: the oracle is running (its output goes to " + log + ")");
            Process process;
            try {
                process = builder.start();
            } catch (IOException e) {
                out.println("  problem: " + command.get(0) + " couldn't be started (" + e.getMessage() + "): the oracle needs Python 3.11 or newer; --python names it");
                return Main.FAILED;
            }
            process.getOutputStream().close();
            boolean done;
            try {
                done = process.waitFor(SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                done = false;
            }
            if (!done) {
                process.destroyForcibly();
                out.println("  problem: the oracle didn't finish within " + SECONDS + " seconds; it was stopped. Its output: " + log);
                return Main.FAILED;
            }
            if (process.exitValue() != 0 || !Files.isRegularFile(facts)) {
                out.println("  problem: the oracle failed (exit " + process.exitValue() + "). The end of its output:");
                tail(log, 25).forEach(l -> out.println("    " + l));
                return Main.FAILED;
            }
            summary(facts, ids, out);
            out.println("Facts written to " + facts + "; `port --facts` reads them");
            out.println("The mod was loaded and run in NeoForge, in the oracle's run folder; nothing was compiled or run in Horizon.");
            return Main.DONE;
        } finally {
            Port.delete(work);
        }
    }

    // ---- the server folder ----

    private static String argsName() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).startsWith("windows") ? "win_args.txt" : "unix_args.txt";
    }

    /** The launch arguments NeoForge's installer wrote, for the newest NeoForge in that folder; null when there are none. */
    static Path argsFile(Path server) throws IOException {
        Path versions = server.resolve("libraries/net/neoforged/neoforge");
        if (!Files.isDirectory(versions)) return null;
        try (Stream<Path> s = Files.list(versions)) {
            return s.map(v -> v.resolve(argsName())).filter(Files::isRegularFile).max(java.util.Comparator.comparing(Dump::versionKey)).orElse(null);
        }
    }

    /** 21.1.9 before 21.1.266: each part as a number. */
    private static String versionKey(Path args) {
        StringBuilder b = new StringBuilder();
        for (String part : args.getParent().getFileName().toString().split("[.-]")) {
            b.append(part.matches("\\d+") ? String.format("%08d", Long.parseLong(part)) : part).append('.');
        }
        return b.toString();
    }

    /** Whether the server folder's eula.txt says eula=true, as the server itself reads it. */
    static boolean eulaAccepted(Path server) throws IOException {
        Path eula = server.resolve("eula.txt");
        if (!Files.isRegularFile(eula)) return false;
        for (String line : Files.readAllLines(eula, StandardCharsets.UTF_8)) {
            if (line.strip().equals("eula=true")) return true;
        }
        return false;
    }

    /**
     * Makes the oracle's folder for that version show the given server: a link, made when there is nothing
     * there. One already there is used when it is this server, or another install of that version; null when
     * the oracle can use it, else why not.
     */
    static String link(Path home, Path server) throws IOException {
        if (Files.isSymbolicLink(home)) {
            Path to = Files.readSymbolicLink(home);
            if (!to.isAbsolute()) to = home.resolveSibling(to);
            if (Files.exists(to) && Files.isSameFile(to, server)) return null;
            return home + " already links to another folder (" + to + "): remove that link, or give that server";
        }
        if (Files.exists(home, LinkOption.NOFOLLOW_LINKS)) {
            return argsFile(home) != null ? null : home + " is there but holds no NeoForge server: remove it, and the oracle uses yours";
        }
        Files.createDirectories(home.getParent());
        try {
            Files.createSymbolicLink(home, server);
        } catch (UnsupportedOperationException | IOException e) {
            return "the link " + home + " -> " + server + " couldn't be made (" + e.getMessage() + ")";
        }
        return null;
    }

    // ---- what came out ----

    private static void summary(Path facts, List<String> ids, PrintStream out) throws IOException {
        Map<String, Object> f;
        try {
            f = Json.object(Json.parse(Files.readString(facts, StandardCharsets.UTF_8)));
        } catch (Json.Invalid e) {
            throw new IOException("the oracle wrote " + facts + ", which isn't JSON: " + e.getMessage());
        }
        for (String key : List.of("blocks", "items", "recipes")) {
            long mine = Json.object(f.get(key)).keySet().stream().filter(k -> ids.contains(k.substring(0, Math.max(0, k.indexOf(':'))))).count();
            out.println("  ok: " + mine + " " + key + " of the mod's");
        }
    }

    private static List<String> tail(Path file, int lines) throws IOException {
        if (!Files.isRegularFile(file)) return List.of("(no output)");
        List<String> all = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
        return all.subList(Math.max(0, all.size() - lines), all.size());
    }
}
