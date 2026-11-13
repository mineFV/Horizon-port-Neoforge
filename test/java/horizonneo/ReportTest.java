package horizonneo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * The report, on the first made-up mod's jar (the program's second argument): the status each part
 * gets, and the two files.
 */
final class ReportTest {
    private ReportTest() {}

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("report");
        ModJar jar = ModJar.open(Path.of(args[1]));
        NeoForgeFrontend frontend = NeoForgeFrontend.read(jar);
        Analysis a = Analyzer.analyze(jar, frontend, BusFacts.documented());

        // What becomes of each part: never `auto` or `content` without knowing.
        List<Report.Feature> features = Report.features(a);
        c.same("the statuses", Map.of("auto", 2, "content", 5, "needs-mapping", 15, "needs-translation", 3, "unsupported", 2, "info", 3), Report.counts(features));
        Report.Feature spinning = feature(features, "ModRegistry.SPINNING_BLOCK");
        c.yes("a block whose class has behaviour isn't plain content", spinning.status().equals("needs-translation") && spinning.note().contains("stepOn"));
        c.same("a block of the game's own class is content", "content", feature(features, "ModRegistry.COMPASS_BLOCK").status());
        c.same("something whose class is only its holder's declared type isn't called content", "needs-translation", feature(features, "ModRegistry.DING").status());
        c.same("a listener waits for the event table", "needs-mapping", feature(features, "ClientEvents.onClientTick").status());
        c.same("a listener NeoForge never calls is nothing to convert", "info", feature(features, "ClientEvents.notStatic").status());
        Report.Feature orphan = feature(features, "Orphan.onTick");
        c.yes("one that only looks unregistered still counts", orphan.status().equals("needs-mapping") && orphan.note().startsWith("No registration that covers it was found"));
        c.same("a screen is out of scope", "unsupported", feature(features, "com.example.pocketcompass.CompassScreen").status());
        c.same("a Mixin class no config lists is nothing to convert", "info", feature(features, "com.example.pocketcompass.mixin.UnlistedMixin").status());
        c.same("an injector waits for horizon-port", "needs-mapping", feature(features, "com.example.pocketcompass.mixin.MinecraftMixin.pocketcompass$onTick").status());
        c.same("an access transformer is nothing to convert", "info", feature(features, "META-INF/accesstransformer.cfg").status());
        c.same("a config isn't converted yet", "needs-translation", feature(features, "Config.SPEC").status());
        c.same("the jar's resources are content", "1 asset file, 1 data file", feature(features, "the jar's resources").detail());
        c.yes("no part is `auto` but the entry points", features.stream().filter(f -> f.status().equals("auto")).allMatch(f -> f.category().equals("entrypoint")));
        c.yes("no listener names a hook yet", features.stream().filter(f -> f.category().equals("listener")).allMatch(f -> f.horizon() == null));

        c.same("the line for the terminal", "Pocket Compass 1.2.3: 13 classes, 2 @Mod classes, 14 event listeners, 6 registrations, 2 Mixin classes "
                + "(2 auto, 5 content, 15 needs-mapping, 3 needs-translation, 2 unsupported, 3 info)", Report.summary(frontend, a));

        // report.json: what it writes reads back as JSON, with every part in it.
        Map<String, Object> json = Json.object(Json.parse(Json.write(Report.json(jar, frontend, a))));
        c.same("the tool and the schema", List.of("horizon-port-neoforge", 1L), List.of(json.get("tool"), json.get("schema")));
        c.same("the mod", List.of("pocketcompass", "Pocket Compass", "1.2.3", "MIT"), List.of(Json.text(Json.object(json.get("mod")), "id"),
                Json.text(Json.object(json.get("mod")), "name"), Json.text(Json.object(json.get("mod")), "version"), Json.text(Json.object(json.get("mod")), "license")));
        c.same("its sections' sizes", List.of(2, 14, 3, 1, 2, 1, 2, 1, 3),
                List.of(size(json, "entryPoints"), size(json, "listeners"), size(json, "registries"), size(json, "configs"), size(json, "mixins"),
                        size(json, "accessTransformers"), size(json, "gameClasses"), size(json, "apiAreas"), size(json, "dependencies")));
        c.same("one row for each part", features.size(), size(json, "features"));
        Map<String, Object> first = Json.object(Json.array(json.get("listeners")).get(0));
        c.same("a listener's event, as Java tools name classes", "net.neoforged.neoforge.client.event.ClientTickEvent$Post", first.get("event"));
        c.yes("a listener has a `hook`, empty for now", first.containsKey("hook") && first.get("hook") == null);
        c.same("the counts", Map.of("auto", 2L, "content", 5L, "needs-mapping", 15L, "needs-translation", 3L, "unsupported", 2L, "info", 3L), json.get("counts"));

        // report.md
        String markdown = Report.markdown(jar, frontend, a);
        c.yes("it says how the jar was read", markdown.contains("Nothing was decompiled, and nothing of the mod was run."));
        c.yes("a listener that isn't registered says so in its row",
                markdown.contains("| `ClientEvents.notStatic` | not registered (@EventBusSubscriber on ClientEvents) | info |"));
        c.yes("it says why some buses aren't known, and how to have them read", markdown.contains("The bus of 5 listeners isn't known.") && markdown.contains("`--neoforge-jar`"));
        c.yes("no warnings, no Warnings section", !markdown.contains("## Warnings"));

        // Both files, in a folder that doesn't exist yet.
        Path dir = Files.createTempDirectory("report-test-");
        try {
            Report.write(dir.resolve("deep/er"), jar, frontend, a);
            c.yes("report.md and report.json are written", Files.isRegularFile(dir.resolve("deep/er/report.md")) && Files.isRegularFile(dir.resolve("deep/er/report.json")));
            c.same("report.md is the markdown", markdown, Files.readString(dir.resolve("deep/er/report.md")));
        } finally {
            Files.deleteIfExists(dir.resolve("deep/er/report.md"));
            Files.deleteIfExists(dir.resolve("deep/er/report.json"));
            Files.deleteIfExists(dir.resolve("deep/er"));
            Files.deleteIfExists(dir.resolve("deep"));
            Files.deleteIfExists(dir);
        }

        // A mod with no code to speak of: every section still says something.
        Analysis empty = new Analysis(new Analysis.Census(0, 0, 0), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), false, List.of());
        String bare = Report.markdown(jar, frontend, empty);
        c.yes("no @Mod class", bare.contains("No class has `@Mod`"));
        c.yes("no listener, registry, config or Mixin", bare.contains("## Event listeners\n\nNone.\n") && bare.contains("No `DeferredRegister`.")
                && bare.contains("None registered.") && bare.contains("## Mixins\n\nNone.\n") && bare.contains("## Access transformers\n\nNone.\n"));
        c.same("its line for the terminal", "Pocket Compass 1.2.3: 0 classes, 0 @Mod classes, 0 event listeners, 0 registrations, 0 Mixin classes", Report.summary(frontend, empty));
        c.same("no part at all", List.of(), Report.features(empty));

        System.exit(c.done());
    }

    private static Report.Feature feature(List<Report.Feature> features, String source) {
        return features.stream().filter(f -> f.source().equals(source)).findFirst().orElseThrow();
    }

    private static int size(Map<String, Object> json, String key) {
        return Json.array(json.get(key)).size();
    }
}
