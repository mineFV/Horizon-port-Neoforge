package horizonneo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * `pack`: a bundle as `<mod id>.hnmod`, the file Horizon's launcher installs and builds into the
 * game. The file is made as Horizon's own documentation says it is read (docs/api/mod-files.md in
 * its source, `horizon_mods::index::native`): `horizon-native.json`, then the bundle's files under
 * `mixins/<id>/`, with the Horizon commit pinned in `horizon-mixin.toml`.
 *
 * horizon-port packs a bundle only once it is a finished port. This packs one that isn't: what is
 * commented out stays out and each TODO stays a TODO, so built into the game it changes nothing
 * yet. That is what makes it worth building: the launcher's build applies it to Horizon itself and
 * compiles it there, which no check here does.
 *
 * Nothing is compiled, and nothing is installed: building native code is the player's to decide.
 */
final class Pack {
    /** horizonSrc: Horizon's source. translator, tools: where to find them, null to look. out: the file to write, null for the bundle's own folder. */
    record Options(Path horizonSrc, String translator, String tools, Path out) {}

    /** Horizon's limits for a native file: its entries, and their size unpacked. */
    static final int MAX_FILES = 512;
    static final long MAX_BYTES = 64L << 20;

    private static final String NATIVE_MANIFEST = "horizon-native.json", MANIFEST = "horizon-mixin.toml";
    private static final Pattern COMMIT = Pattern.compile("[0-9a-f]{40}");
    private static final Pattern TABLE = Pattern.compile("(?m)^\\[\\[");

    private Pack() {}

    static int run(Path bundle, Options o, PrintStream out, PrintStream err) throws IOException {
        Check.Facts facts = Check.read(bundle, new Check.Options(o.horizonSrc(), o.translator(), o.tools(), false), out);
        if (facts == null || facts.problems() > 0) {
            out.println("Not packed: " + (facts == null ? "it isn't a bundle that can be read" : count(facts.problems(), "problem") + " above") + ".");
            return Main.FAILED;
        }
        Path mod = facts.mod();
        String id = facts.id();
        Map<String, Object> root;
        String manifest = Files.readString(mod.resolve(MANIFEST), StandardCharsets.UTF_8);
        Map<String, Object> compat;
        try {
            root = Toml.parse(manifest).root();
            compat = Json.object(Json.parse(Files.readString(bundle.resolve("compatibility.json"), StandardCharsets.UTF_8)));
        } catch (Toml.Problem | Json.Invalid e) {
            throw new IOException("the bundle changed while it was read: " + e.getMessage());
        }
        String version = Toml.text(Toml.table(root.get("mod")), "version"), game = Json.text(compat, "game");
        String horizon = Json.text(compat, "horizon_revision"), mixins = Json.text(compat, "horizon_mixins_revision");

        // What Horizon refuses in a native file, said before anything is written.
        List<String> why = new ArrayList<>();
        if (!id.matches("[a-z0-9_]{1,64}")) why.add("the mod id `" + id + "` isn't one Horizon takes for a native mod: a-z, 0-9 and _, 64 characters at most");
        if (version == null || version.isEmpty()) why.add(MANIFEST + " has no [mod] version");
        if (game == null || game.isEmpty()) why.add("compatibility.json names no game version (`game`)");
        if (horizon == null || !COMMIT.matcher(horizon).matches()) {
            why.add("the bundle names no Horizon commit (compatibility.json, `horizon_revision`): a native mod builds on one. Convert the mod again with "
                    + "--horizon-revision, or against a source that has a .horizon-revision");
        }
        if (mixins == null || !COMMIT.matcher(mixins).matches()) why.add("the bundle names no horizon-mixins commit (compatibility.json, `horizon_mixins_revision`)");
        String pinned = horizon == null ? null : pinned(manifest, root, horizon, why);
        Map<String, Path> files = files(mod, why);
        if (!why.isEmpty()) {
            out.println("Not packed:");
            for (String w : why) out.println("  - " + w + ".");
            return Main.FAILED;
        }

        // The hook points' version instead of one commit only, when the hook points are all it touches.
        Integer hooks = null;
        String onlyHere = null;
        List<String> targets = new ArrayList<>();
        for (Map<String, Object> injection : Toml.tables(root.get("injection"))) targets.add(String.valueOf(Toml.text(injection, "target")));
        if (facts.hookTargets() == null) {
            onlyHere = "horizon-port's list of hook points couldn't be had, so nothing says that hook points are all it touches";
        } else if (!facts.hookTargets().containsAll(targets)) {
            onlyHere = "it goes into functions of Horizon that aren't hook points (" + targets.stream().filter(t -> !facts.hookTargets().contains(t)).distinct().count()
                    + "), which another commit may not have";
        } else if (compat.get("hooks") instanceof Number n && n.intValue() >= 1) {
            hooks = n.intValue();
        } else {
            onlyHere = "compatibility.json names no hooks API";
        }

        Map<String, Object> described = new LinkedHashMap<>();
        described.put("schema", 1);
        described.put("id", id);
        described.put("version", version);
        described.put("game", game);
        described.put("horizon", horizon);
        described.put("mixins", mixins);
        if (hooks != null) described.put("hooks", hooks);

        Path target = (o.out() != null ? o.out() : bundle.resolve(id + ".hnmod")).toAbsolutePath();
        if (Files.isDirectory(target)) throw new IOException("--out: " + target + " is a folder; it takes the file to write");
        if (target.getParent() != null) Files.createDirectories(target.getParent());
        Path part = target.resolveSibling(target.getFileName() + ".part");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(part))) {
                put(zip, NATIVE_MANIFEST, Json.write(described).getBytes(StandardCharsets.UTF_8));
                for (Map.Entry<String, Path> f : files.entrySet()) {
                    byte[] bytes = f.getKey().equals(MANIFEST) ? pinned.getBytes(StandardCharsets.UTF_8) : Files.readAllBytes(f.getValue());
                    put(zip, "mixins/" + id + "/" + f.getKey(), bytes);
                }
            }
            String refused = horizonCheck(part, target, o, out);
            if (refused != null) {
                out.println("Not packed: Horizon's own check refuses the file: " + refused + ".");
                return Main.FAILED;
            }
            Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(part);
        }

        out.println("Packed " + target + ": " + count(files.size(), "file") + " of mixins/" + id + "/, for Horizon " + horizon + ".");
        out.println(hooks != null
                ? "  It touches hook points only (API " + hooks + "): the launcher also builds it on a later Horizon that has that API."
                : "  It builds on that commit only: " + onlyHere + ".");
        out.println("  It isn't a finished port: " + count(facts.todos(), "TODO") + " in the code that is on, " + count(facts.off(), "injection")
                + " commented out. Built into the game it changes nothing yet; what a build tells is whether Horizon takes it and compiles it.");
        out.println("To try it: put the file in the instance's horizon-native-mods/ folder and press Build on the launcher's Mods page, or");
        out.println("  horizon-launcher native-build \"" + bundle.toAbsolutePath() + "\" --instance <name> --trust-native-code");
        out.println("Native code isn't sandboxed: that build is yours to start, once you have read the code. Nothing was compiled or installed here.");
        return Main.DONE;
    }

    /**
     * The manifest with `[compatibility] horizon_revision` as Horizon wants it in a native file: the
     * commit `horizon-native.json` names. Added after `[mod]` when the bundle's manifest has none.
     */
    static String pinned(String manifest, Map<String, Object> root, String horizon, List<String> why) {
        Object compatibility = root.get("compatibility");
        if (compatibility == null) {
            String table = "[compatibility]\nhorizon_revision = \"" + horizon + "\"\n\n";
            Matcher first = TABLE.matcher(manifest);
            return first.find() ? manifest.substring(0, first.start()) + table + manifest.substring(first.start())
                    : manifest + (manifest.endsWith("\n") ? "\n" : "\n\n") + table;
        }
        String named = Toml.text(Toml.table(compatibility), "horizon_revision");
        if (named == null) {
            why.add(MANIFEST + " has a [compatibility] table without `horizon_revision`: add `horizon_revision = \"" + horizon + "\"` to it");
        } else if (!named.equals(horizon)) {
            why.add(MANIFEST + " pins Horizon " + named + " and compatibility.json names " + horizon + ": a native file names one commit");
        }
        return manifest;
    }

    /** The files of mixins/<id>/ by their path in it, in path order; what Horizon doesn't take in a native file goes to `why`. */
    private static Map<String, Path> files(Path mod, List<String> why) throws IOException {
        Map<String, Path> files = new TreeMap<>();
        List<String> refused = new ArrayList<>();
        long bytes = 0;
        List<Path> all;
        try (Stream<Path> s = Files.walk(mod)) {
            all = s.filter(p -> !Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)).sorted().toList();
        }
        for (Path file : all) {
            String rest = mod.relativize(file).toString().replace('\\', '/');
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || rest.contains(":") || !allowed(rest)) {
                refused.add(rest);
                continue;
            }
            files.put(rest, file);
            bytes += Files.size(file);
        }
        if (!refused.isEmpty()) {
            why.add("mixins/" + mod.getFileName() + "/ holds " + count(refused.size(), "file") + " Horizon doesn't take in a native file (" + String.join(", ", refused.subList(0, Math.min(5, refused.size())))
                    + (refused.size() > 5 ? ", ..." : "") + "): only .rs and .toml files, files under data/, and README, LICENSE, COPYING, NOTICE and .gitignore");
        }
        // The native manifest is one of the entries Horizon counts.
        if (files.size() + 1 > MAX_FILES) why.add("the bundle has " + files.size() + " files: a native file holds " + (MAX_FILES - 1) + " at most beside its manifest");
        if (bytes > MAX_BYTES) why.add("the bundle's files take " + (bytes >> 20) + " MiB: a native file holds 64 MiB at most");
        return files;
    }

    /**
     * Whether Horizon takes that file of a bundle (its path under mixins/<id>/) in a native file:
     * sources and manifests, files under the bundle's `data/` or a top-level folder's, and the text
     * files of a crate folder.
     */
    static boolean allowed(String rest) {
        String[] parts = rest.split("/", -1);
        for (String part : parts) if (part.isEmpty() || part.equals(".") || part.equals("..")) return false;
        if (rest.endsWith(".rs") || rest.endsWith(".toml")) return true;
        if (parts.length >= 2 && (parts[0].equals("data") || (parts.length >= 3 && parts[1].equals("data")))) return true;
        String name = parts[parts.length - 1];
        return parts.length <= 2 && (name.equals(".gitignore") || Stream.of("README", "LICENSE", "LICENCE", "COPYING", "NOTICE").anyMatch(name::startsWith));
    }

    /**
     * Horizon's own check of the file (`cargo horizon-mod check`, which reads it as the launcher and
     * the registry do; nothing is compiled). null when it passes or the tool isn't there; else what it said.
     */
    private static String horizonCheck(Path part, Path target, Options o, PrintStream out) throws IOException {
        HorizonTools tools = HorizonTools.locate(o.tools(), o.horizonSrc());
        if (tools == null) {
            out.println("  not checked: the file, by Horizon's own check (its cargo-horizon-mod isn't built: " + HorizonTools.HOW + ")");
            return null;
        }
        // Its check goes by the file's name: it gets the file under the name it will have.
        Path dir = Files.createTempDirectory("horizon-port-neoforge-pack");
        try {
            Path named = dir.resolve(target.getFileName().toString().endsWith(".hnmod") ? target.getFileName().toString() : "mod.hnmod");
            Files.copy(part, named);
            HorizonTools.Run run = tools.horizonMod(List.of("check", named.toString()), dir, 300);
            if (run.exit() != 0) {
                String said = (run.err().isBlank() ? run.out() : run.err()).strip();
                return said.isEmpty() ? "exit " + run.exit() : said.substring(said.lastIndexOf('\n') + 1).strip();
            }
            out.println("  ok: Horizon's own check takes the file (`cargo horizon-mod check`)");
            return null;
        } finally {
            Port.delete(dir);
        }
    }

    private static void put(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        // The same bundle gives the same file, whenever it is packed.
        entry.setTime(315_619_200_000L);
        zip.putNextEntry(entry);
        OutputStream stream = zip;
        stream.write(bytes);
        zip.closeEntry();
    }

    private static String count(int n, String what) {
        return n + " " + what + (n == 1 ? "" : "s");
    }
}
