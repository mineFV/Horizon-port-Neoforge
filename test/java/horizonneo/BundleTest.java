package horizonneo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** What is done to a bundle after horizon-port wrote it, on small made-up pieces of one. */
final class BundleTest {
    private BundleTest() {}

    private static final String MANIFEST = """
            [mod]
            id = "m"

            [[injection]]
            id = "tick"
            kind = "HEAD"
            target = "horizon_client::game::native_hooks::Game<'_>::native_tick"
            helper = "tick.rs"
            bindings = [
              { name = "game", ty = "&mut crate::game::Game<'_>", value = "&mut *self" },
            ]

            [[injection]]
            id = "settings"
            kind = "HEAD"
            target = "horizon_client::game::native_hooks::Game<'_>::native_settings"
            helper = "settings.rs"

            # Off: something horizon-port left off itself
            # [[injection]]
            # id = "other"
            """;

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("the bundle's clean-up");
        Path dir = Files.createTempDirectory("bundle-test-");
        try {
            // ---- commenting an injection out ----
            String off = Bundle.turnOff(MANIFEST, "settings", List.of("why it is off.", "more of why.", "TODO: what to do."));
            c.yes("it is found", off != null);
            c.yes("the reason comes first", off.contains("# Off: why it is off.\n#   more of why.\n#   TODO: what to do.\n# [[injection]]\n# id = \"settings\"\n# kind = \"HEAD\"\n"));
            c.yes("down to its last line", off.contains("# helper = \"settings.rs\"\n\n# Off: something horizon-port left off itself"));
            c.yes("the other injection is as it was", off.startsWith(MANIFEST.substring(0, MANIFEST.indexOf("[[injection]]\nid = \"settings\""))));
            List<Map<String, Object>> left = Toml.tables(Toml.parse(off).root().get("injection"));
            c.same("the manifest still reads, with one injection", List.of("tick"), left.stream().map(t -> Toml.text(t, "id")).toList());
            c.same("lines only gain a mark: none is lost", MANIFEST.split("\n").length + 3, off.split("\n").length);
            String both = Bundle.turnOff(off, "tick", List.of("why."));
            c.same("an injection with an array in it goes whole", List.of(), Toml.tables(Toml.parse(both).root().get("injection")));
            c.yes("to its closing bracket", both.contains("# ]\n\n# Off: why it is off."));
            c.same("an injection that isn't there", null, Bundle.turnOff(MANIFEST, "nope", List.of("why.")));
            c.same("one that is off already isn't found again", null, Bundle.turnOff(MANIFEST, "other", List.of("why.")));

            // ---- a Random by value in horizon-port's runtime.rs ----
            String runtime = """
                    pub mod java {
                        pub struct JRandom(u64);
                        pub mod ore {
                            use super::*;
                            pub fn skip(random: JRandom, chance: f32) -> bool {
                                if chance <= 0.0f32 {
                                    return true;
                                }
                                random.next_float() >= chance
                            }
                            pub fn kept(random: &mut JRandom) -> f32 {
                                random.next_float()
                            }
                            pub fn called(seed: i64, random: JRandom) -> bool {
                                true
                            }
                            pub fn caller() -> bool {
                                called(0, JRandom(0))
                            }
                        }
                    }
                    """;
            List<String> randoms = new java.util.ArrayList<>();
            String cut = Bundle.randomsByValue(runtime, randoms);
            c.same("the function taking one by value, and only it", List.of("skip"), randoms);
            c.yes("it is written out down to its brace, with why", cut.contains("""
                            // skip: taken out by horizon-port-neoforge: it takes a `JRandom` by value, which Rust won't
                            // draw from and Java would share. TODO: take `&mut JRandom`, or the generator it stands for.
                            // pub fn skip(random: JRandom, chance: f32) -> bool {
                            //     if chance <= 0.0f32 {
                            //         return true;
                            //     }
                            //     random.next_float() >= chance
                            // }
                            pub fn kept(random: &mut JRandom) -> f32 {
                    """));
            c.yes("one that is called stays: taking it out would move the error", cut.contains("    pub fn called(seed: i64, random: JRandom) -> bool {"));
            c.same("the rest is as it was", runtime.split("\n").length + 2, cut.split("\n").length);
            List<String> none = new java.util.ArrayList<>();
            c.same("a file without any is left alone", "fn f(r: &mut JRandom) {\n}\n", Bundle.randomsByValue("fn f(r: &mut JRandom) {\n}\n", none));
            c.same("and names none", List.of(), none);

            // ---- targets that aren't in a Horizon source ----
            Path horizon = dir.resolve("horizon");
            Path game = Files.createDirectories(horizon.resolve("crates/horizon-client/src/game"));
            Files.writeString(horizon.resolve("crates/horizon-client/src/game.rs"), "mod native_hooks;\npub fn start() {}\n");
            Files.writeString(game.resolve("native_hooks.rs"), "impl Game<'_> {\n    pub(super) fn native_tick_block(&mut self) {}\n    pub(super) fn native_key<T>(&mut self) {}\n"
                    + "    pub(super) fn  native_tick (&mut self) {}\n}\n");
            Files.createDirectories(game.resolve("deep"));
            Files.writeString(game.resolve("deep/mod.rs"), "pub fn inner() {}\n");
            String hooks = "horizon_client::game::native_hooks::Game<'_>::";
            c.same("a hook point that is there", null, Bundle.missing(horizon, hooks + "native_tick"));
            c.same("one with type parameters", null, Bundle.missing(horizon, hooks + "native_key"));
            c.same("one that isn't", "`crates/horizon-client/src/game/native_hooks.rs` has no `fn native_gone`", Bundle.missing(horizon, hooks + "native_gone"));
            c.same("a longer name doesn't stand in for a shorter one", "`crates/horizon-client/src/game/native_hooks.rs` has no `fn native_tic`",
                    Bundle.missing(horizon, hooks + "native_tic"));
            c.same("a function of a module that is a folder", null, Bundle.missing(horizon, "horizon_client::game::deep::inner"));
            c.same("a function of the first module", null, Bundle.missing(horizon, "horizon_client::game::start"));
            c.same("one that isn't in it", "`crates/horizon-client/src/game.rs` has no `fn stop`", Bundle.missing(horizon, "horizon_client::game::stop"));
            c.same("a crate that isn't in the source can't be looked up", null, Bundle.missing(horizon, "horizon_server::game::tick"));
            c.same("nor can a module that isn't a file", null, Bundle.missing(horizon, "horizon_client::render::draw"));
            c.same("nor a target with no module", null, Bundle.missing(horizon, "horizon_client::main"));

            // ---- the decompiled Java ----
            Path decompiled = Files.createDirectories(dir.resolve("src/com/example"));
            Files.writeString(decompiled.resolve("Thing.java"), "package com.example;\n\npublic class Thing {\n    private void handler(CallbackInfo var1) {\n"
                    + "        total = total + amount(var1);\n        x++;\n    }\n}\n");
            Files.writeString(decompiled.resolve("notes.txt"), "// TODO: this line is in a file that isn't Java\n");
            Set<String> java = Bundle.javaLines(dir.resolve("src"));
            c.yes("a line of the source", java.contains("private void handler(CallbackInfo var1) {"));
            c.yes("a short one says nothing", !java.contains("x++;"));
            c.yes("only Java files count", java.stream().noneMatch(l -> l.contains("isn't Java")));

            Path bundle = Files.createDirectories(dir.resolve("bundle/mixins/m"));
            Files.writeString(bundle.resolve("tick.rs"), "// The `tick` hook point.\n//\n// Decompiled Java, com/example/Thing.java:4:\n// | private void handler(CallbackInfo var1) {\n// |     x++;\n// | }\n"
                    + "//   total = total + amount(var1);\nfn m_tick(game: &mut Game) {\n    // TODO: port the tick handler.\n    total = total + amount(var1);\n}\n");
            Files.writeString(bundle.resolve("horizon-mixin.toml"), "[mod]\nid = \"m\"\n# private void handler(CallbackInfo var1) {\n");
            Files.writeString(bundle.resolve("plain.rs"), "fn untouched() {}\n");
            Files.writeString(bundle.resolve("data.bin"), "// | not a text file of the bundle's\n");
            c.same("lines taken out", 5, Bundle.stripJava(dir.resolve("bundle"), java));
            c.same("the helper afterwards", "// The `tick` hook point.\n//\n// Its Java is com/example/Thing.java in the mod (decompiled code isn't kept in this bundle).\n"
                    + "fn m_tick(game: &mut Game) {\n    // TODO: port the tick handler.\n    total = total + amount(var1);\n}\n", Files.readString(bundle.resolve("tick.rs")));
            c.same("a comment in the manifest that repeats the source goes too", "[mod]\nid = \"m\"\n", Files.readString(bundle.resolve("horizon-mixin.toml")));
            c.same("a file with none is as it was", "fn untouched() {}\n", Files.readString(bundle.resolve("plain.rs")));
            c.same("a file that isn't text is as it was", "// | not a text file of the bundle's\n", Files.readString(bundle.resolve("data.bin")));
            c.same("a second pass finds nothing", 0, Bundle.stripJava(dir.resolve("bundle"), java));

            // ---- horizon-port's own figures ----
            Path translation = dir.resolve("translation.md");
            Files.writeString(translation, "# Java translated to Rust\n\nTranslated 3 of 21 methods and 1 of 14 static fields to Rust. It's in `runtime.rs`.\n");
            c.same("methods and fields", new Bundle.Translation(3, 21, 1, 14), Bundle.translation(translation));
            Files.writeString(translation, "Translated 0 of 2 methods to Rust.\n");
            c.same("methods alone", new Bundle.Translation(0, 2, null, null), Bundle.translation(translation));
            Files.writeString(translation, "Nothing to translate.\n");
            c.same("no figure", null, Bundle.translation(translation));
            c.same("no file", null, Bundle.translation(dir.resolve("none.md")));

            Path report = dir.resolve("report.json");
            Files.writeString(report, """
                    {"features": [
                      {"category": "class", "in": "a/B", "status": "needs-translation"},
                      {"category": "mixin", "in": "a/M.one", "horizon": "HEAD on hook `tick`", "status": "auto", "note": null},
                      {"category": "mixin", "in": "a/M.two", "horizon": "HEAD on hook `tick`", "status": "auto", "note": "First."},
                      {"category": "mixin", "in": "a/M.two", "horizon": "HEAD on hook `key`", "status": "needs-mapping", "note": "Second."}
                    ]}
                    """);
            Map<String, Bundle.Handler> statuses = Bundle.mixinStatuses(report);
            c.same("the Mixin handlers it names", List.of("a/M.one", "a/M.two"), List.copyOf(statuses.keySet()));
            c.same("one's status", new Bundle.Handler("auto", "HEAD on hook `tick`", null, "absent", null), statuses.get("a/M.one"));
            c.same("a handler on two targets has the status furthest from done",
                    new Bundle.Handler("needs-mapping", "HEAD on hook `tick`; HEAD on hook `key`", "First. Second.", "absent", null), statuses.get("a/M.two"));
            Files.writeString(report, "not JSON");
            c.same("a report that can't be read names none", Map.of(), Bundle.mixinStatuses(report));
        } finally {
            Checks.delete(dir);
        }
        c.yes("nothing of it is left", !Files.exists(dir));
        System.exit(c.done());
    }
}
