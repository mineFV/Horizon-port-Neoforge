package horizonneo;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * `check`: goes over a bundle `port` wrote, or one edited since, against a Horizon source.
 *
 * By reading alone, it makes sure of what horizon-mixins and the launcher go by: the manifest reads,
 * every file it names is there, each helper is one function with a parameter for each binding, each
 * target is a function of that source (and what a binding names is something its hook point takes), the
 * hooks API the bundle asks for covers its hook points, the Rust files' brackets pair up, no
 * decompiled Java or class file is in it, and its `.hmod` passes Horizon's own check.
 *
 * With `--compile` it then runs horizon-port's own `check`, which has cargo type-check the bundle's
 * Rust against stand-ins for Horizon's helpers (`cargo check`: nothing of the mod is run). That is
 * never done unasked.
 */
final class Check {
    /** horizonSrc: Horizon's source. translator, tools: where to find them, null to look. compile: also run horizon-port's check. */
    record Options(Path horizonSrc, String translator, String tools, boolean compile) {}

    /** One hook point of the source, as horizon-port lists it. */
    private record Hook(int api, Set<String> takes) {}

    private static final String MANIFEST = "horizon-mixin.toml";
    private static final Pattern COMMENTED_ID = Pattern.compile("(?m)^# id = \"([^\"]+)\"$");
    /** A binding's value that is one name, as it is or borrowed: `button`, `&mut *self`. Anything longer isn't judged. */
    private static final Pattern ONE_NAME = Pattern.compile("(?:&\\s*(?:mut\\s+)?)?\\*?\\s*([a-z_][a-z0-9_]*)");
    private static final Pattern MOD_FILE = Pattern.compile("(?m)^\\s*(?:pub(?:\\([a-z:\\s]*\\))?\\s+)?mod\\s+[A-Za-z_][A-Za-z0-9_]*\\s*;");

    private final PrintStream out;
    private int problems, unchecked;

    private Check(PrintStream out) {
        this.out = out;
    }

    static int run(Path bundle, Options o, PrintStream out, PrintStream err) throws IOException {
        if (!Files.isDirectory(bundle)) throw new IOException(bundle + " isn't a folder: `check` takes the bundle `port` wrote");
        if (!Files.isDirectory(o.horizonSrc())) throw new IOException("--horizon-src: " + o.horizonSrc() + " isn't a folder");
        Check check = new Check(out);
        boolean readable = check.read(bundle, o);
        boolean compiled = false;
        if (readable && o.compile()) compiled = check.compile(bundle, o);
        else if (readable) {
            out.println("Nothing was compiled. --compile also has cargo type-check the bundle's Rust (horizon-port's `check`; it runs nothing of the mod).");
        }
        if (check.problems > 0) {
            out.println("RESULT: failed (" + check.problems + (check.problems == 1 ? " problem)" : " problems)"));
            return Main.FAILED;
        }
        out.println("RESULT: ok" + (compiled ? "" : ", not compiled") + (check.unchecked > 0 ? " (" + check.unchecked + " not checked, above)" : ""));
        return Main.DONE;
    }

    private void ok(String what) {
        out.println("  ok: " + what);
    }

    private void problem(String what) {
        problems++;
        out.println("  problem: " + what);
    }

    private void note(String what) {
        out.println("  note: " + what);
    }

    private void notChecked(String what) {
        unchecked++;
        out.println("  not checked: " + what);
    }

    // ---- by reading ----

    /** false when the bundle can't be read at all. */
    private boolean read(Path bundle, Options o) throws IOException {
        Path mod = modFolder(bundle);
        if (mod == null) return false;
        String manifest = Files.readString(mod.resolve(MANIFEST), StandardCharsets.UTF_8);
        Map<String, Object> root;
        try {
            root = Toml.parse(manifest).root();
        } catch (Toml.Problem e) {
            problem(MANIFEST + " can't be read: " + e.getMessage());
            return false;
        }
        String id = Toml.text(Toml.table(root.get("mod")), "id");
        List<Map<String, Object>> injections = Toml.tables(root.get("injection"));
        List<String> off = new ArrayList<>();
        Matcher commented = COMMENTED_ID.matcher(manifest);
        while (commented.find()) off.add(commented.group(1));
        out.println("Checking " + bundle + ": the mod `" + (id == null ? "?" : id) + "`, " + count(injections.size(), "injection") + " on, " + off.size()
                + " written commented out");
        if (id == null || id.isBlank()) problem(MANIFEST + " has no [mod] id");
        else if (!id.equals(mod.getFileName().toString())) problem("the mod's id is `" + id + "`, but its folder is mixins/" + mod.getFileName());

        Set<Path> rust = new LinkedHashSet<>();
        modules(mod, root, rust);
        Map<String, Hook> hooks = new LinkedHashMap<>();
        Integer api = hookList(o, hooks);
        helpers(mod, injections, rust);
        targets(injections, o.horizonSrc(), api == null ? null : hooks);
        api(bundle, injections, api, hooks);
        revision(bundle, o.horizonSrc());
        brackets(bundle, rust);
        leftovers(bundle);
        content(bundle, id, o);
        todo(bundle, rust, off.size());
        return true;
    }

    /** mixins/<id>/, the one folder that holds the manifest; null, with the problem said, when there isn't one. */
    private Path modFolder(Path bundle) throws IOException {
        Path mixins = bundle.resolve("mixins");
        List<Path> found = List.of();
        if (Files.isDirectory(mixins)) {
            try (Stream<Path> s = Files.list(mixins)) {
                found = s.filter(p -> Files.isRegularFile(p.resolve(MANIFEST))).sorted().toList();
            }
        }
        if (found.size() == 1) return found.get(0);
        out.println("Checking " + bundle);
        problem(found.isEmpty() ? "it has no mixins/<mod id>/" + MANIFEST + ": it isn't a native-mod bundle"
                : found.size() + " folders under mixins/ have a " + MANIFEST + ": a bundle holds one mod");
        return null;
    }

    /** The [[module]] files: there, and within the rules horizon-mixins sets for a file it pastes in as a module. */
    private void modules(Path mod, Map<String, Object> root, Set<Path> rust) throws IOException {
        int before = problems, seen = 0;
        for (Map<String, Object> module : Toml.tables(root.get("module"))) {
            String name = Toml.text(module, "id"), source = Toml.text(module, "source");
            String what = "the module `" + (name == null ? "?" : name) + "`";
            if (name == null) problem("a [[module]] has no id");
            if (Toml.text(module, "file") == null || Toml.text(module, "name") == null) problem(what + " lacks `file` or `name`");
            Path file = source == null ? null : inside(mod, source);
            if (file == null || !Files.isRegularFile(file)) {
                problem(what + " names the file " + source + ", which isn't there");
                continue;
            }
            seen++;
            rust.add(file);
            String text = Files.readString(file, StandardCharsets.UTF_8), code = RustText.code(text);
            if (text.lines().anyMatch(l -> l.strip().startsWith("//!"))) problem(source + " has a `//!` comment: horizon-mixins doesn't take one in a module");
            if (code.contains("#![")) problem(source + " has a `#![...]` attribute: horizon-mixins doesn't take one in a module");
            if (MOD_FILE.matcher(code).find()) problem(source + " has a `mod x;` naming another file: only inline `mod x { ... }` works in a module");
            if (Pattern.compile("\\binclude(?:_str|_bytes)?!").matcher(code).find()) problem(source + " uses `include!`: a module is pasted into Horizon, away from the bundle's files");
        }
        if (problems == before) ok(count(seen, "module") + ": each file is there, with nothing horizon-mixins refuses in a module");
    }

    /** Each injection that is on: complete, with a helper that is one function taking its bindings. */
    private void helpers(Path mod, List<Map<String, Object>> injections, Set<Path> rust) throws IOException {
        int before = problems;
        Set<String> ids = new LinkedHashSet<>();
        for (Map<String, Object> injection : injections) {
            String id = Toml.text(injection, "id"), helper = Toml.text(injection, "helper");
            String what = "the injection `" + (id == null ? "?" : id) + "`";
            if (id == null) problem("an [[injection]] has no id");
            else if (!ids.add(id)) problem("two injections have the id `" + id + "`");
            if (Toml.text(injection, "kind") == null) problem(what + " has no kind");
            String target = Toml.text(injection, "target");
            if (target == null || target.isBlank() || target.equals("TODO")) problem(what + " has no target (" + (target == null ? "none" : "`" + target + "`") + ")");
            Path file = helper == null ? null : inside(mod, helper);
            if (file == null || !Files.isRegularFile(file)) {
                problem(what + " names the helper " + helper + ", which isn't there");
                continue;
            }
            rust.add(file);
            List<RustText.Function> functions = RustText.functions(RustText.code(Files.readString(file, StandardCharsets.UTF_8)));
            int bindings = Toml.tables(injection.get("bindings")).size();
            if (functions.size() != 1) {
                problem(helper + " holds " + count(functions.size(), "function") + " outside any block: a helper is one function");
            } else if (functions.get(0).parameters() != bindings) {
                problem(helper + ": `fn " + functions.get(0).name() + "` takes " + count(functions.get(0).parameters(), "parameter") + ", and " + what + " has "
                        + count(bindings, "binding"));
            }
        }
        if (problems == before) ok(count(injections.size(), "injection") + " on: each has its helper, one function with a parameter for each binding");
    }

    /**
     * horizon-port's list of that source's hook points (`hooks --json`): fills `hooks` by target and
     * returns the source's hooks API; null, with the reason said, when the list can't be had.
     */
    private Integer hookList(Options o, Map<String, Hook> hooks) throws IOException {
        Translator translator;
        try {
            translator = Translator.locate(o.translator());
        } catch (IOException e) {
            notChecked("the hook points' bindings and API (horizon-port.jar lists them, and it wasn't found: --translator, or " + Translator.VARIABLE + ")");
            return null;
        }
        Path work = Files.createTempDirectory("horizon-port-neoforge-check");
        try {
            Path list = work.resolve("hooks.json");
            Translator.Run run = translator.run(List.of(Port.STACK), List.of("hooks", "--horizon-src", o.horizonSrc().toAbsolutePath().toString(), "--json",
                    "--out", list.toString()), 600, null);
            if (run.exit() != 0 || !Files.isRegularFile(list)) {
                notChecked("the hook points' bindings and API (horizon-port couldn't list that source's hook points: " + tail(run.err().isBlank() ? run.out() : run.err()) + ")");
                return null;
            }
            Map<String, Object> read = Json.object(Json.parse(Files.readString(list, StandardCharsets.UTF_8)));
            for (Object h : Json.array(read.get("hooks"))) {
                Map<String, Object> hook = Json.object(h);
                String target = Json.text(hook, "target");
                if (!"hook".equals(Json.text(hook, "kind")) || target == null) continue;
                hooks.put(target, new Hook(hook.get("api") instanceof Number n ? n.intValue() : 0, taken(Json.text(hook, "signature"))));
            }
            return read.get("hooks_api") instanceof Number n ? Integer.valueOf(n.intValue()) : Integer.valueOf(0);
        } catch (Json.Invalid e) {
            notChecked("the hook points' bindings and API (horizon-port's list isn't JSON: " + e.getMessage() + ")");
            return null;
        } finally {
            Port.delete(work);
        }
    }

    /** The names a function takes, `self` among them, from its signature; none when it can't be read. */
    static Set<String> taken(String signature) {
        Set<String> names = new LinkedHashSet<>();
        if (signature == null) return names;
        int open = signature.indexOf('('), depth = 0, start = open + 1;
        if (open < 0) return names;
        for (int i = open + 1; i < signature.length(); i++) {
            char c = signature.charAt(i);
            boolean end = c == ')' && depth == 0;
            if (end || (c == ',' && depth == 0)) {
                String one = signature.substring(start, i).strip();
                int colon = one.indexOf(':');
                String name = (colon < 0 ? one : one.substring(0, colon)).replaceAll("^(?:&\\s*)?(?:'\\w+\\s+)?(?:mut\\s+)?", "").strip();
                if (name.matches("[a-z_][a-z0-9_]*")) names.add(name);
                start = i + 1;
                if (end) break;
            } else if (c == '(' || c == '[' || c == '<') depth++;
            else if (c == ')' || c == ']' || (c == '>' && signature.charAt(i - 1) != '-')) depth--;
        }
        return names;
    }

    /** Each target is in the source, and a binding that is one name names something its hook point takes there. */
    private void targets(List<Map<String, Object>> injections, Path horizonSrc, Map<String, Hook> hooks) {
        int before = problems, points = 0, others = 0;
        for (Map<String, Object> injection : injections) {
            String id = Toml.text(injection, "id"), target = Toml.text(injection, "target");
            if (target == null || target.isBlank() || target.equals("TODO")) continue;
            String missing = Bundle.missing(horizonSrc, target);
            if (missing != null) {
                problem("the injection `" + id + "` goes into `" + target + "`, and " + missing);
                continue;
            }
            Hook hook = hooks == null ? null : hooks.get(target);
            if (hook == null) {
                others++;
                continue;
            }
            points++;
            if (hook.takes().isEmpty()) continue;
            for (Map<String, Object> b : Toml.tables(injection.get("bindings"))) {
                String value = Toml.text(b, "value");
                Matcher one = value == null ? null : ONE_NAME.matcher(value.strip());
                if (one != null && one.matches() && !hook.takes().contains(one.group(1))) {
                    problem("the injection `" + id + "` binds `" + value + "`, and its hook point takes no `" + one.group(1) + "` in this Horizon (it takes "
                            + String.join(", ", hook.takes()) + ")");
                }
            }
        }
        if (problems == before) {
            ok("every target is in this Horizon's source" + (hooks == null ? "" : ": " + count(points, "hook point") + ", each binding naming something it takes"
                    + (others > 0 ? ", " + count(others, "other function") : "")));
        }
    }

    /** The hooks API the bundle asks for: at least what its hook points came in, at most what the source has. */
    private void api(Path bundle, List<Map<String, Object>> injections, Integer sourceApi, Map<String, Hook> hooks) throws IOException {
        Map<String, Object> compat = compatibility(bundle);
        if (compat == null) return;
        if (sourceApi == null) return; // said already, by hookList
        if (!(compat.get("hooks") instanceof Number asked)) {
            problem("compatibility.json names no hooks API (`hooks`): the launcher can't tell which Horizon the mod builds on");
            return;
        }
        int need = 0;
        String from = null;
        for (Map<String, Object> injection : injections) {
            Hook hook = hooks.get(String.valueOf(Toml.text(injection, "target")));
            if (hook != null && hook.api() > need) {
                need = hook.api();
                from = Toml.text(injection, "id");
            }
        }
        if (asked.intValue() < need) {
            problem("compatibility.json asks for hooks API " + asked.intValue() + ", and the hook point of `" + from + "` came in API " + need);
        } else if (asked.intValue() > sourceApi) {
            problem("compatibility.json asks for hooks API " + asked.intValue() + ", and this Horizon has API " + sourceApi);
        } else {
            ok("hooks API " + asked.intValue() + " covers its hook points (they need " + need + "; this Horizon has " + sourceApi + ")");
        }
    }

    private Map<String, Object> compatibility(Path bundle) throws IOException {
        Path file = bundle.resolve("compatibility.json");
        if (!Files.isRegularFile(file)) {
            problem("there is no compatibility.json: the launcher reads the Horizon and the hooks API the mod needs from it");
            return null;
        }
        try {
            return Json.object(Json.parse(Files.readString(file, StandardCharsets.UTF_8)));
        } catch (Json.Invalid e) {
            problem("compatibility.json can't be read: " + e.getMessage());
            return null;
        }
    }

    /** Whether the bundle was made for the commit of this source. A difference isn't a problem: the targets were looked up in this one. */
    private void revision(Path bundle, Path horizonSrc) throws IOException {
        Path file = bundle.resolve("compatibility.json");
        if (!Files.isRegularFile(file)) return;
        String made;
        try {
            made = Json.text(Json.object(Json.parse(Files.readString(file, StandardCharsets.UTF_8))), "horizon_revision");
        } catch (Json.Invalid e) {
            return;
        }
        String here = Port.sourceRevision(horizonSrc);
        if (made == null || made.isBlank()) note("the bundle names no Horizon commit");
        else if (here == null) note("the bundle was made for Horizon " + made + "; this source's commit isn't known");
        else if (here.equals(made)) ok("it was made for this Horizon commit (" + made + ")");
        else note("the bundle was made for Horizon " + made + ", and this source is " + here + ": what is checked here is checked against this source");
    }

    /** The brackets of each Rust file that is on. */
    private void brackets(Path bundle, Set<Path> rust) throws IOException {
        int before = problems;
        for (Path file : rust) {
            for (String wrong : RustText.brackets(RustText.code(Files.readString(file, StandardCharsets.UTF_8)))) {
                problem(bundle.relativize(file).toString().replace('\\', '/') + ", " + wrong);
            }
        }
        if (problems == before) ok("the brackets pair up in the " + count(rust.size(), "Rust file") + " that " + (rust.size() == 1 ? "is" : "are") + " on");
    }

    /** Nothing of the mod's own code is in the bundle: no decompiled Java, no class file, no jar. */
    private void leftovers(Path bundle) throws IOException {
        int before = problems;
        for (Path file : Bundle.files(bundle)) {
            String path = bundle.relativize(file).toString().replace('\\', '/'), name = file.getFileName().toString();
            if (path.startsWith("target/")) continue;
            if (name.endsWith(".java") || name.endsWith(".class") || name.endsWith(".jar")) {
                problem(path + ": the mod's own code doesn't belong in a bundle");
            } else if ((name.endsWith(".rs") || name.endsWith(".toml")) && !path.startsWith("content/") && !path.startsWith("todo/content")) {
                long java = Files.readString(file, StandardCharsets.UTF_8).lines().filter(l -> l.strip().startsWith("// |")).count();
                if (java > 0) problem(path + " quotes decompiled Java (" + count((int) java, "line") + " starting with `// |`): decompiled code isn't kept in a bundle");
            }
        }
        if (problems == before) ok("no decompiled Java, class file or jar is in it");
    }

    /** The content mod: its `.hmod` through Horizon's own check; a folder left under todo/ is said. */
    private void content(Path bundle, String id, Options o) throws IOException {
        List<Path> packed;
        try (Stream<Path> s = Files.list(bundle)) {
            packed = s.filter(p -> p.getFileName().toString().endsWith(".hmod") && Files.isRegularFile(p)).sorted().toList();
        }
        for (Path file : packed) {
            String name = file.getFileName().toString();
            if (id != null && !name.equals(id + ".hmod")) note(name + " isn't named after the mod (`" + id + "`)");
            HorizonTools tools;
            try {
                tools = HorizonTools.locate(o.tools(), o.horizonSrc());
            } catch (IOException e) {
                problem(e.getMessage());
                continue;
            }
            if (tools == null) {
                notChecked(name + " (Horizon's cargo-horizon-mod isn't built: " + HorizonTools.HOW + ")");
                continue;
            }
            HorizonTools.Run run = tools.horizonMod(List.of("check", file.toAbsolutePath().toString()), bundle, 300);
            if (run.exit() == 0) ok(name + " passes `cargo horizon-mod check`");
            else problem(name + " doesn't pass `cargo horizon-mod check`: " + tail(run.err().isBlank() ? run.out() : run.err()));
        }
        if (Files.isDirectory(bundle.resolve(Content.TODO_FOLDER))) {
            note("the mod's content is a folder under " + Content.TODO_FOLDER + "/, not packed: PORTING.md says why");
        }
    }

    /** What is left to do: not a problem, the bundle is made that way. */
    private void todo(Path bundle, Set<Path> rust, int off) throws IOException {
        int todos = 0, files = 0;
        for (Path file : rust) {
            long n = Files.readString(file, StandardCharsets.UTF_8).lines().filter(l -> l.contains("TODO")).count();
            todos += (int) n;
            if (n > 0) files++;
        }
        out.println("Left to do: " + count(todos, "TODO") + " in " + count(files, "file") + " of the code that is on, " + count(off, "injection")
                + " written commented out (PORTING.md lists them).");
    }

    // ---- with cargo ----

    /** horizon-port's own `check`, when it was asked for. true when cargo did type-check the bundle. */
    private boolean compile(Path bundle, Options o) throws IOException {
        Translator translator;
        try {
            translator = Translator.locate(o.translator());
        } catch (IOException e) {
            problem("--compile: " + e.getMessage());
            return false;
        }
        out.println("Compiling: horizon-port's `check` (cargo check on the bundle's Rust, against stand-ins for Horizon's helpers; nothing of the mod is run).");
        out.flush();
        Translator.Run run = translator.run(List.of(Port.STACK), List.of("check", bundle.toAbsolutePath().toString(), "--horizon-src",
                o.horizonSrc().toAbsolutePath().toString()), 3600, null);
        for (String line : (run.out() + run.err()).split("\n")) if (!line.isBlank()) out.println("    " + line.stripTrailing());
        if (run.exit() != 0) {
            problem("horizon-port's check failed (exit " + run.exit() + ", above). It fails on code that uses the game's own types even when they are right: "
                    + "`horizon-port compile` checks those, against Horizon itself");
            return false;
        }
        if (run.out().contains("No cargo found")) {
            notChecked("the compile: there is no cargo on this machine");
            return false;
        }
        ok("horizon-port's check passes");
        return true;
    }

    // ---- words ----

    /** The file a manifest names, when it stays inside the mod's folder. */
    private static Path inside(Path mod, String name) {
        try {
            Path file = mod.resolve(name).normalize();
            return file.startsWith(mod.normalize()) ? file : null;
        } catch (java.nio.file.InvalidPathException e) {
            return null;
        }
    }

    private static String count(int n, String what) {
        return n + " " + what + (n == 1 ? "" : "s");
    }

    private static String tail(String text) {
        String[] lines = text.strip().split("\n");
        return lines[lines.length - 1].strip();
    }
}
