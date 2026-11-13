package horizonneo;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Whether a mod's licence lets it be decompiled and ported, decided by horizon-port's own licence
 * rules, unchanged.
 *
 * horizon-port reads a licence from fabric.mod.json and from the jar's LICENSE / COPYING files. So it
 * is handed a stand-in jar that holds only that: a fabric.mod.json written from what the mod's own
 * metadata declares (licence and authors, word for word), and the jar's licence files as they are.
 * No class and no other file of the mod goes into it, and it is deleted as soon as horizon-port has
 * answered. The rules stay horizon-port's: nothing here classifies a licence.
 */
final class LicenceCheck {
    private LicenceCheck() {}

    /** allowed: horizon-port said yes. report: what it printed, naming the mod's real metadata file. */
    record Verdict(boolean allowed, String report) {}

    /** The most licence-like files a jar may hold: past that the check is refused, not done on a part of them. */
    static final int MAX_FILES = 256;

    /** horizon-port's exit code when the licence doesn't allow it. */
    private static final int NOT_ALLOWED = 3;

    private static final int SECONDS = 120;

    /**
     * The check for a NeoForge jar. A jar that is made for Fabric too is also checked as horizon-port
     * checks any Fabric jar, on its own fabric.mod.json, and both have to allow it.
     */
    static Verdict run(ModJar jar, ModInfo info, String claimedAuthor, Translator translator) throws IOException {
        Verdict verdict = onStandIn(jar, info, claimedAuthor, translator);
        if (!jar.fabric()) return verdict;
        Verdict fabric = ask(translator, jar.path(), claimedAuthor);
        boolean both = verdict.allowed() && fabric.allowed();
        return new Verdict(both, verdict.report()
                + "\nThe jar also has a " + ModJar.FABRIC + ", checked as well. Both have to allow it: **"
                + (both ? "allowed" : "not allowed") + "**.\n\n" + fabric.report());
    }

    private static Verdict onStandIn(ModJar jar, ModInfo info, String claimedAuthor, Translator translator) throws IOException {
        Path dir = Files.createTempDirectory("horizon-port-neoforge-");
        // Under the mod's own file name, so the report is headed with it.
        Path standIn = dir.resolve(jar.name());
        try {
            standIn(jar, info, standIn);
            Verdict verdict = ask(translator, standIn, claimedAuthor);
            return new Verdict(verdict.allowed(), report(verdict.report(), info));
        } finally {
            Files.deleteIfExists(standIn);
            Files.deleteIfExists(dir);
        }
    }

    /** `horizon-port license <jar>`: exit code 0 allows, 3 refuses; anything else is horizon-port failing. */
    private static Verdict ask(Translator translator, Path jar, String claimedAuthor) throws IOException {
        List<String> args = new ArrayList<>(List.of("license", jar.toString()));
        if (claimedAuthor != null) {
            args.add("--i-am-the-author");
            args.add(claimedAuthor);
        }
        Translator.Run run = translator.run(args, SECONDS);
        if (run.exit() != 0 && run.exit() != NOT_ALLOWED) {
            String said = (run.err().isBlank() ? run.out() : run.err()).strip();
            throw new IOException("horizon-port's licence check failed (exit " + run.exit() + ")" + (said.isEmpty() ? "" : ": " + said));
        }
        return new Verdict(run.exit() == 0, run.out());
    }

    /** Writes the stand-in jar: the fabric.mod.json, then the licence-like files of `jar`, each as it is. */
    static void standIn(ModJar jar, ModInfo info, Path target) throws IOException {
        if (jar.licenceFiles().size() > MAX_FILES) {
            throw new IOException(jar.name() + " holds " + jar.licenceFiles().size() + " files named like licences: too many to check them all, so none is");
        }
        try (ZipFile original = new ZipFile(jar.path().toFile());
             OutputStream file = Files.newOutputStream(target);
             ZipOutputStream zip = new ZipOutputStream(file)) {
            put(zip, ModJar.FABRIC, Json.write(fabricMetadata(info)).getBytes(StandardCharsets.UTF_8));
            Set<String> written = new HashSet<>(Set.of(ModJar.FABRIC));
            for (String name : jar.licenceFiles()) {
                ZipEntry entry = original.getEntry(name);
                if (entry == null || !written.add(name)) continue;
                put(zip, name, ModJar.head(original, entry, ModJar.MAX_TEXT));
            }
        }
    }

    /** What the mod declares, in the shape horizon-port reads: nothing is added, nothing is guessed. */
    static Map<String, Object> fabricMetadata(ModInfo info) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("schemaVersion", 1);
        m.put("id", info.id());
        m.put("name", info.name());
        m.put("version", info.version());
        if (!info.licences().isEmpty()) m.put("license", info.licences());
        m.put("authors", info.authors());
        return m;
    }

    /**
     * horizon-port's report, saying where the licence was really read: the metadata file's name, and
     * the authors as the mod writes them (they were handed over as a list of names that each count).
     */
    static String report(String theirs, ModInfo info) {
        String text = named(theirs, info);
        StringBuilder b = new StringBuilder(text);
        if (!text.endsWith("\n")) b.append('\n');
        b.append("\nChecked by horizon-port's licence rules, on what ").append(info.metadataFile())
                .append(" declares and on the jar's licence files.\n");
        for (String warning : info.warnings()) b.append("Note: ").append(warning).append(".\n");
        return b.toString();
    }

    /** horizon-port's words on the licence, with the mod's real metadata file and its authors as the mod writes them. */
    static String named(String theirs, ModInfo info) {
        String text = theirs.replace("Declared in " + ModJar.FABRIC, "Declared in " + info.metadataFile());
        if (info.authors().size() > 1) text = text.replace(String.join(", ", info.authors()), info.authors().get(0));
        return text;
    }

    private static void put(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }
}
