package horizonneo;

import java.io.IOException;
import java.io.PrintStream;
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
                        "--i-am-the-author", "--horizon-tools"), Set.of("--allow-approximate")), out, err);
            }
            case "check" -> {
                return check(Line.read(args, Set.of("--horizon-src", "--translator", "--horizon-tools"), Set.of("--compile")), out, err);
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

    // ---- port ----

    private static int port(Line line, PrintStream out, PrintStream err) throws Usage, IOException, ModsToml.Invalid {
        Path path = line.only("mod jar");
        String horizonSrc = line.options().get("--horizon-src");
        if (horizonSrc == null) throw new Usage("`port` needs --horizon-src <Horizon's source folder>");
        ModJar jar = ModJar.open(path);
        String outDir = line.options().get("--out"), revision = line.options().get("--horizon-revision"), author = line.options().get("--i-am-the-author");
        if (loader(jar, line.option("--loader", "auto")) == Loader.FABRIC) {
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
        Port.Options options = new Port.Options(Path.of(horizonSrc), revision, author, outDir == null ? null : Path.of(outDir), line.options().get("--translator"),
                line.options().get("--horizon-tools"), line.has("--allow-approximate"));
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
                  --allow-approximate. Nothing of the mod is compiled or run. The Horizon commit is
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

            horizon-port.jar is Horizon-Translator's converter, built in its clone
            (bash mod-converter/build.sh). It is found through --translator, else the
            HORIZON_PORT_JAR variable, else a Horizon-Translator/ folder beside this tool.

            Exit codes: 0 done, 1 failed, 2 wrong command line, 3 the licence doesn't allow it.
            """;
}
