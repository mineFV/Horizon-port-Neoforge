package horizonneo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * The copy of a mod's jar that horizon-port is handed: what is in it as it was, what is written
 * again, and what is left out. On the two made-up mods' jars (the second and fourth arguments).
 */
final class StandInTest {
    private StandInTest() {}

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("the copy of the jar");
        Path dir = Files.createTempDirectory("standin-test-");
        try {
            // ---- Pocket Compass ----
            ModJar jar = ModJar.open(Path.of(args[1]));
            NeoForgeFrontend frontend = NeoForgeFrontend.read(jar);
            Analysis a = Analyzer.analyze(jar, frontend, BusFacts.documented());
            Path copy = dir.resolve("pocketcompass.jar");
            StandIn.Written written = StandIn.write(jar, frontend.info(), a, copy);
            try (ZipFile original = new ZipFile(jar.path().toFile()); ZipFile zip = new ZipFile(copy.toFile())) {
                Map<String, Object> metadata = Json.object(Json.parse(text(zip, "fabric.mod.json")));
                c.same("the mod's id", "pocketcompass", Json.text(metadata, "id"));
                c.same("its version", "1.2.3", Json.text(metadata, "version"));
                c.same("its licence", List.of("MIT"), Json.array(metadata.get("license")));
                c.same("its Mixin configs", List.of("pocketcompass.mixins.json"), Json.array(metadata.get("mixins")));
                c.yes("its description", Json.text(metadata, "description").startsWith("Shows which way you face"));

                String config = text(zip, "pocketcompass.mixins.json");
                c.yes("the mod's Mixin config has a comment", text(original, "pocketcompass.mixins.json").contains("//"));
                c.yes("the copy's has none", !config.contains("//"));
                c.same("and is plain JSON of the same thing", Json.write(Json.parse(text(original, "pocketcompass.mixins.json"))), config);

                String mixin = "com/example/pocketcompass/mixin/MinecraftMixin.class";
                c.yes("a listed Mixin class is there, byte for byte", java.util.Arrays.equals(bytes(original, mixin), bytes(zip, mixin)));
                c.yes("a class is there, byte for byte", java.util.Arrays.equals(bytes(original, "com/example/pocketcompass/Shapes.class"),
                        bytes(zip, "com/example/pocketcompass/Shapes.class")));
                c.yes("the original has the Mixin class no config lists", original.getEntry("com/example/pocketcompass/mixin/UnlistedMixin.class") != null);
                c.yes("the copy doesn't", zip.getEntry("com/example/pocketcompass/mixin/UnlistedMixin.class") == null);
                c.yes("the mod's own metadata is there", zip.getEntry(ModsToml.PATH) != null);
                c.yes("its data is there", zip.getEntry("data/pocketcompass/recipe/pocket_compass.json") != null);
            }
            c.same("what was left out", 1, written.leftOut().size());
            c.yes("and why", written.leftOut().get(0).startsWith("the Mixin class UnlistedMixin: no Mixin config lists it"));

            // ---- the second mod: a jar inside the jar, a Mixin config that isn't there ----
            ModJar odd = ModJar.open(Path.of(args[3]));
            NeoForgeFrontend oddFrontend = NeoForgeFrontend.read(odd);
            Path oddCopy = dir.resolve("oddities.jar");
            StandIn.Written oddWritten = StandIn.write(odd, oddFrontend.info(), Analyzer.analyze(odd, oddFrontend, BusFacts.documented()), oddCopy);
            try (ZipFile original = new ZipFile(odd.path().toFile()); ZipFile zip = new ZipFile(oddCopy.toFile())) {
                c.yes("the original holds a jar", original.getEntry("META-INF/jarjar/library-1.0.jar") != null);
                c.yes("the copy doesn't", zip.getEntry("META-INF/jarjar/library-1.0.jar") == null);
                c.same("only the Mixin config that is in the jar is named", List.of("oddities.mixins.json"),
                        Json.array(Json.object(Json.parse(text(zip, "fabric.mod.json"))).get("mixins")));
            }
            c.yes("the jar inside is said to be left out", oddWritten.leftOut().stream().anyMatch(l -> l.startsWith("1 jar inside the jar (META-INF/jarjar/library-1.0.jar)")));

            // ---- entries that mustn't go into a copy ----
            Path made = dir.resolve("made.jar");
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(made))) {
                for (String name : List.of("fabric.mod.json", "../evil.txt", "a/../../evil.txt", "META-INF/SIGNER.SF", "META-INF/SIGNER.RSA", "kept.txt",
                        "META-INF/services/kept.SF")) {
                    zip.putNextEntry(new ZipEntry(name));
                    zip.write(("this is " + name).getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
            }
            ModInfo info = new ModInfo("neoforge", ModsToml.PATH, "made", "Made", "1", "", List.of("Mine"), List.of("MIT"), List.of(), List.of());
            Analysis none = new Analysis(new Analysis.Census(0, 0, 0), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    false, List.of(), List.of());
            Path madeCopy = dir.resolve("made-copy.jar");
            StandIn.Written madeWritten = StandIn.write(ModJar.open(made), info, none, madeCopy);
            try (ZipFile zip = new ZipFile(madeCopy.toFile())) {
                List<String> names = new ArrayList<>();
                zip.stream().forEach(e -> names.add(e.getName()));
                names.sort(null);
                c.same("what the copy holds", List.of("META-INF/services/kept.SF", "fabric.mod.json", "kept.txt"), names);
                c.same("a fabric.mod.json of the jar's own is replaced", "made", Json.text(Json.object(Json.parse(text(zip, "fabric.mod.json"))), "id"));
                c.yes("no description is made up", !Json.object(Json.parse(text(zip, "fabric.mod.json"))).containsKey("description"));
            }
            c.same("files written", 2, madeWritten.files());
            c.yes("names that lead out of the jar are said to be left out", madeWritten.leftOut().stream().anyMatch(l -> l.startsWith("2 entries whose names lead out of the jar")));

            // ---- Mixin configs as plain JSON ----
            c.same("comments and a trailing comma go", Json.write(Json.parse("{\"a\": [1]}")), StandIn.plainJson("{ // why\n \"a\": [1,], }"));
            c.same("what isn't an object isn't a config", null, StandIn.plainJson("[1, 2]"));
            c.same("what isn't JSON isn't one either", null, StandIn.plainJson("{ nope"));
        } finally {
            Checks.delete(dir);
        }
        c.yes("nothing of it is left", !Files.exists(dir));
        System.exit(c.done());
    }

    private static String text(ZipFile zip, String name) throws Exception {
        return new String(bytes(zip, name), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(ZipFile zip, String name) throws Exception {
        try (var in = zip.getInputStream(zip.getEntry(name))) {
            return in.readAllBytes();
        }
    }
}
