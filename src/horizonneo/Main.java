package horizonneo;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** horizon-port-neoforge: turns a NeoForge mod into a Horizon native-mod bundle, through Horizon-Translator's pipeline. */
public final class Main {
    /** Exit codes. REFUSED is the code horizon-port gives when a licence doesn't allow porting. */
    static final int DONE = 0, FAILED = 1, USAGE = 2, REFUSED = 3;

    /** The loader whose metadata a command goes by. */
    enum Loader { NEOFORGE, FABRIC }

    /** The command line was wrong. */
    static final class Usage extends Exception {
        private static final long serialVersionUID = 1L;

        Usage(String message) {
            super(message);
        }
    }

    private Main() {}

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** Runs one command and returns its exit code. It never exits the JVM itself, so tests can call it. */
    static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            return command(args, out, err);
        } catch (Usage e) {
            err.println("horizon-port-neoforge: " + e.getMessage() + " (see --help)");
            return USAGE;
        } catch (IOException | ModsToml.Invalid e) {
            err.println("horizon-port-neoforge: " + e.getMessage());
            return FAILED;
        }
    }

    private static int command(String[] args, PrintStream out, PrintStream err) throws Usage, IOException, ModsToml.Invalid {
        if (args.length == 0) {
            err.print(HELP);
            return USAGE;
        }
        switch (args[0]) {
            case "-h", "--help", "help" -> {
                out.print(HELP);
                return DONE;
            }
            case "metadata" -> {
                return metadata(Line.read(args, Set.of("--loader"), Set.of("--json")), out);
            }
            case "license" -> {
                return license(Line.read(args, Set.of("--loader", "--i-am-the-author", "--translator"), Set.of()), out, err);
            }
            case "analyze" -> {
                return analyze(Line.read(args, Set.of("--loader", "--out", "--neoforge-jar", "--translator"), Set.of()), out, err);
            }
            case "port" -> {
                return port(Line.read(args, Set.of("--loader", "--out", "--neoforge-jar", "--translator", "--horizon-src", "--horizon-revision",
                        "--i-am-the-author", "--horizon-tools", "--facts"), Set.of("--allow-approximate")), out, err);
            }
            case "check" -> {
                return check(Line.read(args, Set.of("--horizon-src", "--translator", "--horizon-tools"), Set.of("--compile")), out, err);
            }
            case "translate" -> {
                return translate(Line.read(args, Set.of("--out", "--server", "--bridge"), Set.of("--blockers")), out, err);
            }
            case "standins" -> {
                Line line = Line.read(args, Set.of("--server", "--horizon-src", "--python"), Set.of());
                if (!line.plain().isEmpty()) throw new Usage("`standins` takes no file, only --server");
                if (!line.has("--server")) throw new Usage("`standins` needs --server <a NeoForge server folder, made by NeoForge's installer>");
                Path server = Path.of(line.options().get("--server"));
                if (!Files.isDirectory(server.resolve("libraries"))) throw new Usage(server + " isn't a NeoForge server: it has no libraries/ folder");
                return StandIns.run(server, line.has("--horizon-src") ? Path.of(line.options().get("--horizon-src")) : null, line.options().get("--python"), out, err);
            }
            case "dump" -> {
                return dump(Line.read(args, Set.of("--server", "--horizon-src", "--out", "--python"), Set.of()), out, err);
            }
            case "pack" -> {
                return pack(Line.read(args, Set.of("--horizon-src", "--translator", "--horizon-tools", "--out"), Set.of()), out, err);
            }
            default -> throw new Usage("unknown command `" + args[0] + "`");
        }
    }

    // ---- check ----

    private static int check(Line line, PrintStream out, PrintStream err) throws Usage, IOException {
        Path bundle = line.only("bundle folder");
        String horizonSrc = line.options().get("--horizon-src");
        if (horizonSrc == null) throw new Usage("`check` needs --horizon-src <Horizon's source folder>");
        return Check.run(bundle, new Check.Options(Path.of(horizonSrc), line.options().get("--translator"), line.options().get("--horizon-tools"),
                line.has("--compile")), out, err);
    }

    // ---- translate ----

    private static int translate(Line line, PrintStream out, PrintStream err) throws Usage, IOException, ModsToml.Invalid {
        Path path = line.only("mod jar");
        ModJar jar = ModJar.open(path);
        if (!jar.neoforge()) throw new Usage("`translate` takes a NeoForge mod: " + path.getFileName() + " has no " + ModsToml.PATH);
        String id = NeoForgeFrontend.read(jar).info().id();
        Path dir = line.options().containsKey("--out") ? Path.of(line.options().get("--out")) : Path.of("out", id.replaceAll("[^A-Za-z0-9_-]", "_") + "-translated");
        Map<String, ClassData> outside = Map.of();
        String read = "no --server: the supertypes of the game's classes aren't known";
        if (line.options().containsKey("--server")) {
            Path server = Path.of(line.options().get("--server"));
            if (!Files.isDirectory(server.resolve("libraries"))) throw new Usage(server + " isn't a NeoForge server: it has no libraries/ folder");
            List<Path> jars = Dump.classpath(server);
            outside = JavaWorld.outside(jars);
            read = "the supertypes of " + outside.size() + " classes of the game, NeoForge and their libraries, from " + jars.size() + " jars in " + server;
        }
        Map<String, ClassData> model = new java.util.TreeMap<>(Library.classes());
        Map<String, String> leftOut = Library.checkStandIns(model, outside);
        model.keySet().removeAll(leftOut.keySet());
        long standIns = model.keySet().stream().filter(Library::standIn).count();
        StringBuilder game = new StringBuilder(outside.isEmpty()
                ? "Stand-ins for the game: " + standIns + " classes, not checked against the game's (no --server)."
                : "Stand-ins for the game: " + standIns + " classes, checked against the game's.");
        for (var e : leftOut.entrySet()) game.append("\n- left out: ").append(e.getKey().replace('/', '.')).append(": ").append(e.getValue());
        JarRust.Result r = JarRust.translate(jar, model, outside);
        java.nio.file.Files.createDirectories(dir);
        java.nio.file.Files.writeString(dir.resolve("translated.rs"), r.rust(), java.nio.charset.StandardCharsets.UTF_8);
        java.nio.file.Files.writeString(dir.resolve("translation.md"), JarRust.report(r, -1).replaceFirst("\n\n", "\n\nRead: " + java.util.regex.Matcher.quoteReplacement(read) + ".\n\n" + java.util.regex.Matcher.quoteReplacement(game.toString()) + "\n\n"),
                java.nio.charset.StandardCharsets.UTF_8);
        out.println("Read: " + read + ".");
        out.println(game);
        out.println(jar.path().getFileName() + ": " + r.translated().size() + " of " + r.methods() + " methods translated from their bytecode, by this tool's own translator");
        int shown = 0;
        for (var e : r.reasons().entrySet()) {
            if (shown++ == 8) break;
            out.println("  not translated: " + e.getValue() + ", " + e.getKey());
        }
        if (line.has("--blockers")) {
            java.nio.file.Files.writeString(dir.resolve("blockers.md"), Blockers.report(JarRust.classes(jar), model, outside, r.translated(), r.why()), java.nio.charset.StandardCharsets.UTF_8);
            out.println("Written: " + dir.resolve("blockers.md") + ", what keeps the mod's methods out, piece by piece (an estimate).");
        }
        if (line.has("--bridge")) {
            List<String> missing = new ArrayList<>();
            Map<String, String> functions = BridgeRust.functions(r, missing);
            if (functions == null) {
                out.println("problem: no bridge: the translation lacks " + String.join(", ", missing));
            } else {
                java.nio.file.Files.writeString(dir.resolve("bridge.rs"), BridgeRust.module(line.options().get("--bridge"), NeoForgeFrontend.read(jar).info(), functions), java.nio.charset.StandardCharsets.UTF_8);
                out.println("Written: " + dir.resolve("bridge.rs") + ", Horizon's side of the bridge, which reaches the translation as " + line.options().get("--bridge") + ".");
            }
        }
        out.println("Written: " + dir.resolve("translated.rs") + " and " + dir.resolve("translation.md") + ". Nothing was compiled or run.");
        return DONE;
    }

    // ---- dump ----

    private static int dump(Line line, PrintStream out, PrintStream err) throws Usage, IOException, ModsToml.Invalid {
        Path path = line.only("mod jar");
        String server = line.options().get("--server"), file = line.options().get("--out"), horizonSrc = line.options().get("--horizon-src");
        if (server == null) throw new Usage("`dump` needs --server <a NeoForge server folder, made by NeoForge's installer>");
        if (horizonSrc == null) throw new Usage("`dump` needs --horizon-src <Horizon's source folder, which holds its NeoForge oracle>");
        ModJar jar = ModJar.open(path);
        if (!jar.neoforge()) throw new Usage("`dump` takes a NeoForge mod: " + path.getFileName() + " has no " + ModsToml.PATH);
        return Dump.run(jar, NeoForgeFrontend.read(jar), new Dump.Options(Path.of(server), Path.of(horizonSrc), file == null ? null : Path.of(file), line.options().get("--python")), out, err);
    }

    // ---- pack ----

    private static int pack(Line line, PrintStream out, PrintStream err) throws Usage, IOException {
        Path bundle = line.only("bundle folder");
        String horizonSrc = line.options().get("--horizon-src"), file = line.options().get("--out");
        if (horizonSrc == null) throw new Usage("`pack` needs --horizon-src <Horizon's source folder>");
        return Pack.run(bundle, new Pack.Options(Path.of(horizonSrc), line.options().get("--translator"), line.options().get("--horizon-tools"),
                file == null ? null : Path.of(file)), out, err);
    }

    // ---- port ----

    private static int port(Line line, PrintStream out, PrintStream err) throws Usage, IOException, ModsToml.Invalid {
        Path path = line.only("mod jar");
        String horizonSrc = line.options().get("--horizon-src");
        if (horizonSrc == null) throw new Usage("`port` needs --horizon-src <Horizon's source folder>");
        ModJar jar = ModJar.open(path);
        String outDir = line.options().get("--out"), revision = line.options().get("--horizon-revision"), author = line.options().get("--i-am-the-author");
        if (loader(jar, line.option("--loader", "auto")) == Loader.FABRIC) {
            if (line.has("--facts")) throw new Usage("--facts is for a NeoForge mod: a Fabric jar goes to horizon-port as it is");
            // horizon-port's own job: it gets the jar as it is, with the options it knows (and the stack Horizon's source takes).
            List<String> args = new ArrayList<>(List.of("port", path.toString(), "--horizon-src", horizonSrc));
            for (String option : List.of("--horizon-revision", "--i-am-the-author", "--out")) {
                if (line.has(option)) {
                    args.add(option);
                    args.add(line.options().get(option));
                }
            }
            Translator.Run run = Translator.locate(line.options().get("--translator")).run(List.of(Port.STACK), args, 1800, null);
            out.print(run.out());
            err.print(run.err());
            return run.exit();
        }
        NeoForgeFrontend frontend = NeoForgeFrontend.read(jar);
        if (line.has("--facts") && !java.nio.file.Files.isRegularFile(Path.of(line.options().get("--facts")))) {
            throw new Usage("--facts names " + line.options().get("--facts") + ", which isn't a file (`dump` writes it)");
        }
        Port.Options options = new Port.Options(Path.of(horizonSrc), revision, author, outDir == null ? null : Path.of(outDir), line.options().get("--translator"),
                line.options().get("--horizon-tools"), line.has("--allow-approximate"), line.has("--facts") ? Path.of(line.options().get("--facts")) : null);
        return Port.run(jar, frontend, facts(line), options, out, err);
    }

    /** What is known of each event's bus: NeoForge's documentation, or its own jars when they are given. */
    private static BusFacts facts(Line line) throws IOException {
        String neoforge = line.options().get("--neoforge-jar");
        if (neoforge == null) return BusFacts.documented();
        List<Path> jars = new ArrayList<>();
        for (String one : neoforge.split(java.io.File.pathSeparator)) if (!one.isBlank()) jars.add(Path.of(one));
        return BusFacts.fromJars(jars);
    }

    // ---- analyze ----

    private static int analyze(Line line, PrintStream out, PrintStream err) throws Usage, IOException, ModsToml.Invalid {
        Path path = line.only("mod jar");
        ModJar jar = ModJar.open(path);
        String outDir = line.options().get("--out");
        if (loader(jar, line.option("--loader", "auto")) == Loader.FABRIC) {
            // horizon-port's own job: it gets the jar as it is.
            List<String> args = new ArrayList<>(List.of("analyze", path.toString()));
            if (outDir != null) {
                args.add("--out");
                args.add(outDir);
            }
            Translator.Run run = Translator.locate(line.options().get("--translator")).run(args, 300);
            out.print(run.out());
            err.print(run.err());
            return run.exit();
        }
        NeoForgeFrontend frontend = NeoForgeFrontend.read(jar);
        Analysis analysis = Analyzer.analyze(jar, frontend, facts(line));
        // The mod's id comes from the jar: only its plain characters go into a folder's name.
        Path dir = outDir != null ? Path.of(outDir) : Path.of("out", frontend.info().id().replaceAll("[^A-Za-z0-9_-]", "_"));
        Report.write(dir, jar, frontend, analysis);
        out.println(Report.summary(frontend, analysis));
        out.println("Report: " + dir.resolve("report.md"));
        return DONE;
    }

    // ---- license ----

    private static int license(Line line, PrintStream out, PrintStream err) throws Usage, IOException, ModsToml.Invalid {
        Path path = line.only("mod jar");
        ModJar jar = ModJar.open(path);
        String author = line.options().get("--i-am-the-author");
        if (loader(jar, line.option("--loader", "auto")) == Loader.FABRIC) {
            // horizon-port's own job: it gets the jar as it is, and its answer is passed on as it is.
            List<String> args = new ArrayList<>(List.of("license", path.toString()));
            if (author != null) {
                args.add("--i-am-the-author");
                args.add(author);
            }
            Translator.Run run = Translator.locate(line.options().get("--translator")).run(args, 120);
            out.print(run.out());
            err.print(run.err());
            return run.exit();
        }
        ModInfo info = NeoForgeFrontend.read(jar).info();
        LicenceCheck.Verdict verdict = LicenceCheck.run(jar, info, author, Translator.locate(line.options().get("--translator")));
        out.print(verdict.report());
        return verdict.allowed() ? DONE : REFUSED;
    }

    // ---- metadata ----

    private static int metadata(Line line, PrintStream out) throws Usage, IOException, ModsToml.Invalid {
        ModJar jar = ModJar.open(line.only("mod jar"));
        if (loader(jar, line.option("--loader", "auto")) == Loader.FABRIC) {
            throw new IOException(jar.name() + " is a Fabric mod: horizon-port reads those itself (`horizon-port analyze`)");
        }
        NeoForgeFrontend frontend = NeoForgeFrontend.read(jar);
        out.print(line.has("--json") ? Json.write(describe(jar, frontend)) : text(jar, frontend));
        return DONE;
    }

    /** Which loader's metadata to go by: the one asked for, or the only one the jar has. */
    static Loader loader(ModJar jar, String asked) throws Usage, IOException {
        switch (asked) {
            case "neoforge" -> {
                if (jar.neoforge()) return Loader.NEOFORGE;
                throw new IOException(jar.name() + " has no " + ModsToml.PATH + ": " + whatItIs(jar));
            }
            case "fabric" -> {
                if (jar.fabric()) return Loader.FABRIC;
                throw new IOException(jar.name() + " has no " + ModJar.FABRIC + ": " + whatItIs(jar));
            }
            case "auto" -> {
                if (jar.neoforge() && jar.fabric()) {
                    throw new Usage(jar.name() + " is made for both NeoForge and Fabric: say which to go by with --loader neoforge or --loader fabric");
                }
                if (jar.neoforge()) return Loader.NEOFORGE;
                if (jar.fabric()) return Loader.FABRIC;
                throw new IOException(jar.name() + ": " + whatItIs(jar));
            }
            default -> throw new Usage("--loader takes auto, neoforge or fabric, not `" + asked + "`");
        }
    }

    private static String whatItIs(ModJar jar) {
        if (jar.neoforge()) return "it is a NeoForge mod";
        if (jar.fabric()) return "it is a Fabric mod";
        if (jar.legacyForge()) {
            return "its " + ModJar.LEGACY_FORGE + " is Forge's, or NeoForge's before 20.5, which this tool doesn't read";
        }
        return "it has neither " + ModsToml.PATH + " nor " + ModJar.FABRIC + ", so it isn't a NeoForge or Fabric mod";
    }

    private static String text(ModJar jar, NeoForgeFrontend frontend) {
        ModInfo info = frontend.info();
        ModsToml toml = frontend.toml();
        ModsToml.Mod mod = toml.mods().get(0);
        StringBuilder b = new StringBuilder();
        b.append(info.name()).append(' ').append(info.version()).append(" (`").append(info.id()).append("`), a NeoForge mod: ")
                .append(jar.name()).append('\n');
        row(b, "licence", toml.declaredLicense() == null ? "none declared" : toml.declaredLicense());
        row(b, "licence files", list(jar.licenceFiles()));
        row(b, "authors", mod.authors().isEmpty() ? "none listed" : mod.authors());
        row(b, "description", info.description().isEmpty() ? "none" : info.description().replaceAll("\\s+", " "));
        row(b, "Mixin configs", list(info.mixinConfigs()));
        row(b, "access transformers", list(toml.accessTransformers()));
        for (ModsToml.Dependency d : toml.dependencies()) {
            String side = d.side().equals("BOTH") ? "" : ", " + d.side().toLowerCase(Locale.ROOT) + " only";
            row(b, d.of().equals(info.id()) ? "depends on" : "`" + d.of() + "` depends on",
                    d.modId() + " (" + d.type() + ", " + d.versionRange() + side + ")");
        }
        if (jar.fabric()) row(b, "also", "a " + ModJar.FABRIC + ": the jar is made for Fabric too");
        for (String w : info.warnings()) row(b, "warning", w);
        return b.toString();
    }

    private static void row(StringBuilder b, String label, String value) {
        b.append("  ").append(label).append(": ").append(value).append('\n');
    }

    private static String list(List<String> items) {
        return items.isEmpty() ? "none" : String.join(", ", items);
    }

    /** The same, for `--json`. */
    private static Map<String, Object> describe(ModJar jar, NeoForgeFrontend frontend) {
        ModInfo info = frontend.info();
        ModsToml toml = frontend.toml();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jar", jar.name());
        m.put("loader", info.loader());
        m.put("metadata", info.metadataFile());
        m.put("id", info.id());
        m.put("name", info.name());
        m.put("version", info.version());
        m.put("description", info.description());
        m.put("license", toml.declaredLicense());
        m.put("licenseFiles", jar.licenceFiles());
        m.put("authors", toml.mods().get(0).authors());
        m.put("authorNames", info.authors());
        List<Object> mixins = new ArrayList<>();
        for (ModsToml.MixinConfig c : toml.mixins()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("config", c.config());
            o.put("requiredMods", c.requiredMods());
            mixins.add(o);
        }
        m.put("mixins", mixins);
        m.put("accessTransformers", toml.accessTransformers());
        List<Object> dependencies = new ArrayList<>();
        for (ModsToml.Dependency d : toml.dependencies()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("of", d.of());
            o.put("modId", d.modId());
            o.put("type", d.type());
            o.put("versionRange", d.versionRange());
            o.put("ordering", d.ordering());
            o.put("side", d.side());
            o.put("reason", d.reason());
            dependencies.add(o);
        }
        m.put("dependencies", dependencies);
        m.put("otherMods", toml.mods().stream().skip(1).map(ModsToml.Mod::modId).toList());
        m.put("alsoFabric", jar.fabric());
        m.put("warnings", info.warnings());
        return m;
    }

    // ---- the command line ----

    /** A command line: the command, what isn't an option, and the options by name ("" for one without a value). */
    private record Line(String command, List<String> plain, Map<String, String> options) {
        static Line read(String[] args, Set<String> valued, Set<String> flags) throws Usage {
            List<String> plain = new ArrayList<>();
            Map<String, String> options = new LinkedHashMap<>();
            for (int i = 1; i < args.length; i++) {
                String a = args[i];
                if (valued.contains(a)) {
                    if (i + 1 >= args.length) throw new Usage(a + " needs a value");
                    options.put(a, args[++i]);
                } else if (flags.contains(a)) {
                    options.put(a, "");
                } else if (a.startsWith("--")) {
                    throw new Usage("`" + args[0] + "` has no option " + a);
                } else {
                    plain.add(a);
                }
            }
            return new Line(args[0], plain, options);
        }

        boolean has(String option) {
            return options.containsKey(option);
        }

        String option(String name, String fallback) {
            return options.getOrDefault(name, fallback);
        }

        /** The one thing the command takes besides options. */
        Path only(String what) throws Usage {
            if (plain.size() != 1) throw new Usage("`" + command + "` takes one " + what);
            return Path.of(plain.get(0));
        }
    }

    static final String HELP = """
            horizon-port-neoforge: NeoForge mods to Horizon native-mod bundles

              metadata <mod.jar> [--json] [--loader neoforge]
                  What the jar's META-INF/neoforge.mods.toml says: the mod, its licence and
                  authors, its dependencies, its Mixin configs and access transformers.
                  Text, or JSON with --json. Nothing else in the jar is read.

              license <mod.jar> [--i-am-the-author NAME] [--loader auto|neoforge|fabric] [--translator JAR]
                  Whether the mod's licence lets you decompile and port it, by horizon-port's own
                  rules: open licences (MIT, Apache, GPL, LGPL, MPL...) do, with their conditions;
                  All Rights Reserved, no-derivatives, unknown and missing licences don't, unless
                  NAME is one of the mod's authors. It goes by `license` in
                  META-INF/neoforge.mods.toml and by the jar's LICENSE / COPYING files; the
                  strictest wins. Exit code 0 when it is allowed, 3 when it isn't.
                  A Fabric jar goes to horizon-port as it is.

              analyze <mod.jar> [--out DIR] [--neoforge-jar JAR[:JAR...]] [--loader auto|neoforge|fabric]
                  What the mod does, read from its class files and resources without decompiling
                  anything: its @Mod classes, its event listeners with the bus and the side of
                  each, what it registers, its configs, its Mixin classes and its access
                  transformers. Writes DIR/report.md and DIR/report.json (default DIR:
                  out/<mod id>). --neoforge-jar: NeoForge's own jars, read for the bus each event
                  is posted on; without them a bus is given only where the mod's code or
                  NeoForge's documentation tells it. A Fabric jar goes to horizon-port as it is.

              port <mod.jar> --horizon-src FOLDER [--horizon-revision SHA] [--i-am-the-author NAME] [--out DIR]
                   [--allow-approximate] [--horizon-tools DIR] [--neoforge-jar JAR[:JAR...]] [--loader auto|neoforge|fabric]
                   [--facts FILE]
                  The licence check, then horizon-port's own `port` on a copy of the jar: it
                  decompiles it, matches its Mixins against that Horizon source and translates
                  the Java that doesn't need the game to Rust. Writes a Horizon native-mod bundle
                  to DIR (default: out/<mod id>-horizon; a folder that is already there isn't
                  written over), with PORTING.md and report/. The decompiled Java isn't kept,
                  in the bundle or anywhere else. Each event listener is put on the hook point
                  that Horizon's hook catalogue (native-hooks.json in that source) names for its
                  event: switched on when the catalogue names one hook point with nothing more
                  to read, written commented out with the reason otherwise. --allow-approximate
                  also switches on those whose hook point comes with a note. A listener's own
                  code isn't translated: its helper has a TODO. The jar's data and assets are
                  carried over by Horizon's own horizon-port-pack and packed as <mod id>.hmod, a
                  content mod, by its cargo-horizon-mod (in --horizon-tools, else the
                  HORIZON_TOOLS variable, else the source's target/release; this tool doesn't
                  build them). A carry that declares blocks or items copies vanilla ones by
                  guess: it is left as a folder under todo/, or packed as it is with
                  --allow-approximate. With --facts (what `dump` wrote), the carry is
                  horizon-port-pack's pipeline (import, inventory, carry), which takes each
                  block's template and properties, and each item's, from those facts: it is
                  packed, and PORTING.md lists the blocks with no template and what a
                  template doesn't reproduce. With the machines file `dump` writes beside the
                  facts, the mod's block entities are put on Horizon's block-entity hook points
                  with what NeoForge gave of them (type, ticking, item slots and their faces,
                  energy, fluid tanks); what they do is a TODO, and report/machines.md lists
                  what isn't carried over. Nothing of the mod is compiled or run. The Horizon commit is
                  --horizon-revision, else the source's .horizon-revision, else git's HEAD there.
                  A Fabric jar goes to horizon-port as it is.

              check <bundle-folder> --horizon-src FOLDER [--compile] [--horizon-tools DIR]
                  Goes over a bundle `port` wrote, or one you edited since, against that Horizon
                  source, by reading alone: the manifest reads and every file it names is there,
                  each helper is one function with a parameter for each binding, each target is
                  a function of that source (and what a binding names is something its hook
                  point takes there), the
                  hooks API the bundle asks for covers its hook points, the Rust files' brackets
                  pair up, no decompiled Java or class file is in it, and its .hmod passes
                  `cargo horizon-mod check`. It says how many TODOs are left. --compile then
                  runs horizon-port's own `check`: cargo type-checks the bundle's Rust against
                  stand-ins for Horizon's helpers (it writes <bundle>/target/; nothing of the
                  mod is run). The last line is `RESULT: ok`, `RESULT: ok, not compiled` or
                  `RESULT: failed (...)`; exit code 0 or 1.

              translate <mod.jar> [--out DIR] [--server FOLDER] [--bridge PATH] [--blockers]
                  This tool's own translator, from the mod's bytecode to Rust (no decompiling):
                  numbers with Java's exact semantics (wrapping arithmetic, NaN, casts),
                  branches, loops and switches, Math's methods with an exact counterpart;
                  objects, fields, static fields and class initializers, arrays,
                  inheritance, interfaces and default methods, lambdas and method
                  references, casts and null, in one uniform object model; exceptions
                  (try/catch/finally, class initializers that throw); Java's library as a
                  model of this tool's own, translated with the mod: texts, boxes, records,
                  enums, java.util.function, collections, Optional. Not yet: Class objects,
                  streams, the game. Writes DIR/translated.rs
                  and DIR/translation.md (default: out/<mod id>-translated/), which says
                  why each other method isn't translated. Nothing is compiled or run.
                  --server: a NeoForge server's folder (as for dump): the supertypes of the
                  classes in its libraries (the game's, NeoForge's, theirs) are read, so a
                  question such as "is this class of the mod a List?" can be answered
                  when the class implements one of the game's interfaces. Only the
                  server's classes: the client's stay unknown. --bridge: also writes
                  DIR/bridge.rs, Horizon's side of the bridge to the translation (which
                  it reaches as PATH, a Rust path such as super::port_mymod_java): it
                  keeps the mod's block entities and ticks them. --blockers: also writes
                  DIR/blockers.md, an estimate of what keeps the mod's methods out: every
                  reason of each, followed through the methods it calls, and which
                  missing pieces alone would bring the most of them in.

              standins --server FOLDER [--horizon-src FOLDER [--python PROGRAM]]
                  The translator's stand-ins for the game (game/ in this tool's source) against
                  the game itself: their superclasses, interfaces and methods against the
                  classes in the server's libraries, and their answers against the game's.
                  One program (probe/) is compiled against the server's jars and against the
                  stand-ins, each is run, and what they print must be the same. That runs
                  the game's classes as a library, to observe them: no server is started,
                  no world, no mod; their code isn't read. Exit 1 when they differ.
                  --horizon-src: the probe also runs as a mod in the server, through
                  Horizon's NeoForge oracle as dump runs a mod (the game's registries
                  filled), and its answers there are compared too.

              dump <mod.jar> --server FOLDER --horizon-src FOLDER [--out FILE] [--python PROGRAM]
                  The mod's real facts, read in NeoForge itself by Horizon's own oracle
                  (tools/neoforge_oracle.py in Horizon's source, run unchanged): every block
                  of the mod's namespaces with each state's hardness, light, tool need, map
                  colour, sounds, shapes and drops; its items with their components; its
                  recipes and tags; and vanilla's blocks, for templates. Beside the oracle,
                  this tool's machines exporter (compiled against the server's jars) places
                  each block that has a block entity and writes FILE's .machines.json: its
                  type, whether it ticks, its saved keys, its item, energy and fluid
                  handlers through each face, and its menu; without a compiler or the
                  server's jars, the dump goes on without it. --server is a
                  NeoForge server made by NeoForge's installer (--install-server) for the
                  mod's game version: the oracle is given it (a link in Horizon's
                  .local/neoforge-oracle/), so it downloads nothing. Its eula.txt must say
                  eula=true, which is yours to write: this tool never does, and stops
                  without it. This runs the original mod's Java in NeoForge, on this
                  machine. Writes FILE (default: out/<mod id>-facts.json), the format
                  `port --facts` reads, and the oracle's output beside it. Nothing is
                  compiled or run in Horizon.

              pack <bundle-folder> --horizon-src FOLDER [--out FILE] [--horizon-tools DIR]
                  Packs a bundle as <mod id>.hnmod, the file Horizon's launcher installs and
                  builds into the game (default FILE: in the bundle's folder). First the checks
                  of `check`, by reading; a bundle with a problem isn't packed. The file is made
                  as Horizon's docs/api/mod-files.md says: horizon-native.json, then the files of
                  mixins/<id>/, with the Horizon commit pinned in horizon-mixin.toml, and it is
                  put through `cargo horizon-mod check`. A bundle that isn't a finished port is
                  packed as it is, TODOs and all: built, it changes nothing in the game yet, and
                  the build tells whether Horizon takes it and compiles it. Nothing is compiled
                  and nothing is installed here: building native code is yours to start.

            horizon-port.jar is Horizon-Translator's converter, built in its clone
            (bash mod-converter/build.sh). It is found through --translator, else the
            HORIZON_PORT_JAR variable, else a Horizon-Translator/ folder beside this tool.

            Exit codes: 0 done, 1 failed, 2 wrong command line, 3 the licence doesn't allow it.
            """;
}
