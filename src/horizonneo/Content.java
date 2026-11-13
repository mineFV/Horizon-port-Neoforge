package horizonneo;

import horizonneo.Analysis.Registration;
import horizonneo.Analysis.Registry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * The mod's declarative content (its data and assets) as a Horizon content mod, a `.hmod`.
 *
 * Both steps are Horizon's own, run unchanged (`HorizonTools`): `horizon-port-pack check` carries
 * the jar's `assets/` and `data/` over into a folder content mod, upgrading what the game's version
 * changed, and `cargo horizon-mod build` packs that folder with the checks the game makes when it
 * reads content. Nothing of the mod's code is read for it but names.
 *
 * One thing in a carry is a guess, and Horizon says so: without NeoForge running, it declares a
 * block for each blockstate file, copying the vanilla block whose properties look closest (a block
 * with none copies stone), and an item for each item model, copying the stick. The mod's own
 * hardness, sounds, shapes and stack sizes are in its code. So a carry that declares anything is
 * packed only when the user asked for approximate answers too; otherwise its folder is written
 * under `todo/`, with every declaration listed, for a person to check and pack.
 */
final class Content {
    private Content() {}

    /**
     * Something the content mod declares. kind: `block`, `item`... id: `namespace:name`.
     * template: the vanilla thing it copies. registered: the mod's code registers that id.
     */
    record Declared(String kind, String id, String template, boolean registered) {}

    /**
     * What became of the content. state: "packed" (file is the `.hmod`, folder its source),
     * "todo" (folder only, not packed), "none" (nothing to carry), "failed" (a tool refused),
     * "no-tools" (Horizon's tools aren't built). file, folder: paths in the bundle, null when
     * not written. summary, ports, skipped: Horizon's own words on the jar. undeclared: blocks
     * and items the mod registers that the carry didn't declare. leftOut: files the carry kept
     * out, each with why, as paths in the bundle. why: what state it is and why, in words.
     */
    record Result(String state, String file, String folder, int files, String summary, List<String> ports, List<String> skipped,
                  List<Declared> declared, List<String> undeclared, Map<String, String> leftOut, String why) {
        Result {
            ports = List.copyOf(ports);
            skipped = List.copyOf(skipped);
            declared = List.copyOf(declared);
            undeclared = List.copyOf(undeclared);
        }

        boolean packed() {
            return state.equals("packed");
        }
    }

    static final String FOLDER = "content", TODO_FOLDER = "todo/content", LEFT_OUT = "todo/content-left-out";

    private static final int SECONDS = 15 * 60;

    /**
     * Carries the jar's content over and writes it into the bundle. work: an empty temporary
     * folder. approximate: pack a carry that declares blocks or items too.
     */
    static Result carry(ModJar jar, Analysis analysis, HorizonTools tools, Path work, Path bundle, boolean approximate) throws IOException {
        if (analysis.census().assets() + analysis.census().data() == 0) {
            return none("none", null, "the jar holds no `assets/` and no `data/`: there is nothing to carry");
        }
        if (tools == null) {
            return none("no-tools", null, "Horizon's tools for it aren't built: " + HorizonTools.HOW + " (or name their folder with --horizon-tools)");
        }
        Path out = work.resolve("carried");
        HorizonTools.Run check = tools.portPack(List.of("check", jar.path().toAbsolutePath().toString(), out.toString()), SECONDS);
        if (check.exit() != 0) return none("failed", null, "horizon-port-pack refused the jar: " + last(check.err().isBlank() ? check.out() : check.err()));

        String summary = null;
        List<String> ports = new ArrayList<>(), skipped = new ArrayList<>();
        Map<String, Object> report = Map.of();
        try {
            if (Files.isRegularFile(out.resolve("check.json"))) {
                Map<String, Object> said = Json.object(Json.parse(Files.readString(out.resolve("check.json"), StandardCharsets.UTF_8)));
                summary = Json.text(said, "summary");
                for (Object p : Json.array(said.get("ports"))) if (p instanceof String s) ports.add(s);
                for (Object s : Json.array(said.get("skipped"))) if (s instanceof String t) skipped.add(t);
            }
            if (Files.isRegularFile(out.resolve("carry-report.json"))) {
                report = Json.object(Json.parse(Files.readString(out.resolve("carry-report.json"), StandardCharsets.UTF_8)));
            }
        } catch (Json.Invalid e) {
            return none("failed", summary, "what horizon-port-pack wrote about the jar can't be read (" + e.getMessage() + ")");
        }

        Path carried = out.resolve("carry");
        List<Path> files = Files.isDirectory(carried) ? files(carried) : List.of();
        int count = (int) files.stream().filter(f -> !f.equals(carried.resolve("horizon-mod.toml"))).count();
        Map<String, String> leftOut = leftOut(out.resolve("outside"), report, bundle);
        if (count == 0) {
            return new Result("none", null, null, 0, summary, ports, skipped, List.of(), List.of(), leftOut,
                    summary != null && !Files.isDirectory(carried) ? "horizon-port-pack carried nothing over: " + summary
                            : "horizon-port-pack found nothing of it that a content mod holds");
        }

        List<Declared> declared = declared(carried, analysis);
        List<String> undeclared = undeclared(declared, analysis);
        String guessed = declared.isEmpty() ? null : guessed(declared);
        if (guessed != null && !approximate) {
            copy(carried, bundle.resolve(TODO_FOLDER));
            return new Result("todo", null, TODO_FOLDER, count, summary, ports, skipped, declared, undeclared, leftOut,
                    "not packed: it declares " + guessed + ", each copying a vanilla one that is Horizon's guess. `--allow-approximate` packs it as it is");
        }

        HorizonTools.Run build = tools.horizonMod(List.of("build"), carried, SECONDS);
        Path packed = null;
        Path target = carried.resolve("target").resolve("horizon-mod");
        if (build.exit() == 0 && Files.isDirectory(target)) {
            try (Stream<Path> s = Files.list(target)) {
                List<Path> made = s.filter(f -> f.getFileName().toString().endsWith(".hmod")).toList();
                if (made.size() == 1) packed = made.get(0);
            }
        }
        String refused = packed == null ? "cargo horizon-mod build refused it: " + last(build.err().isBlank() ? build.out() : build.err()) : null;
        if (packed != null) {
            HorizonTools.Run checked = tools.horizonMod(List.of("check", packed.toString()), carried, SECONDS);
            if (checked.exit() != 0) refused = "cargo horizon-mod check refused the file: " + last(checked.err().isBlank() ? checked.out() : checked.err());
        }
        if (refused != null) {
            delete(carried.resolve("target"));
            copy(carried, bundle.resolve(TODO_FOLDER));
            return new Result("failed", null, TODO_FOLDER, count, summary, ports, skipped, declared, undeclared, leftOut, refused);
        }
        String name = packed.getFileName().toString();
        Files.copy(packed, bundle.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        delete(carried.resolve("target"));
        copy(carried, bundle.resolve(FOLDER));
        return new Result("packed", name, FOLDER, count, summary, ports, skipped, declared, undeclared, leftOut,
                "packed and checked by `cargo horizon-mod`" + (guessed == null ? "; it declares no block or item, so nothing in it is a guess"
                        : "; it declares " + guessed + ", each copying a vanilla one that is Horizon's guess, as `--allow-approximate` asked"));
    }

    private static Result none(String state, String summary, String why) {
        return new Result(state, null, null, 0, summary, List.of(), List.of(), List.of(), List.of(), Map.of(), why);
    }

    // ---- what a carry declares ----

    /** The blocks, items and the like under `data/<namespace>/horizon/<kind>/`, in path order. */
    static List<Declared> declared(Path carried, Analysis analysis) throws IOException {
        List<Declared> out = new ArrayList<>();
        Path data = carried.resolve("data");
        if (!Files.isDirectory(data)) return out;
        List<String> registered = registered(analysis);
        for (Path f : files(data)) {
            Path rel = data.relativize(f);
            if (rel.getNameCount() < 4 || !rel.getName(1).toString().equals("horizon") || !f.getFileName().toString().endsWith(".json")) continue;
            String kind = rel.getName(2).toString();
            String path = rel.subpath(3, rel.getNameCount()).toString().replace('\\', '/');
            String id = rel.getName(0) + ":" + path.substring(0, path.length() - ".json".length());
            String template = null;
            try {
                template = Json.text(Json.object(Json.parse(Files.readString(f, StandardCharsets.UTF_8))), "template");
            } catch (Json.Invalid e) {
                // Left without a template: the list still names it.
            }
            out.add(new Declared(kind, id, template, registered.contains(kind + " " + id)));
        }
        return out;
    }

    /** Every block and item the mod's code registers, as `block namespace:name`. */
    private static List<String> registered(Analysis analysis) {
        List<String> out = new ArrayList<>();
        for (Registry r : analysis.registries()) {
            String kind = "BLOCK".equals(r.kind()) ? "block" : "ITEM".equals(r.kind()) ? "item" : null;
            if (kind == null || r.namespace() == null) continue;
            for (Registration e : r.entries()) if (e.name() != null) out.add(kind + " " + r.namespace() + ":" + e.name());
        }
        return out;
    }

    /** The blocks and items the mod registers that the carry doesn't declare. A block's own item goes with its block. */
    static List<String> undeclared(List<Declared> declared, Analysis analysis) {
        List<String> have = declared.stream().map(d -> d.kind() + " " + d.id()).toList();
        List<String> out = new ArrayList<>();
        for (String r : registered(analysis)) {
            if (have.contains(r)) continue;
            if (r.startsWith("item ") && have.contains("block " + r.substring("item ".length()))) continue;
            out.add(r);
        }
        return out;
    }

    /** "2 blocks and 1 item". */
    private static String guessed(List<Declared> declared) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Declared d : declared) counts.merge(d.kind(), 1, Integer::sum);
        List<String> parts = new ArrayList<>();
        counts.forEach((kind, n) -> parts.add(n + " " + kind.replace('_', ' ') + (n == 1 ? "" : "s")));
        return parts.size() <= 1 ? String.join("", parts) : String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
    }

    // ---- what a carry keeps out ----

    /** Moves the files the carry kept out into the bundle's todo folder, and says why each was. */
    private static Map<String, String> leftOut(Path outside, Map<String, Object> report, Path bundle) throws IOException {
        Map<String, String> out = new TreeMap<>();
        if (!Files.isDirectory(outside)) return out;
        Map<String, String> reasons = new LinkedHashMap<>();
        for (Object f : Json.array(report.get("recipes_unregistered"))) {
            reasons.put(String.valueOf(f), "a recipe whose result is an item of the mod's that the content mod doesn't declare: the game refuses a recipe for an item it doesn't know");
        }
        for (Object f : Json.array(report.get("recipes_off"))) reasons.put(String.valueOf(f), "a recipe whose NeoForge conditions don't pass");
        Json.object(report.get("unreadable")).forEach((f, why) -> reasons.put(f, "a file the game can't read: " + why));
        for (Object f : Json.array(report.get("outside"))) {
            reasons.put(String.valueOf(f), "outside what a content mod may hold: another mod's namespace, or vanilla data that isn't a tag");
        }
        for (Path f : files(outside)) {
            String name = outside.relativize(f).toString().replace('\\', '/');
            Path to = bundle.resolve(LEFT_OUT).resolve(name);
            Files.createDirectories(to.getParent());
            Files.copy(f, to, StandardCopyOption.REPLACE_EXISTING);
            out.put(LEFT_OUT + "/" + name, reasons.getOrDefault(name, "kept out by horizon-port-pack"));
        }
        return out;
    }

    // ---- in words ----

    /** The lines of PORTING.md's section on the content. */
    static List<String> words(Result r) {
        List<String> out = new ArrayList<>();
        switch (r.state()) {
            case "none" -> out.add("No content mod was made: " + r.why() + ".");
            case "no-tools" -> out.add("**No content mod was made**: " + r.why() + ". The jar's data and assets are still to carry over.");
            case "failed" -> out.add("**No `.hmod` was made**: " + r.why() + "." + (r.folder() == null ? "" : " The folder it was to be packed from is `" + r.folder() + "/`."));
            case "todo" -> {
                out.add("The mod's data and assets were carried over by Horizon's `horizon-port-pack` into the folder content mod `" + r.folder() + "/` ("
                        + r.files() + (r.files() == 1 ? " file" : " files") + "), **" + r.why() + "**.");
                out.add("");
                out.add("TODO: check each declaration below against the mod (its hardness, sounds, shape and stack size are in its code), then pack the folder: "
                        + "`cargo horizon-mod build` in it.");
            }
            default -> out.add("The mod's data and assets are in **`" + r.file() + "`**, a content mod (" + r.files() + (r.files() == 1 ? " file" : " files")
                    + "; its folder is `" + r.folder() + "/`): carried over by Horizon's `horizon-port-pack`, " + r.why()
                    + ". Put it in `horizon-mods/` in Horizon's game folder and switch it on in Options > Mods. It has the native bundle's id: Horizon can "
                    + "also take a mod's content and code as one `.hmod`, which this tool doesn't make, and whether it takes the two side by side wasn't tried.");
        }
        if (r.summary() != null && (r.packed() || r.state().equals("todo"))) {
            out.add("");
            out.add("What `horizon-port-pack` says of the jar: " + r.summary() + (r.ports().isEmpty() ? "" : " It carries: " + String.join("; ", r.ports()) + "."));
        }
        if (!r.declared().isEmpty()) {
            out.add("");
            out.add("| Declared | Copies | Registered by the mod's code |");
            out.add("|---|---|---|");
            for (Declared d : r.declared()) {
                out.add("| " + d.kind() + " `" + d.id() + "` | " + (d.template() == null ? "not read" : "`" + d.template() + "`") + " | "
                        + (d.registered() ? "yes" : "**no**: the carry declares it from an asset, the code doesn't register it") + " |");
            }
        }
        if (!r.undeclared().isEmpty() && (r.packed() || r.state().equals("todo"))) {
            out.add("");
            out.add("Registered by the mod's code, but not declared in the content mod (the jar has no blockstate or item model for them): "
                    + String.join(", ", r.undeclared().stream().map(u -> u.substring(0, u.indexOf(' ')) + " `" + u.substring(u.indexOf(' ') + 1) + "`").toList()) + ".");
        }
        if (!r.leftOut().isEmpty()) {
            out.add("");
            out.add("Kept out of the content mod by the carry:");
            out.add("");
            r.leftOut().forEach((file, why) -> out.add("- `" + file + "`: " + why + "."));
        }
        return out;
    }

    // ---- files ----

    private static List<Path> files(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile).sorted().toList();
        }
    }

    private static void copy(Path from, Path to) throws IOException {
        for (Path f : files(from)) {
            Path target = to.resolve(from.relativize(f).toString());
            Files.createDirectories(target.getParent());
            Files.copy(f, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void delete(Path dir) {
        Port.delete(dir);
    }

    /** The last line a program printed, for a message. */
    private static String last(String text) {
        String[] lines = text.strip().split("\n");
        String line = lines[lines.length - 1].strip();
        return line.isEmpty() ? "it said nothing" : line.replaceFirst("^error: ", "");
    }
}
