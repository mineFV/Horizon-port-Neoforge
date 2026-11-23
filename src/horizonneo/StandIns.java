package horizonneo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * `standins`: the translator's stand-ins for the game (game/) against the game itself, in two ways. Their signatures
 * against the real classes of a NeoForge server's jars (`Library.checkStandIns`), and their behaviour: one program
 * (probe/, `GameProbe`) is compiled against the server's jars and against the stand-ins, each is run in a JVM of its
 * own, and what they print must be the same. That runs the game's classes as a library (no server, no world, no mod),
 * to observe what they answer; their code is never read.
 */
final class StandIns {
    private StandIns() {}

    /** The probe's files in this tool's jar; GameProbe's main runs the others. */
    static final List<String> PROBES = List.of("probe/horizonneo/probe/GameProbe.java", "probe/horizonneo/probe/NbtProbe.java",
            "probe/horizonneo/probe/IdProbe.java", "probe/horizonneo/probe/MthProbe.java",
            "probe/horizonneo/probe/VecProbe.java",
            "probe/horizonneo/probe/EquipProbe.java",
            "probe/horizonneo/probe/RegistryProbe.java", "probe/horizonneo/probe/ComponentProbe.java", "probe/horizonneo/probe/CapabilityProbe.java", "probe/horizonneo/probe/TextProbe.java", "probe/horizonneo/probe/TagProbe.java", "probe/horizonneo/probe/LogProbe.java", "probe/horizonneo/probe/CodecProbe.java", "probe/horizonneo/probe/BuiltInProbe.java", "probe/horizonneo/probe/ConfigProbe.java", "probe/horizonneo/probe/ConfigFiles.java", "probe/horizonneo/probe/ColorProbe.java", "probe/horizonneo/probe/ArithProbe.java", "probe/horizonneo/probe/NameProbe.java", "probe/horizonneo/probe/FastutilProbe.java", "probe/horizonneo/probe/GuavaProbe.java", "probe/horizonneo/probe/BoxProbe.java", "probe/horizonneo/probe/RandomProbe.java", "probe/horizonneo/probe/ShapeProbe.java", "probe/horizonneo/probe/ChunkProbe.java", "probe/horizonneo/probe/FluidTypeProbe.java", "probe/horizonneo/probe/EntityTypeProbe.java", "probe/horizonneo/probe/FuelProbe.java", "probe/horizonneo/probe/BlockItemProbe.java", "probe/horizonneo/probe/IntrusiveProbe.java", "probe/horizonneo/probe/AttributeProbe.java", "probe/horizonneo/probe/ArmorProbe.java",
            "probe/horizonneo/probe/ModProbe.java",
            "probe/horizonneo/probe/ItemProbe.java", "probe/horizonneo/probe/AbsentProbe.java", "probe/horizonneo/probe/RegisterProbe.java", "probe/horizonneo/probe/ListProbe.java",
            "probe/horizonneo/probe/IntListProbe.java");

    /** A class of the probe deleted once it is compiled (AbsentProbe): what naming a class that isn't there does. */
    static final String ABSENT = "horizonneo/probe/HorizonProbeAbsent.class";

    /** A run of the probe: what it printed, or why it couldn't run. */
    record Run(List<String> lines, String problem) {}

    static int run(Path server, PrintStream out, PrintStream err) throws IOException {
        return run(server, null, null, out, err);
    }

    /**
     * horizonSrc: Horizon's source, whose NeoForge oracle (`dump`'s) runs the probe as a mod in the server, the game's
     * registries filled: its answers are compared with the stand-ins' too. Null: the probe runs outside only.
     */
    static int run(Path server, Path horizonSrc, String python, PrintStream out, PrintStream err) throws IOException {
        List<Path> jars = Dump.classpath(server);
        Map<String, ClassData> model = Library.classes();
        Map<String, ClassData> outside = JavaWorld.outside(jars);
        Map<String, String> leftOut = Library.checkStandIns(model, outside);
        long standIns = model.keySet().stream().filter(Library::standIn).count();
        long missing = model.keySet().stream().filter(Library::standIn).filter(n -> !outside.containsKey(n)).count();
        int problems = 0;
        for (var e : leftOut.entrySet()) {
            out.println("problem: " + e.getKey().replace('/', '.') + ": " + e.getValue());
            problems++;
        }
        if (missing > 0) out.println("problem: " + missing + " of the stand-ins aren't classes of this server's");
        if (missing > 0) problems++;
        if (leftOut.isEmpty() && missing == 0) out.println("ok: the " + standIns + " stand-ins have the supertypes and the methods of the server's classes");
        Path work = Files.createTempDirectory("horizonneo-standins");
        try {
            Path modelDir = work.resolve("model");
            writeModel(modelDir);
            String cp = String.join(java.io.File.pathSeparator, jars.stream().map(p -> p.toAbsolutePath().toString()).toList());
            Run game = probe(work.resolve("game"), cp);
            Run ours = probe(work.resolve("ours"), modelDir.toString());
            if (game.problem() != null) {
                out.println("problem: the probe doesn't run against the server's jars: " + game.problem());
                return 1;
            }
            if (ours.problem() != null) {
                out.println("problem: the probe doesn't run against the stand-ins: " + ours.problem());
                return 1;
            }
            int differ = 0;
            int n = Math.max(game.lines().size(), ours.lines().size());
            for (int i = 0; i < n; i++) {
                String g = i < game.lines().size() ? game.lines().get(i) : "(nothing)";
                String o = i < ours.lines().size() ? ours.lines().get(i) : "(nothing)";
                if (g.equals(o)) continue;
                if (differ++ < 20) out.println("problem: the game gives `" + g + "`, the stand-ins `" + o + "`");
            }
            if (differ > 20) out.println("problem: and " + (differ - 20) + " more differences");
            if (differ == 0) out.println("ok: the game and the stand-ins give the same " + game.lines().size() + " answers to the probe");
            problems += differ;
            if (horizonSrc != null) {
                Run oursInServer = probe(work.resolve("ours-server"), modelDir.toString(), List.of("server"));
                if (oursInServer.problem() != null) {
                    out.println("problem: the probe's server questions don't run against the stand-ins: " + oursInServer.problem());
                    problems++;
                } else {
                    problems += inServer(server, horizonSrc, python, cp, work, oursInServer.lines(), out);
                }
            }
            out.println("Ran the game's classes from " + jars.size() + " jars in " + server + " as a library, to observe their answers: no server, no world, no mod.");
            return problems == 0 ? 0 : 1;
        } finally {
            delete(work);
        }
    }

    static final String SERVER_MOD = "probe/server/horizonneo/probe/InServer.java", SERVER_TOML = "probe/server/META-INF/neoforge.mods.toml";

    /** The probe mod's data (its tags, its fuel), at their paths under probe/server/. */
    static final List<String> SERVER_DATA = List.of("data/horizonprobe/tags/item/probed.json", "data/horizonprobe/tags/block/blocks.json",
            "data/neoforge/data_maps/item/furnace_fuels.json");

    /**
     * The probe as a mod (probe/server/), run in the server by `dump`'s path: its answers, from horizon-probe.txt where
     * the oracle's server runs, against the stand-ins'. How many differ (1 when it couldn't run).
     */
    private static int inServer(Path server, Path horizonSrc, String python, String cp, Path work, List<String> ours, PrintStream out) throws IOException {
        Path dir = work.resolve("in-server"), classes = dir.resolve("classes");
        Files.createDirectories(classes.resolve("META-INF"));
        List<String> sources = new ArrayList<>();
        for (String probe : new ArrayList<>(PROBES) {{ add(SERVER_MOD); }}) {
            Path src = dir.resolve("src").resolve(probe.substring(probe.lastIndexOf('/') + 1));
            Files.createDirectories(src.getParent());
            try (InputStream in = StandIns.class.getResourceAsStream("/" + probe)) {
                if (in == null) throw new IOException("this tool's jar doesn't carry " + probe);
                Files.copy(in, src);
            }
            sources.add(src.toString());
        }
        for (String data : SERVER_DATA) {
            try (InputStream in = StandIns.class.getResourceAsStream("/probe/server/" + data)) {
                if (in == null) throw new IOException("this tool's jar doesn't carry probe/server/" + data);
                Files.createDirectories(classes.resolve(data).getParent());
                Files.copy(in, classes.resolve(data));
            }
        }
        try (InputStream in = StandIns.class.getResourceAsStream("/" + SERVER_TOML)) {
            if (in == null) throw new IOException("this tool's jar doesn't carry " + SERVER_TOML);
            Files.copy(in, classes.resolve("META-INF/neoforge.mods.toml"));
        }
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream said = new ByteArrayOutputStream();
        List<String> args = new ArrayList<>(List.of("--release", "21", "-proc:none", "-nowarn", "-cp", cp, "-d", classes.toString()));
        args.addAll(sources);
        if (javac == null || javac.run(null, said, said, args.toArray(String[]::new)) != 0) {
            out.println("problem: the probe doesn't compile as a mod against the server's jars: " + firstLine(said.toString(StandardCharsets.UTF_8)));
            return 1;
        }
        Files.delete(classes.resolve(ABSENT));
        Path jar = dir.resolve("horizonprobe.jar");
        try (java.io.OutputStream file = Files.newOutputStream(jar); java.util.jar.JarOutputStream j = new java.util.jar.JarOutputStream(file);
             Stream<Path> s = Files.walk(classes)) {
            for (Path p : s.filter(Files::isRegularFile).sorted().toList()) {
                j.putNextEntry(new java.util.jar.JarEntry(classes.relativize(p).toString().replace('\\', '/')));
                Files.copy(p, j);
                j.closeEntry();
            }
        }
        Path answers = horizonSrc.toAbsolutePath().normalize().resolve(Dump.HOME).resolve("runs").resolve("export").resolve("horizon-probe.txt");
        Files.deleteIfExists(answers);
        // The probe's config files, written new by NeoForge each run (their text is compared).
        Path configs = answers.getParent().resolve("config");
        if (Files.isDirectory(configs)) {
            try (Stream<Path> s = Files.list(configs)) {
                for (Path p : s.filter(p -> p.getFileName().toString().startsWith("horizonprobe-")).toList()) Files.delete(p);
            }
        }
        ByteArrayOutputStream dumpSaid = new ByteArrayOutputStream();
        ModJar mod = ModJar.open(jar);
        int exit;
        try (PrintStream quiet = new PrintStream(dumpSaid, true, StandardCharsets.UTF_8)) {
            exit = Dump.run(mod, NeoForgeFrontend.read(mod), new Dump.Options(server, horizonSrc, dir.resolve("facts.json"), python), quiet, quiet);
        } catch (ModsToml.Invalid e) {
            throw new IOException("the probe's neoforge.mods.toml doesn't read: " + e.getMessage());
        }
        if (exit != 0 || !Files.isRegularFile(answers)) {
            out.println("problem: the probe didn't run in the server (dump's exit " + exit + "):");
            dumpSaid.toString(StandardCharsets.UTF_8).lines().skip(Math.max(0, dumpSaid.toString(StandardCharsets.UTF_8).lines().count() - 15)).forEach(l -> out.println("  " + l));
            return 1;
        }
        // Kept where the server ran (Horizon's .local, git-ignored) until the next run: the game's answers, to read.
        List<String> game = Files.readAllLines(answers, StandardCharsets.UTF_8);
        int differ = 0;
        for (int i = 0; i < Math.max(game.size(), ours.size()); i++) {
            String g = i < game.size() ? game.get(i) : "(nothing)";
            String o = i < ours.size() ? ours.get(i) : "(nothing)";
            if (g.equals(o)) continue;
            if (differ++ < 20) out.println("problem: in a server, the game gives `" + g + "`, the stand-ins `" + o + "`");
        }
        if (differ > 20) out.println("problem: and " + (differ - 20) + " more differences in the server");
        if (differ == 0) out.println("ok: in a NeoForge server, the game and the stand-ins give the same " + game.size() + " answers to the probe");
        return differ;
    }

    /** The model's classes, out of this tool's jar, as a folder a JVM runs them from. */
    private static void writeModel(Path dir) throws IOException {
        try (InputStream index = StandIns.class.getResourceAsStream("/model/index.txt")) {
            if (index == null) throw new IOException("this tool's jar carries no model (build it with build.sh)");
            for (String line : new String(index.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String name = line.strip();
                if (name.isEmpty() || !(Library.standIn(name) || Library.bridge(name))) continue;
                try (InputStream in = StandIns.class.getResourceAsStream("/model/" + name)) {
                    if (in == null) throw new IOException("the model's " + name + " is missing from this tool's jar");
                    Path to = dir.resolve(name);
                    Files.createDirectories(to.getParent());
                    Files.copy(in, to);
                }
            }
        }
    }

    /** Compiles the probe against cp into dir, runs it with cp, and returns what it printed. */
    static Run probe(Path dir, String cp) throws IOException {
        return probe(dir, cp, List.of());
    }

    /** As probe(dir, cp), the probe given these arguments ("server": what needs the game started too). */
    static Run probe(Path dir, String cp, List<String> probeArgs) throws IOException {
        Path classes = dir.resolve("classes");
        Files.createDirectories(classes);
        List<String> sources = new ArrayList<>();
        for (String probe : PROBES) {
            Path src = dir.resolve("src").resolve(probe.substring(probe.lastIndexOf('/') + 1));
            Files.createDirectories(src.getParent());
            try (InputStream in = StandIns.class.getResourceAsStream("/" + probe)) {
                if (in == null) return new Run(List.of(), "this tool's jar doesn't carry " + probe + " (build it with build.sh)");
                Files.copy(in, src);
            }
            sources.add(src.toString());
        }
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) return new Run(List.of(), "this Java has no compiler (a JDK is needed, not a JRE)");
        ByteArrayOutputStream said = new ByteArrayOutputStream();
        List<String> args = new ArrayList<>(List.of("--release", "21", "-proc:none", "-nowarn", "-cp", cp, "-d", classes.toString()));
        args.addAll(sources);
        if (javac.run(null, said, said, args.toArray(String[]::new)) != 0) {
            return new Run(List.of(), "it doesn't compile: " + firstLine(said.toString(StandardCharsets.UTF_8)));
        }
        Files.delete(classes.resolve(ABSENT));
        String jvm = ProcessHandle.current().info().command().orElse("java");
        Path printed = dir.resolve("out.txt"), errors = dir.resolve("err.txt");
        List<String> command = new ArrayList<>(List.of(jvm, "-cp", classes + java.io.File.pathSeparator + cp, "horizonneo.probe.GameProbe"));
        command.addAll(probeArgs);
        Process p = new ProcessBuilder(command)
                // In its own folder: the game's logger writes logs/ where it runs, which is deleted with it.
                .directory(dir.toFile()).redirectOutput(printed.toFile()).redirectError(errors.toFile()).start();
        try {
            if (!p.waitFor(300, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return new Run(List.of(), "it didn't end in 300 s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            p.destroyForcibly();
            return new Run(List.of(), "interrupted");
        }
        if (p.exitValue() != 0) return new Run(List.of(), "it ended with " + p.exitValue() + ": " + firstLine(Files.readString(errors, StandardCharsets.ISO_8859_1)));
        return new Run(Files.readAllLines(printed, StandardCharsets.UTF_8), null);
    }

    private static String firstLine(String text) {
        String t = text.strip();
        int nl = t.indexOf('\n');
        return nl < 0 ? t : t.substring(0, nl);
    }

    private static void delete(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        List<Path> all = new ArrayList<>();
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(all::add);
        }
        for (Path p : all) Files.delete(p);
    }
}
