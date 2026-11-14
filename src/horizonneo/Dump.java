package horizonneo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * `dump`: the mod's real facts, read in a NeoForge server. The dumper (dumper/ in this repository, carried in
 * this tool's jar as source) is compiled against that server's own jars, put beside the mod in a server folder
 * of its own, and the server is started without a window; once it has started, the dumper writes what the
 * game registered for the mod's namespaces and stops it.
 *
 * This runs the original mod's Java, in NeoForge: the one thing CLAUDE.md's rule 2 allows, at the
 * maintainer's request. Minecraft's EULA is the maintainer's to accept, in the server folder they give: this
 * never writes `eula=true`. The folder the server runs in is a temporary one, deleted afterwards, so the
 * given server folder isn't changed.
 */
final class Dump {
    private Dump() {}

    /** How long a server may take to start and dump. */
    static final int SECONDS = 600;
    static final String DUMPER = "horizon_port_dumper";

    record Options(Path server, Path out, String java) {}

    static int run(ModJar jar, NeoForgeFrontend frontend, Options o, PrintStream out, PrintStream err) throws IOException {
        List<String> ids = frontend.toml().mods().stream().map(ModsToml.Mod::modId).toList();
        Path server = o.server().toAbsolutePath().normalize();
        out.println("Dumping " + String.join(", ", ids) + " (" + jar.path().getFileName() + ") in the NeoForge server " + server);

        Path args = argsFile(server);
        if (args == null) {
            out.println("  problem: " + server + " isn't a NeoForge server: there is no libraries/net/neoforged/neoforge/<version>/" + argsName()
                    + " (made by NeoForge's installer: java -jar neoforge-<version>-installer.jar --install-server <folder>)");
            return Main.FAILED;
        }
        if (!eulaAccepted(server)) {
            out.println("  problem: " + server.resolve("eula.txt") + " doesn't say eula=true. Minecraft's EULA is yours to read and accept "
                    + "(https://aka.ms/MinecraftEULA): write eula=true in that file yourself; this tool never does.");
            return Main.FAILED;
        }
        out.println("  ok: NeoForge " + args.getParent().getFileName() + ", and its EULA is accepted");

        Path facts = (o.out() == null ? Path.of("out", ids.get(0) + "-facts.json") : o.out()).toAbsolutePath().normalize();
        Files.createDirectories(facts.getParent());
        Files.deleteIfExists(facts);
        Path log = facts.resolveSibling(facts.getFileName().toString().replaceAll("\\.json$", "") + ".log");

        Path work = Files.createTempDirectory("horizon-dump-");
        try {
            Path dumper = work.resolve("dumper.jar");
            String compiled = compileDumper(server, work.resolve("dumper"), dumper);
            if (compiled != null) {
                out.println("  problem: the dumper doesn't compile against this server's jars:");
                compiled.lines().limit(20).forEach(l -> out.println("    " + l));
                return Main.FAILED;
            }
            out.println("  ok: the dumper compiles against this server's jars");

            Path run = work.resolve("server");
            Files.createDirectories(run.resolve("mods"));
            Files.copy(jar.path(), run.resolve("mods").resolve(jar.path().getFileName()));
            Files.copy(dumper, run.resolve("mods/horizon-port-dumper.jar"));
            Files.copy(server.resolve("eula.txt"), run.resolve("eula.txt"));
            Files.writeString(run.resolve("server.properties"), properties(freePort()), StandardCharsets.UTF_8);
            Files.writeString(run.resolve("args.txt"), absolute(Files.readString(args, StandardCharsets.UTF_8), server), StandardCharsets.UTF_8);

            List<String> command = new ArrayList<>(List.of(o.java() == null ? java() : o.java(),
                    "-Dhorizon.dump.namespaces=" + String.join(",", ids), "-Dhorizon.dump.out=" + facts, "@" + run.resolve("args.txt"), "nogui"));
            ProcessBuilder builder = new ProcessBuilder(command).directory(run.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
            out.println("  note: the server is starting (its output goes to " + log + "); it can take a few minutes");
            Process process = builder.start();
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
                out.println("  problem: the server didn't stop within " + SECONDS + " seconds; it was stopped. Its output: " + log);
                return Main.FAILED;
            }
            if (!Files.isRegularFile(facts)) {
                out.println("  problem: the server stopped (exit " + process.exitValue() + ") without the dumper writing anything. The end of its output:");
                tail(log, 25).forEach(l -> out.println("    " + l));
                return Main.FAILED;
            }
            summary(facts, out);
            out.println("Facts written to " + facts);
            out.println("The mod was loaded and run in NeoForge, in a folder that has been deleted; nothing was compiled or run in Horizon.");
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

    private static final Pattern LIBRARIES = Pattern.compile("(^|[\\s:;=,])libraries(?=[/\\\\\\s]|$)", Pattern.MULTILINE);

    /** The installer's arguments, with its paths into libraries/ made absolute, so the server can run in another folder. */
    static String absolute(String args, Path server) {
        String folder = server.resolve("libraries").toString().replace('\\', '/');
        return LIBRARIES.matcher(args).replaceAll(m -> m.group(1) + Matcher.quoteReplacement(folder));
    }

    /** A server that listens on this machine alone, in a flat world with nothing to generate that a dump doesn't need. */
    static String properties(int port) {
        return String.join("\n",
                "# Written by horizon-port-neoforge's dump: a server for reading registries, deleted afterwards.",
                "server-ip=127.0.0.1",
                "server-port=" + port,
                "online-mode=false",
                "level-name=dump",
                "level-type=minecraft\\:flat",
                "generate-structures=false",
                "spawn-protection=0",
                "max-players=1",
                "enable-query=false",
                "enable-rcon=false",
                "");
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static String java() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    // ---- the dumper ----

    /** Where the dumper's files are in this tool's jar, and where they go in the dumper's own. */
    static final Map<String, String> DUMPER_FILES = Map.of(
            "dumper/horizonneo/dumper/Dumper.java", "horizonneo/dumper/Dumper.java",
            "dumper/META-INF/neoforge.mods.toml", "META-INF/neoforge.mods.toml");

    /** The jars the dumper compiles against: NeoForge's patched classes first, then the game's, then the rest. */
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
        Path game = libraries.resolve("net/minecraft/server");
        all.stream().filter(p -> !out.contains(p) && !p.startsWith(game)).forEach(out::add);
        return out;
    }

    /** Compiles the dumper into a jar; null when it compiled, else what the compiler said. */
    private static String compileDumper(Path server, Path dir, Path jarFile) throws IOException {
        Path src = dir.resolve("src"), classes = dir.resolve("classes");
        Files.createDirectories(classes);
        for (Map.Entry<String, String> f : DUMPER_FILES.entrySet()) {
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
                src.resolve("horizonneo/dumper/Dumper.java").toString());
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

    // ---- what came out ----

    private static void summary(Path facts, PrintStream out) throws IOException {
        Map<String, Object> f;
        try {
            f = Json.object(Json.parse(Files.readString(facts, StandardCharsets.UTF_8)));
        } catch (Json.Invalid e) {
            throw new IOException("the dumper wrote " + facts + ", which isn't JSON: " + e.getMessage());
        }
        for (String key : List.of("blocks", "items", "blockEntityTypes", "menus", "fluids", "creativeTabs")) {
            List<Object> things = Json.array(f.get(key));
            int unread = 0;
            for (Object t : things) {
                Object u = Json.object(t).get("unread");
                if (u instanceof Map<?, ?> m && !m.isEmpty()) unread++;
            }
            out.println("  ok: " + things.size() + " " + key + (unread == 0 ? "" : " (" + unread + " with values not read: their `unread` says why)"));
        }
    }

    private static List<String> tail(Path file, int lines) throws IOException {
        if (!Files.isRegularFile(file)) return List.of("(no output)");
        List<String> all = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
        return all.subList(Math.max(0, all.size() - lines), all.size());
    }
}
