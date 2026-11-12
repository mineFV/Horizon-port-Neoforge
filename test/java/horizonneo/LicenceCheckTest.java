package horizonneo;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** What horizon-port is handed for the licence check: the mod's declared licence and its licence files, and nothing else of the mod. */
final class LicenceCheckTest {
    private LicenceCheckTest() {}

    private static final String TOML = """
            license = "LGPL-3.0-only"
            [[mods]]
            modId = "pocketcompass"
            version = "1.2.3"
            displayName = "Pocket Compass"
            authors = "Mine, Second Author"
            """;

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("licence stand-in");
        Path dir = Files.createTempDirectory("licence-check-test-");
        try {
            Map<String, byte[]> mod = new LinkedHashMap<>();
            mod.put(ModsToml.PATH, bytes(TOML));
            mod.put("LICENSE", bytes("All rights reserved.\n"));
            mod.put("META-INF/COPYING.txt", bytes("GNU LESSER GENERAL PUBLIC LICENSE\n"));
            mod.put("com/example/pocketcompass/PocketCompass.class", new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
            mod.put("assets/pocketcompass/lang/en_us.json", bytes("{}"));
            mod.put("pocketcompass.mixins.json", bytes("{}"));
            Path jarPath = dir.resolve("pocketcompass.jar");
            zip(jarPath, mod);

            ModJar jar = ModJar.open(jarPath);
            c.same("the entries named like licences", List.of("LICENSE", "META-INF/COPYING.txt"), jar.licenceFiles());
            ModInfo info = NeoForgeFrontend.read(jar).info();

            Path standIn = dir.resolve("stand-in.jar");
            LicenceCheck.standIn(jar, info, standIn);
            Map<String, byte[]> entries = unzip(standIn);
            c.same("the stand-in holds the metadata and the licence files, and nothing else of the mod",
                    List.of("fabric.mod.json", "LICENSE", "META-INF/COPYING.txt"), List.copyOf(entries.keySet()));
            c.same("a licence file goes in as it is", "All rights reserved.\n", text(entries.get("LICENSE")));
            String json = text(entries.get("fabric.mod.json"));
            c.yes("the metadata is JSON with the licence in it", json.startsWith("{") && json.contains("\"license\"") && json.contains("\"LGPL-3.0-only\""));

            Map<String, Object> metadata = LicenceCheck.fabricMetadata(info);
            c.same("the licence, word for word", List.of("LGPL-3.0-only"), metadata.get("license"));
            c.same("the names that count as authors", List.of("Mine, Second Author", "Mine", "Second Author"), metadata.get("authors"));
            c.same("the mod's id", "pocketcompass", metadata.get("id"));

            ModInfo undeclared = new ModInfo("neoforge", ModsToml.PATH, "m", "M", "1", "", List.of(), List.of(), List.of(),
                    List.of("something to note"));
            c.yes("no licence declared: no `license` at all, not an empty one", !LicenceCheck.fabricMetadata(undeclared).containsKey("license"));

            // The report says where the licence was really read.
            String theirs = "# Licence check: pocketcompass.jar\n\n- Declared in fabric.mod.json: `LGPL-3.0-only`\n"
                    + "- Authors: Mine, Second Author, Mine, Second Author\n";
            String ours = LicenceCheck.report(theirs, info);
            c.yes("it names the mod's own metadata file", ours.contains("- Declared in META-INF/neoforge.mods.toml: `LGPL-3.0-only`\n") && !ours.contains("fabric.mod.json"));
            c.yes("it lists the authors as the mod writes them", ours.contains("- Authors: Mine, Second Author\n"));
            c.yes("it says whose rules decided", ours.contains("Checked by horizon-port's licence rules, on what META-INF/neoforge.mods.toml declares"));
            c.yes("it passes on what was odd in the metadata", LicenceCheck.report(theirs, undeclared).contains("Note: something to note.\n"));

            // Sizes are the jar's own claim: a huge licence file is cut, and too many are refused.
            Map<String, byte[]> big = new LinkedHashMap<>();
            big.put(ModsToml.PATH, bytes(TOML));
            big.put("LICENSE", new byte[ModJar.MAX_TEXT + 4096]);
            Path bigPath = dir.resolve("big.jar");
            zip(bigPath, big);
            Path bigStandIn = dir.resolve("big-stand-in.jar");
            LicenceCheck.standIn(ModJar.open(bigPath), info, bigStandIn);
            c.same("a licence file is read up to 1 MiB", ModJar.MAX_TEXT, unzip(bigStandIn).get("LICENSE").length);

            Map<String, byte[]> many = new LinkedHashMap<>();
            many.put(ModsToml.PATH, bytes(TOML));
            for (int i = 0; i <= LicenceCheck.MAX_FILES; i++) many.put("licenses/LICENSE-" + i + ".txt", bytes("x"));
            Path manyPath = dir.resolve("many.jar");
            zip(manyPath, many);
            ModJar manyJar = ModJar.open(manyPath);
            c.refuses("too many licence files to check them all", "too many to check them all, so none is",
                    () -> LicenceCheck.standIn(manyJar, info, dir.resolve("many-stand-in.jar")));
        } finally {
            List<Path> left = new ArrayList<>();
            try (var files = Files.list(dir)) {
                files.forEach(left::add);
            }
            for (Path p : left) Files.delete(p);
            Files.delete(dir);
        }
        System.exit(c.done());
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void zip(Path file, Map<String, byte[]> entries) throws IOException {
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue());
                zip.closeEntry();
            }
        }
    }

    private static Map<String, byte[]> unzip(Path file) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(file.toFile())) {
            Enumeration<? extends ZipEntry> all = zip.entries();
            while (all.hasMoreElements()) {
                ZipEntry e = all.nextElement();
                try (var in = zip.getInputStream(e)) {
                    entries.put(e.getName(), in.readAllBytes());
                }
            }
        }
        return entries;
    }
}
