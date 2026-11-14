package horizonneo;

import horizonneo.Analysis.Injector;
import horizonneo.Analysis.MixinClass;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A bundle horizon-port wrote from the stand-in jar, made right for a NeoForge mod.
 *
 * horizon-port wrote it for a Fabric mod, by its own rules for one. What is changed here, and why:
 *
 * - The decompiled Java it copies into the helpers as comments is taken out: decompiled code isn't
 *   kept in a bundle.
 * - The `.hnmod` it packs when it sees nothing left is removed: it was packed before that, and
 *   horizon-port doesn't see what a NeoForge mod does outside its Mixins.
 * - The settings page it turns on when a class extends one of the game's screens is commented out,
 *   unless the mod itself says it has a config screen: a class's name isn't a correspondence.
 * - An injection whose target isn't in the Horizon source given is commented out: horizon-port's
 *   table of Horizon's functions was written for one Horizon, and the source may be another.
 * - The mod's NeoForge event listeners are put on Horizon's hook points (`Wiring`): on when the
 *   event table is sure of the hook point, written commented out with the reason when it isn't.
 * - Where it names Fabric as the mod's loader, it says NeoForge.
 *
 * Everything else is horizon-port's, as it wrote it. PORTING.md gets a section that says all this.
 */
final class Bundle {
    private Bundle() {}

    /** horizon-port's count of the mod's Java it translated to Rust. fields is null when it gave none. */
    record Translation(int methods, int methodsOf, Integer fields, Integer fieldsOf) {}

    /**
     * What became of one Mixin handler. status, horizon, note: horizon-port's own words for it.
     * state: "on", "off" (written commented out) or "absent" (not found in the bundle); injection:
     * the `[[injection]]` it is in, when there is one.
     */
    record Handler(String status, String horizon, String note, String state, String injection) {}

    /**
     * on, off: the manifest's injections by id, as it stands after the changes. handlers: by
     * `com/example/SomeMixin.handler`. listeners: what became of each event listener, in the
     * analysis's order; none when there was no event table to go by. content: what became of the
     * mod's data and assets, null when that wasn't tried. changes: what was changed here, in
     * sentences.
     */
    record Outcome(String modId, List<String> on, List<String> off, Map<String, Handler> handlers, List<Wiring.Wired> listeners,
                   Content.Result content, Translation translation, List<String> changes, int javaLines, Machines.Made machines) {
        Outcome {
            on = List.copyOf(on);
            off = List.copyOf(off);
            listeners = List.copyOf(listeners);
            changes = List.copyOf(changes);
        }

        /** How many listeners are on a hook point. */
        long listenersOn() {
            return listeners.stream().filter(w -> w.state().equals("on")).count();
        }
    }

    /** The step that carries the mod's data and assets over into the bundle, once the bundle is in order. */
    interface Carrier {
        Content.Result carry(Path bundle) throws IOException;
    }

    /** horizon-port's bundle isn't what this expects of one: nothing is delivered. */
    static final class Unexpected extends IOException {
        private static final long serialVersionUID = 1L;

        Unexpected(String message) {
            super("horizon-port's bundle isn't laid out as expected (" + message + "): nothing was written");
        }
    }

    private static final String MANIFEST = "horizon-mixin.toml";
    private static final Pattern JAVA_HEADER = Pattern.compile("^(\\s*)// (?:Decompiled Java|Java), (.+?):\\d+:\\s*$");
    private static final Pattern COUNTS = Pattern.compile("\\d+ injections? (?:is|are) on; \\d+ (?:is|are) written commented out");
    private static final Pattern TODO_HEAD = Pattern.compile("^- \\d+ TODOs? (?:is|are) left in the code:$");
    private static final String FULL = "**Yes: this is a full port.**";
    static final String SECTION = "## From a NeoForge mod";

    /**
     * Makes the changes. dir: the bundle. decompiled: the folder horizon-port decompiled into, read
     * only to make sure none of it stays in the bundle. horizonSrc: the Horizon source the mod was
     * converted against. table: which hook point stands for which NeoForge event in that source,
     * null when it has no hook catalogue. approximate: also switch on the listeners whose hook
     * point comes with a note. carrier: carries the content over, null to leave it. machines: what
     * NeoForge gave of the mod's block entities (`dump`'s machines file), null when there is none.
     */
    static Outcome finish(Path dir, Path decompiled, Path horizonSrc, ModInfo info, Analysis analysis, EventTable table, boolean approximate,
                          Carrier carrier, Map<String, Object> machines) throws IOException {
        Path mod = modFolder(dir);
        String id = mod.getFileName().toString();
        List<String> changes = new ArrayList<>();

        boolean packed = false;
        for (Path f : files(dir)) {
            if (!f.getFileName().toString().endsWith(".hnmod")) continue;
            Files.delete(f);
            packed = true;
        }

        int javaLines = stripJava(dir, javaLines(decompiled));
        if (javaLines > 0) {
            changes.add("The decompiled Java horizon-port copies into the helpers as comments was taken out (" + javaLines
                    + (javaLines == 1 ? " line" : " lines") + "): decompiled code isn't kept in a bundle. Each helper still names the Java file it stands in for.");
        }

        // Injections this turns off, with what their helper says: it names the Java each stood in for.
        Map<String, String> turnedOff = new LinkedHashMap<>();
        List<String> hooksOff = new ArrayList<>(settings(dir, mod, analysis, changes, turnedOff));
        hooksOff.addAll(targets(dir, mod, horizonSrc, changes, turnedOff));
        reword(dir, mod, info);
        List<Wiring.Wired> listeners = table == null ? List.of() : Wiring.apply(mod, id, analysis, table, approximate);
        Machines.Made made = table == null || machines == null ? null : Machines.apply(mod, id, machines, table);

        Map<String, Handler> theirs = mixinStatuses(dir.resolve("report").resolve("report.json"));
        Translation translation = translation(dir.resolve("report").resolve("translation.md"));
        // Their report describes the stand-in as a Fabric mod; it is kept beside this tool's own.
        for (String name : List.of("report.md", "report.json")) {
            Path f = dir.resolve("report").resolve(name);
            if (Files.isRegularFile(f)) Files.move(f, f.resolveSibling("horizon-port-" + name));
        }

        String manifest = read(mod.resolve(MANIFEST));
        List<String> on = new ArrayList<>(), off = new ArrayList<>();
        Map<String, String> helpers = injections(mod, manifest, on, off);
        Map<String, Handler> handlers = handlers(analysis, theirs, manifest, mod, helpers, turnedOff);

        // Last, so that nothing above goes through the mod's own files.
        Content.Result content = carrier == null ? null : carrier.carry(dir);
        Outcome outcome = new Outcome(id, on, off, handlers, listeners, content, translation, changes, javaLines, made);
        if (made != null) {
            Files.createDirectories(dir.resolve("report"));
            Files.writeString(dir.resolve("report").resolve("machines.md"), Machines.report(machines, made), StandardCharsets.UTF_8);
        }
        Path porting = dir.resolve("PORTING.md");
        if (!Files.isRegularFile(porting)) throw new Unexpected("no PORTING.md");
        Files.writeString(porting, porting(read(porting), info, analysis, outcome, mod, dir, packed, hooksOff), StandardCharsets.UTF_8);

        for (Path f : files(dir)) {
            String name = dir.relativize(f).toString().replace('\\', '/');
            // The content is the mod's own files, carried over: nothing decompiled can be in them.
            if (!text(f) || name.startsWith(Content.FOLDER + "/") || name.startsWith(Content.TODO_FOLDER)) continue;
            for (String line : read(f).split("\n", -1)) {
                if (line.strip().startsWith("// |")) throw new Unexpected("decompiled Java is still in " + name);
            }
        }
        return outcome;
    }

    /** mixins/<id>/, the one folder that holds the manifest. */
    private static Path modFolder(Path dir) throws IOException {
        Path mixins = dir.resolve("mixins");
        if (!Files.isDirectory(mixins)) throw new Unexpected("no mixins/ folder");
        List<Path> found;
        try (Stream<Path> s = Files.list(mixins)) {
            found = s.filter(p -> Files.isRegularFile(p.resolve(MANIFEST))).toList();
        }
        if (found.size() != 1) throw new Unexpected(found.size() + " folders with a " + MANIFEST + " under mixins/");
        return found.get(0);
    }

    // ---- the decompiled Java ----

    /** Every line of the decompiled source that is long enough to be told from a chance match. */
    static Set<String> javaLines(Path decompiled) throws IOException {
        Set<String> lines = new HashSet<>();
        if (decompiled == null || !Files.isDirectory(decompiled)) return lines;
        for (Path f : files(decompiled)) {
            if (!f.getFileName().toString().endsWith(".java")) continue;
            for (String line : read(f).split("\n")) {
                String t = line.strip();
                if (t.length() >= 16) lines.add(t);
            }
        }
        return lines;
    }

    /** Takes the copied Java out of every text file of the bundle and returns how many lines went. */
    static int stripJava(Path dir, Set<String> java) throws IOException {
        int removed = 0;
        for (Path f : files(dir)) {
            if (!text(f)) continue;
            String before = read(f);
            StringBuilder after = new StringBuilder();
            int here = 0;
            for (String line : before.split("\n", -1)) {
                String t = line.strip();
                Matcher header = JAVA_HEADER.matcher(line);
                if (t.startsWith("// |") || copied(f, t, java)) {
                    here++;
                    continue;
                }
                if (header.matches()) {
                    line = header.group(1) + "// Its Java is " + header.group(2) + " in the mod (decompiled code isn't kept in this bundle).";
                }
                after.append(line).append('\n');
            }
            after.setLength(after.length() - 1);
            if (after.toString().equals(before)) continue;
            Files.writeString(f, after, StandardCharsets.UTF_8);
            removed += here;
        }
        return removed;
    }

    /** A comment line that repeats a line of the decompiled source, whatever marks it as a comment. */
    private static boolean copied(Path file, String stripped, Set<String> java) {
        String name = file.getFileName().toString();
        String mark = name.endsWith(".rs") ? "//" : name.endsWith(".toml") ? "#" : null;
        if (mark == null || !stripped.startsWith(mark)) return false;
        String said = stripped.replaceFirst("^[/#|!\\s]+", "").strip();
        return java.contains(said);
    }

    // ---- the settings page ----

    /**
     * Comments out the settings page's two injections when they are an empty page horizon-port turned
     * on by itself and the mod doesn't say it has a config screen. Returns the hook points it turned off.
     */
    private static List<String> settings(Path dir, Path mod, Analysis analysis, List<String> changes, Map<String, String> turnedOff) throws IOException {
        String manifest = read(mod.resolve(MANIFEST));
        Map<String, String[]> pages = new LinkedHashMap<>();
        for (Map<String, Object> injection : Toml.tables(parse(manifest).get("injection"))) {
            String target = Toml.text(injection, "target"), id = Toml.text(injection, "id"), helper = Toml.text(injection, "helper");
            if (target == null || id == null || helper == null) continue;
            if (target.endsWith("::native_settings") || target.endsWith("::native_settings_click")) pages.put(id, new String[] {helper, target});
        }
        if (pages.isEmpty()) return List.of();
        boolean empty = false;
        for (String[] page : pages.values()) {
            Path helper = mod.resolve(page[0]);
            if (Files.isRegularFile(helper) && read(helper).contains("TODO")) empty = true;
        }
        if (!empty) return List.of();
        if (!analysis.configScreens().isEmpty()) {
            changes.add("The settings page (`" + String.join("`, `", pages.keySet()) + "`) is left on: the mod names NeoForge's `IConfigScreenFactory` in "
                    + String.join(", ", analysis.configScreens().stream().map(ClassData::simple).toList()) + ", so it has a config screen of its own.");
            return List.of();
        }
        List<String> hooks = new ArrayList<>();
        for (Map.Entry<String, String[]> page : pages.entrySet()) {
            String helper = page.getValue()[0];
            String hook = hook(page.getValue()[1]);
            hooks.add(hook);
            off(dir, mod, page.getKey(), helper, turnedOff, List.of(
                    "the `" + hook + "` hook point (the mod's settings page), turned off by horizon-port-neoforge.",
                    "horizon-port turns it on when a class of the mod extends one of the game's screens. That doesn't say the",
                    "mod has a settings page: it names no config screen of NeoForge's (IConfigScreenFactory).",
                    "TODO: if the mod has settings to show, fill in todo/" + helper + ", move it back beside this file and uncomment this."));
        }
        changes.add("The settings page (`" + String.join("`, `", pages.keySet()) + "`) is commented out in `" + MANIFEST + "`, its helpers moved to `todo/`. "
                + "horizon-port turns one on when a class of the mod extends one of the game's screens; the mod names no config screen of NeoForge's "
                + "(`IConfigScreenFactory`), so nothing says it has a settings page.");
        return hooks;
    }

    // ---- targets that aren't in this Horizon ----

    /**
     * Comments out every injection whose target function isn't in the Horizon source: horizon-port
     * takes hook points from a table written for one Horizon, and this source may be another.
     * A target that can't be looked up (the source is a zip, or its module isn't a file of its
     * own) is left as it is. Returns the hook points turned off.
     */
    private static List<String> targets(Path dir, Path mod, Path horizonSrc, List<String> changes, Map<String, String> turnedOff) throws IOException {
        List<String> hooks = new ArrayList<>();
        if (horizonSrc == null || !Files.isDirectory(horizonSrc)) return hooks;
        Map<String, Object> root = parse(read(mod.resolve(MANIFEST)));
        for (Map<String, Object> module : Toml.tables(root.get("module"))) {
            String file = Toml.text(module, "file");
            if (file != null && !Files.isRegularFile(horizonSrc.resolve(file))) {
                changes.add("Problem: the bundle adds its code to `" + file + "`, which isn't in the Horizon source given. It won't build on that Horizon as it is.");
            }
        }
        for (Map<String, Object> injection : Toml.tables(root.get("injection"))) {
            String target = Toml.text(injection, "target"), id = Toml.text(injection, "id"), helper = Toml.text(injection, "helper");
            if (target == null || id == null || helper == null) continue;
            String missing = missing(horizonSrc, target);
            if (missing == null) continue;
            off(dir, mod, id, helper, turnedOff, List.of(
                    "`" + target + "`, turned off by horizon-port-neoforge.",
                    "That function isn't in the Horizon source this was converted against (" + missing + ").",
                    "horizon-port's table of Horizon's functions was written for another Horizon.",
                    "TODO: find what this Horizon has in its place, then move todo/" + helper + " back beside this file and uncomment this."));
            changes.add("The injection `" + id + "` is commented out, its helper moved to `todo/`: its target, `" + target
                    + "`, isn't in the Horizon source given (" + missing + ").");
            if (target.contains("::native_")) hooks.add(hook(target));
        }
        return hooks;
    }

    /**
     * Why a target such as `horizon_client::game::native_hooks::Game<'_>::native_tick` isn't in the
     * source, in words; null when it is there or can't be looked up. The crate is `crates/<name>`,
     * each module after it a file or a folder, and the last part the function.
     */
    static String missing(Path horizonSrc, String target) {
        List<String> parts = new ArrayList<>(List.of(target.split("::")));
        if (parts.size() < 3) return null;
        Path at = horizonSrc.resolve("crates").resolve(parts.get(0).replace('_', '-')).resolve("src");
        if (!Files.isDirectory(at)) return null;
        String function = parts.get(parts.size() - 1);
        Path file = null;
        for (String part : parts.subList(1, parts.size() - 1)) {
            if (!part.matches("[A-Za-z_][A-Za-z0-9_]*")) break;
            if (Files.isRegularFile(at.resolve(part + ".rs"))) file = at.resolve(part + ".rs");
            else if (Files.isRegularFile(at.resolve(part).resolve("mod.rs"))) file = at.resolve(part).resolve("mod.rs");
            else break;
            at = at.resolve(part);
        }
        if (file == null || !function.matches("[A-Za-z_][A-Za-z0-9_]*")) return null;
        try {
            if (Pattern.compile("\\bfn\\s+" + function + "\\s*[<(]").matcher(read(file)).find()) return null;
        } catch (IOException e) {
            return null;
        }
        return "`" + horizonSrc.relativize(file).toString().replace('\\', '/') + "` has no `fn " + function + "`";
    }

    /** `tick` for a target ending in `::native_tick`. */
    private static String hook(String target) {
        return target.substring(target.lastIndexOf("::native_") + "::native_".length());
    }

    /** Comments one injection out in the manifest, with why, and moves its helper to todo/. */
    private static void off(Path dir, Path mod, String id, String helper, Map<String, String> turnedOff, List<String> why) throws IOException {
        String edited = turnOff(read(mod.resolve(MANIFEST)), id, why);
        if (edited == null) throw new Unexpected("the injection `" + id + "` can't be found again");
        Files.writeString(mod.resolve(MANIFEST), edited, StandardCharsets.UTF_8);
        Path from = mod.resolve(helper);
        String text = "";
        if (Files.isRegularFile(from)) {
            text = read(from);
            Path todo = dir.resolve("todo");
            Files.createDirectories(todo);
            Files.move(from, todo.resolve(from.getFileName().toString()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        turnedOff.put(id, text);
    }

    /**
     * The manifest with the `[[injection]]` of that id commented out, under `# Off:` and the reasons,
     * the way horizon-port writes the ones it leaves off. null when there is no such injection.
     */
    static String turnOff(String manifest, String id, List<String> why) {
        List<String> lines = new ArrayList<>(List.of(manifest.split("\n", -1)));
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).strip().equals("[[injection]]")) continue;
            int end = i + 1;
            while (end < lines.size() && !lines.get(end).isBlank() && !lines.get(end).startsWith("[")) end++;
            boolean it = false;
            for (int j = i; j < end; j++) if (lines.get(j).strip().equals("id = \"" + id + "\"")) it = true;
            if (!it) continue;
            List<String> block = new ArrayList<>();
            for (int k = 0; k < why.size(); k++) block.add((k == 0 ? "# Off: " : "#   ") + why.get(k));
            for (int j = i; j < end; j++) block.add("# " + lines.get(j));
            List<String> out = new ArrayList<>(lines.subList(0, i));
            out.addAll(block);
            out.addAll(lines.subList(end, lines.size()));
            return String.join("\n", out);
        }
        return null;
    }

    // ---- Fabric's name ----

    private static void reword(Path dir, Path mod, ModInfo info) throws IOException {
        Path cargo = dir.resolve("Cargo.toml");
        if (Files.isRegularFile(cargo)) {
            replace(cargo, " ported from Fabric to a Horizon native mod by horizon-port.",
                    " ported from NeoForge to a Horizon native mod by horizon-port, through horizon-port-neoforge.");
        }
        // The line horizon-port writes for a mod with no description of its own.
        Path runtime = mod.resolve("runtime.rs");
        if (Files.isRegularFile(runtime) && info.description().isBlank()) replace(runtime, ", ported from Fabric.\";", ", ported from NeoForge.\";");
    }

    private static void replace(Path file, String from, String to) throws IOException {
        String text = read(file);
        if (text.contains(from)) Files.writeString(file, text.replace(from, to), StandardCharsets.UTF_8);
    }

    // ---- what horizon-port reported ----

    /** Each Mixin handler's status in horizon-port's report.json, by `com/example/SomeMixin.handler`. */
    static Map<String, Handler> mixinStatuses(Path reportJson) throws IOException {
        Map<String, Handler> out = new LinkedHashMap<>();
        if (!Files.isRegularFile(reportJson)) return out;
        Map<String, Object> root;
        try {
            root = Json.object(Json.parse(read(reportJson)));
        } catch (Json.Invalid e) {
            return out;
        }
        for (Object o : Json.array(root.get("features"))) {
            Map<String, Object> f = Json.object(o);
            String in = Json.text(f, "in"), status = Json.text(f, "status");
            if (!"mixin".equals(Json.text(f, "category")) || in == null || status == null) continue;
            Handler h = new Handler(status, Json.text(f, "horizon"), Json.text(f, "note"), "absent", null);
            // One handler on several targets: the status furthest from done stands for all of them.
            out.merge(in, h, (a, b) -> new Handler(rank(a.status()) >= rank(b.status()) ? a.status() : b.status(),
                    both(a.horizon(), b.horizon(), "; "), both(a.note(), b.note(), " "), "absent", null));
        }
        return out;
    }

    private static int rank(String status) {
        return List.of("auto", "content", "needs-mapping", "needs-translation", "unsupported").indexOf(status);
    }

    private static String both(String a, String b, String between) {
        if (a == null || a.equals(b)) return b == null ? a : b;
        return b == null ? a : a + between + b;
    }

    /** horizon-port's own count, from the first lines of its translation.md. null when it isn't there. */
    static Translation translation(Path translationMd) throws IOException {
        if (!Files.isRegularFile(translationMd)) return null;
        String text = read(translationMd);
        Matcher methods = Pattern.compile("Translated (\\d+) of (\\d+) methods").matcher(text);
        if (!methods.find()) return null;
        Matcher fields = Pattern.compile("(\\d+) of (\\d+) static fields").matcher(text);
        boolean hasFields = fields.find();
        return new Translation(Integer.parseInt(methods.group(1)), Integer.parseInt(methods.group(2)),
                hasFields ? Integer.valueOf(fields.group(1)) : null, hasFields ? Integer.valueOf(fields.group(2)) : null);
    }

    // ---- the manifest ----

    private static Map<String, Object> parse(String manifest) throws IOException {
        try {
            return Toml.parse(manifest).root();
        } catch (Toml.Problem e) {
            throw new Unexpected(MANIFEST + " can't be read: " + e.getMessage());
        }
    }

    /**
     * Fills `on` and `off` with the manifest's injections and makes sure every file it names is there.
     * Returns each injection that is on with its helper file.
     */
    private static Map<String, String> injections(Path mod, String manifest, List<String> on, List<String> off) throws IOException {
        Map<String, Object> root = parse(manifest);
        Map<String, String> helpers = new LinkedHashMap<>();
        for (Map<String, Object> module : Toml.tables(root.get("module"))) {
            String source = Toml.text(module, "source");
            if (source == null || !Files.isRegularFile(mod.resolve(source))) throw new Unexpected("a [[module]] names the file " + source + ", which isn't there");
        }
        for (Map<String, Object> injection : Toml.tables(root.get("injection"))) {
            String id = Toml.text(injection, "id"), helper = Toml.text(injection, "helper");
            if (id == null) throw new Unexpected("an [[injection]] has no id");
            if (helper == null || !Files.isRegularFile(mod.resolve(helper))) throw new Unexpected("the injection `" + id + "` names the file " + helper + ", which isn't there");
            on.add(id);
            helpers.put(id, helper);
        }
        Matcher commented = Pattern.compile("(?m)^# id = \"([^\"]+)\"$").matcher(manifest);
        while (commented.find()) off.add(commented.group(1));
        return helpers;
    }

    /** Where each listed Mixin handler ended up: named in an injection that is on, in one that is off, or nowhere. */
    private static Map<String, Handler> handlers(Analysis analysis, Map<String, Handler> theirs, String manifest, Path mod, Map<String, String> helpers,
                                                 Map<String, String> turnedOff) throws IOException {
        Map<String, String> helperText = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : helpers.entrySet()) helperText.put(e.getKey(), read(mod.resolve(e.getValue())));
        String[] lines = manifest.split("\n");
        Map<String, Handler> out = new LinkedHashMap<>();
        for (MixinClass m : analysis.mixins()) {
            if (m.config() == null) continue;
            for (Injector i : m.injectors()) {
                String key = m.className() + "." + i.handler();
                Pattern named = Pattern.compile("(?<![\\w$.])" + Pattern.quote(ClassData.simple(m.className()) + "." + i.handler()) + "(?![\\w$])");
                String state = "absent", injection = null;
                for (int n = 0; n < lines.length && injection == null; n++) {
                    if (!lines[n].startsWith("# Off:") || !named.matcher(lines[n]).find()) continue;
                    state = "off";
                    for (int k = n + 1; k < lines.length && lines[k].startsWith("#"); k++) {
                        Matcher id = Pattern.compile("^# id = \"([^\"]+)\"$").matcher(lines[k]);
                        if (id.matches()) {
                            injection = id.group(1);
                            break;
                        }
                    }
                    if (injection == null) injection = "?";
                }
                if (state.equals("absent")) {
                    for (Map.Entry<String, String> e : helperText.entrySet()) {
                        if (!named.matcher(e.getValue()).find()) continue;
                        state = "on";
                        injection = e.getKey();
                        break;
                    }
                }
                if (state.equals("absent")) {
                    for (Map.Entry<String, String> e : turnedOff.entrySet()) {
                        if (!named.matcher(e.getValue()).find()) continue;
                        state = "off";
                        injection = e.getKey();
                        break;
                    }
                }
                Handler said = theirs.get(key);
                out.put(key, said == null ? new Handler(null, null, null, state, injection)
                        : new Handler(said.status(), said.horizon(), said.note(), state, injection));
            }
        }
        return out;
    }

    // ---- PORTING.md ----

    static String porting(String theirs, ModInfo info, Analysis analysis, Outcome o, Path mod, Path dir, boolean packed, List<String> hooksOff)
            throws IOException {
        // Sections by their `## ` line; the first holds what comes before any.
        List<String[]> sections = new ArrayList<>();
        StringBuilder body = new StringBuilder();
        String heading = "";
        for (String line : theirs.split("\n", -1)) {
            if (line.startsWith("## ")) {
                sections.add(new String[] {heading, body.toString()});
                heading = line;
                body.setLength(0);
            } else {
                body.append(line).append('\n');
            }
        }
        body.setLength(Math.max(0, body.length() - 1));
        sections.add(new String[] {heading, body.toString()});

        String counts = o.on().size() + (o.on().size() == 1 ? " injection is on; " : " injections are on; ") + o.off().size()
                + (o.off().size() == 1 ? " is" : " are") + " written commented out";
        sections.get(0)[1] = COUNTS.matcher(sections.get(0)[1]).replaceFirst(Matcher.quoteReplacement(counts));

        List<String> left = left(analysis, o);
        int build = -1;
        for (int i = 0; i < sections.size(); i++) {
            String[] s = sections.get(i);
            if (s[0].equals("## What each part became") && !hooksOff.isEmpty()) {
                s[1] = hookRows(s[1], hooksOff);
            } else if (s[0].equals("## Build it")) {
                build = i;
                // The launcher takes the folder that holds horizon-mixin.toml (its `bundles`, launcher/src/native_mods.rs), not the one above mixins/.
                s[1] = s[1].replace("native-build \"<this folder>\"", "native-build \"<this folder>/mixins/" + o.modId() + "\" --instance <name>");
            } else if (s[0].equals("## Ready to use?")) {
                s[1] = ready(s[1], o, left, mod, dir, packed);
            } else if (s[0].equals("## Licence")) {
                s[1] = LicenceCheck.named(s[1], info);
            }
        }
        String[] ours = {SECTION, section(info, analysis, o, left)};
        if (build >= 0) sections.add(build, ours);
        else sections.add(ours);

        StringBuilder out = new StringBuilder();
        for (String[] s : sections) {
            if (!s[0].isEmpty()) out.append(s[0]).append('\n');
            out.append(s[1]);
            if (s == ours && build >= 0) out.append('\n');
        }
        if (build < 0 && out.charAt(out.length() - 1) != '\n') out.append('\n');
        return out.toString();
    }

    /** The table's rows for the hook points this turned off, saying they are off now. */
    private static String hookRows(String table, List<String> hooksOff) {
        List<String> lines = new ArrayList<>(List.of(table.split("\n", -1)));
        for (int i = 0; i < lines.size(); i++) {
            for (String hook : hooksOff) {
                String start = "| hook `" + hook + "` | on | ";
                if (lines.get(i).startsWith(start)) {
                    lines.set(i, "| hook `" + hook + "` | off | Commented out by horizon-port-neoforge: see \"From a NeoForge mod\" below. |");
                }
            }
        }
        return String.join("\n", lines);
    }

    /** What a NeoForge mod does that horizon-port doesn't see, so that isn't in the bundle. */
    private static List<String> left(Analysis analysis, Outcome o) {
        List<String> left = new ArrayList<>();
        long listeners = analysis.listeners().stream().filter(Analysis.Listener::registered).count();
        long on = o.listenersOn();
        if (on > 0) {
            left.add(on + (on == 1 ? " event listener is" : " event listeners are") + " on " + (on == 1 ? "a hook point" : "hook points")
                    + ", but " + (on == 1 ? "its handler isn't" : "their handlers aren't") + " ported: each helper has a TODO for it.");
        }
        if (listeners - on > 0) {
            left.add((listeners - on) + (listeners - on == 1 ? " event listener isn't" : " event listeners aren't") + " on a hook point"
                    + (o.listeners().isEmpty() ? ": the Horizon source given has no hook catalogue (`native-hooks.json`) to go by."
                            : ": each is written commented out in `horizon-mixin.toml` with why (the table under \"From a NeoForge mod\")."));
        }
        int registered = analysis.registries().stream().mapToInt(r -> r.entries().size()).sum();
        Content.Result content = o.content();
        long declared = content == null ? 0 : content.declared().stream().filter(d -> Boolean.TRUE.equals(d.registered())).count();
        if (registered > 0 && content != null && content.packed() && declared > 0) {
            left.add(registered + (registered == 1 ? " thing is" : " things are") + " registered with `DeferredRegister`: " + declared + (declared == 1 ? " is" : " are")
                    + " declared in `" + content.file() + "`, copying a vanilla template to check; the rest " + (registered - declared == 1 ? "isn't" : "aren't")
                    + " in the bundle (`report/report.md`).");
        } else if (registered > 0) {
            left.add(registered + (registered == 1 ? " thing" : " things") + " registered with `DeferredRegister` "
                    + (registered == 1 ? "isn't" : "aren't") + " in the bundle (`report/report.md`).");
        }
        if (content != null && !content.packed() && !content.state().equals("none")) {
            left.add(switch (content.state()) {
                case "todo" -> "The content mod isn't packed: its folder is `" + content.folder() + "/`, with declarations to check first.";
                case "no-tools" -> "The jar's data and assets aren't carried over into a content mod: Horizon's tools for it aren't built.";
                default -> "No `.hmod` was made of the jar's data and assets: " + content.why() + ".";
            });
        }
        if (!analysis.configs().isEmpty()) {
            left.add(analysis.configs().size() + (analysis.configs().size() == 1 ? " config" : " configs") + " registered with NeoForge "
                    + (analysis.configs().size() == 1 ? "isn't" : "aren't") + " read by the bundle.");
        }
        long absent = o.handlers().values().stream().filter(h -> h.state().equals("absent")).count();
        if (absent > 0) {
            left.add(absent + (absent == 1 ? " Mixin handler isn't" : " Mixin handlers aren't") + " in the bundle at all (`report/report.md` says which).");
        }
        return left;
    }

    private static String section(ModInfo info, Analysis analysis, Outcome o, List<String> left) {
        StringBuilder b = new StringBuilder("\n");
        b.append(info.name()).append(" is a NeoForge mod. horizon-port-neoforge handed horizon-port a copy of its jar, and horizon-port did what it does ")
                .append("for any mod: it matched the Mixins against Horizon and translated the Java that doesn't need the game. ")
                .append("It knows nothing of NeoForge, so everything above and below is about that part only.\n\n");
        if (left.isEmpty()) {
            b.append("Nothing of the mod's NeoForge side is known to be left out.\n");
        } else {
            b.append("Not in the bundle:\n\n");
            for (String l : left) b.append("- ").append(l).append('\n');
        }
        if (!o.changes().isEmpty()) {
            b.append("\nChanged after horizon-port wrote the bundle:\n\n");
            for (String c : o.changes()) b.append("- ").append(c).append('\n');
        }
        if (o.content() != null) {
            b.append('\n');
            for (String line : Content.words(o.content())) b.append(line).append('\n');
        }
        if (o.machines() != null) {
            b.append('\n');
            for (String line : Machines.words(o.machines())) b.append(line).append('\n');
        }
        if (o.listeners().stream().anyMatch(w -> !w.state().equals("none"))) {
            b.append("\nThe mod's event listeners, put on Horizon's hook points from its hook catalogue (`native-hooks.json`). One is on only when the ")
                    .append("catalogue names a single hook point for its event, with nothing more to read; its helper does nothing until its `TODO` is filled in:\n\n")
                    .append("| Listener | NeoForge event | Hook point | State | Why |\n|---|---|---|---|---|\n");
            for (Wiring.Wired w : o.listeners()) {
                if (w.state().equals("none")) continue;
                b.append("| `").append(Report.handler(w.listener())).append("` | ").append(Wiring.eventName(w.listener().event())).append(" | ")
                        .append(w.hook() == null ? "" : "`" + w.hook() + "`").append(" | ")
                        .append(w.state().equals("on") ? "on, in `" + w.injection() + "`" : "off, as `" + w.injection() + "`").append(" | ")
                        .append(Character.toUpperCase(w.why().charAt(0))).append(w.why().substring(1).replace("|", "\\|")).append(w.why().endsWith(".") ? "" : ".").append(" |\n");
            }
        }
        b.append("\n`report/report.md` is the mod as NeoForge sees it (entry points, listeners, registries, Mixins) with what became of each part; ")
                .append("`report/horizon-port-report.md` is horizon-port's own report, on the copy.\n")
                .append("\nTo go over this folder against a Horizon source, by reading: `horizon-port-neoforge check <this folder> --horizon-src <source>`. ")
                .append("To have Horizon's launcher build it as it is, TODOs and all: `horizon-port-neoforge pack <this folder> --horizon-src <source>` makes `")
                .append(o.modId()).append(".hnmod`. Built, it changes nothing in the game until the TODOs are filled in.\n");
        return b.toString();
    }

    /** The "Ready to use?" section, with what horizon-port doesn't see added to what is left. */
    private static String ready(String theirs, Outcome o, List<String> left, Path mod, Path dir, boolean packed) throws IOException {
        if (theirs.contains(FULL) || packed) {
            StringBuilder b = new StringBuilder("\n**Not packed.** horizon-port saw nothing left to finish and packed `" + o.modId()
                    + ".hnmod`. That file was removed: it was packed before this bundle was gone over for a NeoForge mod, "
                    + "and horizon-port doesn't see what such a mod does outside its Mixins. ");
            if (left.isEmpty()) {
                b.append("Nothing else is known to be left.");
            } else {
                b.append("What's left:\n");
                for (String l : left) b.append("\n- ").append(l);
            }
            // As many line ends after it as there were, so the next section keeps its blank line.
            return b + theirs.substring(theirs.stripTrailing().length());
        }
        List<String> lines = new ArrayList<>(List.of(theirs.split("\n", -1)));
        // What horizon-port says a content mod is needed for, now that there is an answer.
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            if (o.content() == null || !line.startsWith("- ") || !line.contains("content mod (.hmod)")) continue;
            if (o.content().packed()) {
                lines.set(i, line.replace(", which this tool doesn't make.", ": `" + o.content().file() + "` (see \"From a NeoForge mod\").")
                        .replace(": that needs a content mod (.hmod).", ": that is for a content mod; see `" + o.content().file() + "` under \"From a NeoForge mod\"."));
            } else {
                lines.set(i, line.replace(", which this tool doesn't make.", ", which isn't made (see \"From a NeoForge mod\")."));
            }
        }
        // The TODOs are counted again: this tool moved some and wrote others.
        List<String> todos = todos(mod, dir);
        boolean listed = false;
        for (int i = 0; i < lines.size() && !listed; i++) {
            if (!TODO_HEAD.matcher(lines.get(i)).matches()) continue;
            int end = i + 1;
            while (end < lines.size() && lines.get(end).startsWith("  - ")) end++;
            lines.subList(i, end).clear();
            lines.addAll(i, todos);
            listed = true;
        }
        int last = lines.size();
        while (last > 0 && lines.get(last - 1).isBlank()) last--;
        List<String> added = new ArrayList<>();
        if (!listed) added.addAll(todos);
        for (String l : left) added.add("- " + l);
        lines.addAll(last, added);
        return String.join("\n", lines);
    }

    /** Every TODO in the mod's folder, as horizon-port lists them: file, line, the line itself. */
    private static List<String> todos(Path mod, Path dir) throws IOException {
        List<String> found = new ArrayList<>();
        for (Path f : files(mod)) {
            if (!text(f)) continue;
            String[] lines = read(f).split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].contains("TODO")) found.add("  - `" + dir.relativize(f).toString().replace('\\', '/') + ":" + (i + 1) + "` " + lines[i].strip());
            }
        }
        if (found.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        out.add("- " + found.size() + (found.size() == 1 ? " TODO is" : " TODOs are") + " left in the code:");
        out.addAll(found.subList(0, Math.min(12, found.size())));
        if (found.size() > 12) out.add("  - ... and " + (found.size() - 12) + " more");
        return out;
    }

    // ---- files ----

    /** Every file under a folder, in path order. */
    static List<Path> files(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile).sorted().toList();
        }
    }

    private static boolean text(Path file) {
        String name = file.getFileName().toString();
        return name.endsWith(".rs") || name.endsWith(".toml") || name.endsWith(".md");
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
