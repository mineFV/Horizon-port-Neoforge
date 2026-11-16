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
    static final List<String> PROBES = List.of("probe/horizonneo/probe/GameProbe.java", "probe/horizonneo/probe/NbtProbe.java");

    /** A run of the probe: what it printed, or why it couldn't run. */
    record Run(List<String> lines, String problem) {}

    static int run(Path server, PrintStream out, PrintStream err) throws IOException {
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
            out.println("Ran the game's classes from " + jars.size() + " jars in " + server + " as a library, to observe their answers: no server, no world, no mod.");
            return problems == 0 ? 0 : 1;
        } finally {
            delete(work);
        }
    }

    /** The model's classes, out of this tool's jar, as a folder a JVM runs them from. */
    private static void writeModel(Path dir) throws IOException {
        try (InputStream index = StandIns.class.getResourceAsStream("/model/index.txt")) {
            if (index == null) throw new IOException("this tool's jar carries no model (build it with build.sh)");
            for (String line : new String(index.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String name = line.strip();
                if (name.isEmpty() || !Library.standIn(name)) continue;
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
        String jvm = ProcessHandle.current().info().command().orElse("java");
        Path printed = dir.resolve("out.txt"), errors = dir.resolve("err.txt");
        Process p = new ProcessBuilder(jvm, "-cp", classes + java.io.File.pathSeparator + cp, "horizonneo.probe.GameProbe")
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
