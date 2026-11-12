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
            default -> throw new Usage("unknown command `" + args[0] + "`");
        }
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

            Exit codes: 0 done, 1 failed, 2 wrong command line.
            """;
}
