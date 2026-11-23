package horizonneo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

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
 * What the oracle's facts don't hold of the mod's machines (block entity types, ticking, item, energy and
 * fluid capabilities, menus) comes from this tool's companion exporter (exporter/ in this repository, carried
 * in its jar as source): it is compiled against the server's own jars and handed to the oracle with `--deps`,
 * so the oracle loads it beside the mod, unchanged. It writes `<facts without .json>.machines.json`.
 *
 * This runs the original mod's Java, in NeoForge: the one thing CLAUDE.md's rule 2 allows, at the
 * maintainer's request.
 */
final class Dump {
    private Dump() {}

    /** How long the oracle may take: it builds its exporter, starts the server and dumps. */
    static final int SECONDS = 15 * 60;
    static final String ORACLE = "tools/neoforge_oracle.py", HOME = ".local/neoforge-oracle";
    /** The JVM's option to write lambdas' classes, and the folder it writes them to, where the JVM runs. */
    static final String DUMP_LAMBDAS = "jdk.invoke.LambdaMetafactory.dumpProxyClassFiles", LAMBDA_FOLDER = "DUMP_LAMBDA_PROXY_CLASS_FILES";

    /** Deletes a folder and what it holds, when it is there. */
    static void deleteTree(Path dir) throws IOException {
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) return;
        List<Path> all;
        try (Stream<Path> s = Files.walk(dir)) {
            all = s.sorted(java.util.Comparator.reverseOrder()).toList();
        }
        for (Path p : all) Files.delete(p);
    }

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
        Path machines = machinesFile(facts);
        Files.deleteIfExists(machines);
        Path log = facts.resolveSibling(facts.getFileName().toString().replaceAll("\\.json$", "") + ".log");
        Path work = Files.createTempDirectory("horizon-dump-");
        try {
            Path jars = work.resolve("jars");
            Files.createDirectories(jars);
            Files.copy(jar.path(), jars.resolve(jar.path().getFileName()));
            Path deps = Files.createDirectories(work.resolve("deps"));
            String compiled = compileExporter(server, work.resolve("exporter"), deps.resolve("horizon-port-machines.jar"));
            if (compiled != null) {
                // The oracle's facts don't need it: the dump goes on, and says what it won't have.
                out.println("  problem: the machines exporter doesn't compile against this server's jars, so the mod's block entities won't be read:");
                compiled.lines().limit(20).forEach(l -> out.println("    " + l));
            } else {
                out.println("  ok: the machines exporter compiles against this server's jars");
            }
            List<String> command = new ArrayList<>(List.of(o.python() == null ? "python3" : o.python(), oracle.toString(), "export",
                    "--jars", jars.toString()));
            if (compiled == null) command.addAll(List.of("--deps", deps.toString()));
            command.addAll(List.of("--namespaces", String.join(",", ids), "--out", facts.toString()));
            ProcessBuilder builder = new ProcessBuilder(command).directory(horizon.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
            builder.environment().put("HORIZON_NEOFORGE", version);
            // The JVM writes each lambda's class to DUMP_LAMBDA_PROXY_CLASS_FILES/ where the server runs: the
            // exporter reads its block entities' tickers there, whose code names the mod's method each one runs
            // (Tickers). The JVM reads JAVA_TOOL_OPTIONS itself: the oracle, run unchanged, needn't know.
            String before = builder.environment().get("JAVA_TOOL_OPTIONS");
            builder.environment().put("JAVA_TOOL_OPTIONS", (before == null || before.isBlank() ? "" : before + " ") + "-D" + DUMP_LAMBDAS + "=true");
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
            // What the JVM wrote of lambda classes isn't needed once the exporter has read it.
            deleteTree(horizon.resolve(HOME).resolve("runs").resolve("export").resolve(LAMBDA_FOLDER));
            if (process.exitValue() != 0 || !Files.isRegularFile(facts)) {
                out.println("  problem: the oracle failed (exit " + process.exitValue() + "). The end of its output:");
                tail(log, 25).forEach(l -> out.println("    " + l));
                return Main.FAILED;
            }
            summary(facts, ids, out);
            if (compiled == null) machines(machines, out);
            out.println("Facts written to " + facts + (Files.isRegularFile(machines) ? " and " + machines : "") + "; `port --facts` reads them");
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

    /** The machines file beside a facts file, as the exporter names it. */
    static Path machinesFile(Path facts) {
        return facts.resolveSibling(facts.getFileName().toString().replaceAll("\\.json$", "") + ".machines.json");
    }

    private static void machines(Path machines, PrintStream out) throws IOException {
        if (!Files.isRegularFile(machines)) {
            out.println("  problem: the machines exporter wrote nothing (" + machines + "): the mod's block entities aren't read. The oracle's output says why");
            return;
        }
        Map<String, Object> m;
        try {
            m = Json.object(Json.parse(Files.readString(machines, StandardCharsets.UTF_8)));
        } catch (Json.Invalid e) {
            throw new IOException("the machines exporter wrote " + machines + ", which isn't JSON: " + e.getMessage());
        }
        if (Json.text(m, "failed") != null) {
            out.println("  problem: the machines exporter failed: " + Json.text(m, "failed"));
            return;
        }
        Map<String, Object> blocks = Json.object(m.get("blocks"));
        long unread = blocks.values().stream().filter(b -> !Json.object(Json.object(b).get("unread")).isEmpty()).count();
        long menus = blocks.values().stream().filter(b -> Json.object(b).containsKey("menu")).count();
        out.println("  ok: " + Json.object(m.get("block_entity_types")).size() + " block entity types of the mod's; " + blocks.size() + " blocks with a block entity, "
                + menus + " of them with a menu" + (unread == 0 ? "" : " (" + unread + " with values not read: their `unread` says why)"));
    }

    // ---- the machines exporter ----

    /** Where the exporter's files are in this tool's jar, and where they go in the exporter's own. */
    static final Map<String, String> EXPORTER_FILES = Map.of(
            "exporter/horizonneo/machines/MachinesExporter.java", "horizonneo/machines/MachinesExporter.java",
            "exporter/META-INF/neoforge.mods.toml", "META-INF/neoforge.mods.toml");

    /**
     * The jars the exporter compiles against: NeoForge's patched classes first, then the game's, then the rest: the
     * libraries the server runs with, NeoForge's own jars among them.
     */
    static List<Path> classpath(Path server) throws IOException {
        Path libraries = server.resolve("libraries");
        List<Path> all;
        try (Stream<Path> s = Files.walk(libraries)) {
            all = s.filter(p -> p.toString().endsWith(".jar")).sorted().toList();
        }
        List<Path> out = new ArrayList<>();
        all.stream().filter(p -> p.getFileName().toString().matches("neoforge-.*-server\\.jar")).forEach(out::add);
        all.stream().filter(p -> p.getFileName().toString().matches("server-.*-srg\\.jar")).forEach(out::add);
        // The game's other jars there are the installer's inputs (obfuscated, or the vanilla bundler): not what runs.
        Path game = libraries.resolve("net/minecraft/server"), neoforge = libraries.resolve("net/neoforged/neoforge");
        // So are the installer's own libraries beside the server's (Guava 20 beside the game's 32, which sorts after it):
        // where the server's launch arguments are, only the jars they name, and NeoForge's, which FML finds itself.
        java.util.Set<Path> launched = launched(server, all);
        all.stream().filter(p -> !out.contains(p) && !p.startsWith(game) && (launched == null || launched.contains(p) || p.startsWith(neoforge))).forEach(out::add);
        return out;
    }

    /**
     * The jars a server's launch arguments name (NeoForge's installer writes them in
     * libraries/net/neoforged/neoforge/VERSION/unix_args.txt: the module path and the legacy class path), as found among
     * all; null when there are none to read.
     */
    private static java.util.Set<Path> launched(Path server, List<Path> all) throws IOException {
        Path args = null;
        for (Path p : all) {
            if (!p.getFileName().toString().matches("neoforge-.*-server\\.jar")) continue;
            Path candidate = p.resolveSibling("unix_args.txt");
            if (Files.isRegularFile(candidate)) args = candidate;
        }
        if (args == null) return null;
        java.util.Set<Path> named = new java.util.HashSet<>();
        for (String token : Files.readString(args, StandardCharsets.UTF_8).split("[\\s:=]+")) {
            if (token.startsWith("libraries/") && token.endsWith(".jar")) named.add(server.resolve(token).normalize());
        }
        java.util.Set<Path> out = new java.util.HashSet<>();
        for (Path p : all) if (named.contains(p.normalize())) out.add(p);
        return out;
    }

    /** Compiles the exporter into a jar; null when it compiled, else what the compiler said. */
    static String compileExporter(Path server, Path dir, Path jarFile) throws IOException {
        Path src = dir.resolve("src"), classes = dir.resolve("classes");
        Files.createDirectories(classes);
        for (Map.Entry<String, String> f : EXPORTER_FILES.entrySet()) {
            Path to = (f.getValue().endsWith(".java") ? src : classes).resolve(f.getValue());
            Files.createDirectories(to.getParent());
            try (InputStream in = Dump.class.getResourceAsStream("/" + f.getKey())) {
                if (in == null) return "this tool's jar doesn't carry " + f.getKey() + " (build it with build.sh)";
                Files.copy(in, to);
            }
        }
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) return "this Java has no compiler (a JDK is needed, not a JRE)";
        String cp = String.join(java.io.File.pathSeparator, classpath(server).stream().map(Path::toString).toList());
        ByteArrayOutputStream said = new ByteArrayOutputStream();
        int exit = javac.run(null, said, said, "--release", "21", "-proc:none", "-nowarn", "-cp", cp, "-d", classes.toString(),
                src.resolve("horizonneo/machines/MachinesExporter.java").toString());
        if (exit != 0) return said.toString(StandardCharsets.UTF_8);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (OutputStream file = Files.newOutputStream(jarFile); JarOutputStream jar = new JarOutputStream(file, manifest);
             Stream<Path> s = Files.walk(classes)) {
            for (Path p : s.filter(Files::isRegularFile).sorted().toList()) {
                jar.putNextEntry(new JarEntry(classes.relativize(p).toString().replace('\\', '/')));
                Files.copy(p, jar);
                jar.closeEntry();
            }
        }
        return null;
    }

    private static List<String> tail(Path file, int lines) throws IOException {
        if (!Files.isRegularFile(file)) return List.of("(no output)");
        List<String> all = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
        return all.subList(Math.max(0, all.size() - lines), all.size());
    }
}
