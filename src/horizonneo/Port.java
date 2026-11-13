package horizonneo;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * `port`: a NeoForge mod's jar to a Horizon native-mod bundle.
 *
 * The Mixin matching and the Java to Rust translation are horizon-port's, run unchanged on a
 * stand-in copy of the jar (`StandIn`). This checks the licence first, by horizon-port's rules,
 * then runs it, makes the bundle right for a NeoForge mod (`Bundle`), writes this tool's own report
 * into it, and deletes everything else: the copy and the decompiled source.
 *
 * Nothing of the mod is compiled or run, here or by horizon-port's `port`.
 */
final class Port {
    private Port() {}

    /** horizonSrc: Horizon's source. revision, author, out: null when not given. tools: the folder of Horizon's content tools, null to look
     * in the source. approximate: also switch on listeners whose hook point comes with a note, and pack content whose declarations are guesses. */
    record Options(Path horizonSrc, String revision, String author, Path out, String translator, String tools, boolean approximate) {}

    /** How long horizon-port may take: it decompiles the jar and indexes Horizon's source. */
    private static final int SECONDS = 30 * 60;

    /**
     * horizon-port strips Rust comments with a regular expression that recurses once per character
     * of a string literal. Horizon has lines long enough to overflow Java's default stack on it.
     */
    static final String STACK = "-Xss512m";

    static int run(ModJar jar, NeoForgeFrontend frontend, BusFacts facts, Options o, PrintStream out, PrintStream err) throws IOException {
        ModInfo info = frontend.info();
        if (!Files.isDirectory(o.horizonSrc()) && !Files.isRegularFile(o.horizonSrc())) {
            throw new IOException("--horizon-src: " + o.horizonSrc() + " isn't Horizon's source folder");
        }
        Translator translator = Translator.locate(o.translator());
        HorizonTools tools = HorizonTools.locate(o.tools(), o.horizonSrc());
        LicenceCheck.Verdict verdict = LicenceCheck.run(jar, info, o.author(), translator);
        if (!verdict.allowed()) {
            out.print(verdict.report());
            out.println("\nNothing was decompiled and nothing was written.");
            return Main.REFUSED;
        }
        Path target = o.out() != null ? o.out() : Path.of("out", info.id().replaceAll("[^A-Za-z0-9_-]", "_") + "-horizon");
        if (Files.exists(target) && (!Files.isDirectory(target) || !empty(target))) {
            throw new IOException(target + " is already there: a bundle someone has worked on isn't written over. Remove it, or name another folder with --out");
        }
        Analysis analysis = Analyzer.analyze(jar, frontend, facts);
        Revision revision = revision(o);

        Path work = Files.createTempDirectory("horizon-port-neoforge-");
        try {
            // Under the mod's own file name: horizon-port writes it into the bundle's texts.
            Path copyDir = Files.createDirectory(work.resolve("jar"));
            Path copy = copyDir.resolve(jar.name());
            Path decompiled = work.resolve("src");
            Path bundle = work.resolve("bundle");
            StandIn.Written written = StandIn.write(jar, info, analysis, copy);

            List<String> args = new ArrayList<>(List.of("port", copy.toString(), "--horizon-src", o.horizonSrc().toAbsolutePath().toString(), "--out", bundle.toString()));
            if (revision.pass() != null) {
                args.add("--horizon-revision");
                args.add(revision.pass());
            }
            if (o.author() != null) {
                args.add("--i-am-the-author");
                args.add(o.author());
            }
            Translator.Run run = translator.run(List.of(STACK, "-Dhorizonport.srcdir=" + decompiled), args, SECONDS, work);
            if (run.exit() == Main.REFUSED) {
                out.print(LicenceCheck.named(run.out(), info));
                out.println("\nNothing was written.");
                return Main.REFUSED;
            }
            if (run.err().contains("StackOverflowError")) {
                throw new IOException("horizon-port ran out of stack (" + STACK + ") reading Horizon's source: a line of it is too long for its comment stripper");
            }
            if (run.exit() != 0 || !Files.isDirectory(bundle)) {
                throw new IOException("horizon-port's `port` failed (exit " + run.exit() + ")" + tail(run.err().isBlank() ? run.out() : run.err()));
            }

            EventTable table = table(o.horizonSrc(), translator, work);
            Path carrying = Files.createDirectory(work.resolve("content"));
            Bundle.Outcome outcome = Bundle.finish(bundle, decompiled, o.horizonSrc(), info, analysis, table, o.approximate(),
                    into -> Content.carry(jar, analysis, tools, carrying, into, o.approximate()));
            Report.write(bundle.resolve("report"), jar, frontend, analysis, outcome);
            deliver(bundle, target);

            out.println(info.name() + " " + info.version() + ", a NeoForge mod: bundle written to " + target);
            out.println("  Licence: porting is allowed, by horizon-port's rules (PORTING.md has the conditions).");
            if (revision.words() != null) out.println("  Horizon: " + revision.words());
            out.println("  Injections: " + outcome.on().size() + " on" + ids(outcome.on()) + ", " + outcome.off().size() + " written commented out" + ids(outcome.off()));
            long on = outcome.handlers().values().stream().filter(h -> h.state().equals("on")).count();
            out.println("  Mixin handlers: " + (outcome.handlers().isEmpty() ? "none" : outcome.handlers().size() + ", " + on + " of them on"));
            Bundle.Translation t = outcome.translation();
            out.println("  Java translated to Rust: " + (t == null ? "horizon-port gave no count"
                    : t.methods() + " of " + t.methodsOf() + " methods" + (t.fields() == null ? "" : ", " + t.fields() + " of " + t.fieldsOf() + " static fields")
                            + " (horizon-port's count)"));
            long listeners = analysis.listeners().stream().filter(Analysis.Listener::registered).count();
            long listenersOff = outcome.listeners().stream().filter(w -> w.state().equals("off")).count();
            out.println("  Event listeners: " + (listeners == 0 ? "none" : table == null ? listeners + ", none on a hook point (this Horizon source has no native-hooks.json)"
                    : listeners + ", " + outcome.listenersOn() + " on a hook point, " + listenersOff + " written commented out"
                            + (listeners - outcome.listenersOn() - listenersOff > 0 ? ", " + (listeners - outcome.listenersOn() - listenersOff) + " of events of the mod's own" : "")));
            Content.Result content = outcome.content();
            out.println("  Content: " + switch (content.state()) {
                case "packed" -> content.file() + " (" + content.files() + (content.files() == 1 ? " file" : " files") + "), " + content.why();
                case "todo" -> content.why() + " (" + content.folder() + "/)";
                case "none" -> "no content mod, " + content.why();
                default -> "no content mod: " + content.why();
            });
            for (String c : outcome.changes()) out.println("  Changed: " + c);
            for (String l : written.leftOut()) out.println("  Not handed to horizon-port: " + l + ".");
            out.println("Start with " + target.resolve("PORTING.md"));
        } finally {
            delete(work);
        }
        if (Files.exists(work)) {
            err.println("horizon-port-neoforge: " + work + " couldn't be deleted. It holds the mod's decompiled source: delete it yourself.");
            return Main.FAILED;
        }
        out.println("The decompiled source and the copy of the jar have been deleted. Nothing was compiled or run.");
        return Main.DONE;
    }

    // ---- Horizon's hook points ----

    /**
     * Which hook point stands for which NeoForge event in that Horizon source: its hook catalogue,
     * with each hook point's signature as horizon-port reads it (`hooks --json`). null when the
     * source has no catalogue.
     */
    static EventTable table(Path horizonSrc, Translator translator, Path work) throws IOException {
        if (!Files.isRegularFile(horizonSrc.resolve("native-hooks.json"))) return null;
        Path list = work.resolve("hooks.json");
        Translator.Run run = translator.run(List.of("hooks", "--horizon-src", horizonSrc.toAbsolutePath().toString(), "--json", "--out", list.toString()), 600);
        // Without its list no hook point has a signature, and the table says so for each listener.
        String hookList = run.exit() == 0 && Files.isRegularFile(list) ? Files.readString(list, StandardCharsets.UTF_8) : null;
        return EventTable.read(horizonSrc, hookList);
    }

    // ---- Horizon's commit ----

    /** pass: the commit to hand horizon-port, null when it finds it itself or there is none. words: for the user. */
    private record Revision(String pass, String words) {}

    /**
     * The commit of Horizon's source: the one given, else the source's `.horizon-revision` (which
     * horizon-port reads itself), else what git says of the folder.
     */
    private static Revision revision(Options o) throws IOException {
        if (o.revision() != null) {
            if (!o.revision().matches("[0-9a-f]{40}")) throw new IOException("--horizon-revision takes a full commit: 40 characters, 0-9 and a-f");
            return new Revision(o.revision(), "commit " + o.revision() + " (given)");
        }
        if (!Files.isDirectory(o.horizonSrc())) return new Revision(null, null);
        Path file = o.horizonSrc().resolve(".horizon-revision");
        if (Files.isRegularFile(file)) {
            String said = Files.readString(file, StandardCharsets.UTF_8).strip();
            if (said.matches("[0-9a-f]{40}")) return new Revision(null, "commit " + said + " (its .horizon-revision)");
        }
        if (Files.exists(o.horizonSrc().resolve(".git"))) {
            String head = git(o.horizonSrc(), "rev-parse", "HEAD");
            if (head != null && head.strip().matches("[0-9a-f]{40}")) {
                String changed = git(o.horizonSrc(), "status", "--porcelain", "--untracked-files=no");
                return new Revision(head.strip(), "commit " + head.strip() + " (git's HEAD of that folder"
                        + (changed != null && !changed.isBlank() ? "; it has changes that aren't committed, which that commit doesn't have" : "") + ")");
            }
        }
        return new Revision(null, "its commit isn't known (no .horizon-revision, and --horizon-revision wasn't given): the bundle doesn't name one");
    }

    /** What a git command prints for that folder; null when git isn't there or fails. It changes nothing. */
    private static String git(Path folder, String... args) {
        List<String> command = new ArrayList<>(List.of("git", "-C", folder.toAbsolutePath().toString()));
        command.addAll(List.of(args));
        try {
            Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
            p.getOutputStream().close();
            String said = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!p.waitFor(60, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return null;
            }
            return p.exitValue() == 0 ? said : null;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    // ---- files ----

    /** The finished bundle to where it was asked for; copied when it can't simply be moved there. */
    private static void deliver(Path bundle, Path target) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.deleteIfExists(target);
        try {
            Files.move(bundle, target);
            return;
        } catch (IOException acrossDisks) {
            // The temporary folder is often on another disk: copy instead.
        }
        for (Path from : walk(bundle)) {
            Path to = target.resolve(bundle.relativize(from).toString());
            if (Files.isDirectory(from)) Files.createDirectories(to);
            else Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean empty(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.findAny().isEmpty();
        }
    }

    private static List<Path> walk(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.sorted().toList();
        }
    }

    /** Deletes a folder and all in it. What can't be deleted is left; the caller looks. */
    static void delete(Path dir) {
        if (!Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    // Kept going: as much as can be deleted is.
                }
            }
        } catch (IOException e) {
            // The caller sees that the folder is still there.
        }
    }

    private static String ids(List<String> ids) {
        return ids.isEmpty() ? "" : " (" + String.join(", ", ids) + ")";
    }

    /** The last lines of what a program printed, for an error message. */
    private static String tail(String text) {
        String[] lines = text.strip().split("\n");
        if (lines.length == 1 && lines[0].isEmpty()) return "";
        StringBuilder b = new StringBuilder(":");
        for (int i = Math.max(0, lines.length - 8); i < lines.length; i++) b.append("\n  ").append(lines[i]);
        return b.toString();
    }
}
