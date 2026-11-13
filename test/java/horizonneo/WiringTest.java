package horizonneo;

import horizonneo.Analysis.Listener;
import horizonneo.BusFacts.Bus;
import horizonneo.EventTable.Binding;
import horizonneo.EventTable.Hook;
import horizonneo.EventTable.Level;
import horizonneo.Wiring.Wired;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Listeners put on hook points, in a small made-up bundle: what is switched on, what is written commented out, and why. */
final class WiringTest {
    private WiringTest() {}

    private static final String T = "horizon_client::game::native_hooks::Game<'_>::native_";
    private static final String E = "net/neoforged/neoforge/event/";

    private static final String CATALOGUE = """
            { "hooks": [
              { "name": "tick", "target": "%1$stick", "neoforge": "ClientTickEvent.Post", "side": "client", "mod_api": {} },
              { "name": "destroy_block", "target": "%1$sdestroy_block", "neoforge": "BlockEvent.BreakEvent", "side": "server", "mod_api": {} },
              { "name": "item_dropped", "target": "%1$sitem_dropped", "neoforge": "ItemTossEvent (only hears)", "side": "server", "mod_api": {} },
              { "name": "frame", "target": "%1$sframe", "neoforge": "RenderFrameEvent.Pre", "mod_api": {} },
              { "name": "render_hand", "target": "%1$srender_hand", "neoforge": "RenderHandEvent", "mod_api": {} },
              { "name": "render_hand_pose", "target": "%1$srender_hand_pose", "neoforge": "RenderHandEvent (PoseStack)", "mod_api": {} }
            ] }
            """.formatted(T);

    private static final String HOOK_LIST = """
            { "hooks": [
              { "name": "native_tick", "kind": "hook", "api": 1, "signature": "fn native_tick(&mut self)", "doc": "Once a tick.",
                "bindings": [ { "name": "game", "ty": "&mut crate::game::Game<'_>", "value": "&mut *self" } ] },
              { "name": "native_destroy_block", "kind": "hook", "api": 14, "signature": "fn native_destroy_block(&mut self, pos: [i32; 3], canceled: &mut bool)",
                "doc": "A player breaks\\n a block.", "bindings": [
                  { "name": "game", "ty": "&mut crate::game::Game<'_>", "value": "&mut *self" },
                  { "name": "pos", "ty": "[i32; 3]", "value": "pos" },
                  { "name": "canceled", "ty": "&mut bool", "value": "&mut *canceled" } ] },
              { "name": "native_item_dropped", "kind": "hook", "api": 30, "signature": "fn native_item_dropped(&self, player: u128)",
                "bindings": [ { "name": "game", "ty": "&crate::game::Game<'_>", "value": "&*self" }, { "name": "player", "ty": "u128", "value": "player" } ] }
            ] }
            """;

    private static final String MANIFEST = """
            [mod]
            id = "m"

            [[module]]
            id = "runtime"
            file = "crates/horizon-client/src/game.rs"
            name = "port_m"
            source = "runtime.rs"

            [[module]]
            id = "game_side"
            file = "crates/horizon-client/src/game.rs"
            name = "port_m_game"
            source = "game_side.rs"

            [[injection]]
            id = "tick"
            kind = "HEAD"
            target = "%1$stick"
            helper = "tick.rs"
            bindings = [
              { name = "game", ty = "&mut crate::game::Game<'_>", value = "&mut *self" },
            ]

            # Off: something horizon-port left off
            # [[injection]]
            # id = "handlers-on-toss"
            # target = "TODO"
            """.formatted(T);

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("listeners on hook points");
        EventTable table = EventTable.build(CATALOGUE, HOOK_LIST, List.of(), List.of());
        List<Listener> listeners = List.of(
                listener(E + "level/BlockEvent$BreakEvent", "Handlers", "onBreak", true, "HIGH", false, "both", null, List.of()),
                listener("net/neoforged/neoforge/client/event/ClientTickEvent$Post", "Handlers", "onTick", true, "NORMAL", false, "client", "@OnlyIn", List.of()),
                listener(E + "entity/item/ItemTossEvent", "Handlers", "onToss", true, "NORMAL", false, "both", null, List.of()),
                listener(E + "level/BlockEvent$BreakEvent", "Handlers", "wrongBus", true, "NORMAL", false, "both", null,
                        List.of("it is added to the mod bus, but by NeoForge's documentation its event is posted on the game bus")),
                listener(E + "level/BlockEvent$BreakEvent", "Handlers", "never", false, "NORMAL", false, "both", null, List.of()),
                listener("com/example/OwnEvent", "Handlers", "onOwn", true, "NORMAL", false, "both", null, List.of()),
                listener("net/neoforged/neoforge/client/event/RenderFrameEvent$Pre", "Handlers", "onFrame", true, "NORMAL", false, "both", null, List.of()),
                listener("net/neoforged/neoforge/client/event/RenderHandEvent", "Handlers", "onHand", true, "NORMAL", false, "both", null, List.of()),
                listener("net/neoforged/fml/event/lifecycle/FMLClientSetupEvent", "Handlers", null, true, "NORMAL", false, "both", null, List.of()),
                listener(E + "level/BlockEvent$BreakEvent", "Other", "alsoBreak", true, "NORMAL", true, "both", null, List.of()));
        Analysis a = new Analysis(new Analysis.Census(0, 0, 0), List.of(), listeners, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), false, List.of());

        Path dir = Files.createTempDirectory("wiring-test-");
        try {
            Path mod = bundle(dir.resolve("plain"));
            List<Wired> wired = Wiring.apply(mod, "m", a, table, false);
            c.same("one answer a listener, in order", 10, wired.size());
            c.same("what each became", List.of("on", "on", "off", "off", "none", "none", "off", "off", "off", "on"), wired.stream().map(Wired::state).toList());
            c.same("the table's answer for each",
                    java.util.Arrays.asList(Level.EXACT, Level.EXACT, Level.APPROXIMATE, Level.EXACT, null, Level.OWN, Level.EXACT, Level.SEVERAL, Level.NONE, Level.EXACT),
                    wired.stream().map(Wired::level).toList());
            c.same("where each is in the manifest",
                    java.util.Arrays.asList("destroy-block", "tick", "handlers-on-toss-2", "handlers-wrong-bus", null, null, "handlers-on-frame", "handlers-on-hand", "handlers-listener", "destroy-block"),
                    wired.stream().map(Wired::injection).toList());
            c.same("the hook point named for each",
                    java.util.Arrays.asList("destroy_block", "tick", "item_dropped", "destroy_block", null, null, "frame", null, null, "destroy_block"),
                    wired.stream().map(Wired::hook).toList());
            c.yes("a listener on the wrong bus is never called", wired.get(3).why().startsWith("NeoForge never calls it: it is added to the mod bus"));
            c.yes("an approximate answer says how to switch it on", wired.get(2).why().endsWith("`--allow-approximate` switches such listeners on"));
            c.yes("a hook point with no signature can't be written a helper", wired.get(6).why().contains("but horizon-port read no signature for `frame`"));
            c.yes("an event of the mod's own isn't NeoForge's", wired.get(5).why().startsWith("it isn't one of NeoForge's events"));

            String manifest = Files.readString(mod.resolve("horizon-mixin.toml"));
            List<Map<String, Object>> injections = Toml.tables(Toml.parse(manifest).root().get("injection"));
            c.same("the injections that are on", List.of("tick", "destroy-block"), injections.stream().map(i -> Toml.text(i, "id")).toList());
            c.same("the new one's target", T + "destroy_block", Toml.text(injections.get(1), "target"));
            c.same("its helper", "destroy-block.rs", Toml.text(injections.get(1), "helper"));
            c.same("its bindings", List.of("game", "pos", "canceled"), Toml.tables(injections.get(1).get("bindings")).stream().map(b -> Toml.text(b, "name")).toList());
            c.yes("what was there is as it was", manifest.startsWith(MANIFEST.stripTrailing()));
            c.yes("a listener that is off: the reason, a TODO, the injection it would be", manifest.contains("""
                    # Off: Handlers.onToss listens to NeoForge's ItemTossEvent.
                    #   Horizon's catalogue names `item_dropped` for it, with a note to read first: "only hears". `--allow-approximate` switches such listeners on.
                    #   TODO: read what Horizon says of `item_dropped`; if it is what the handler needs, write its helper and uncomment this.
                    # [[injection]]
                    # id = "handlers-on-toss-2"
                    # kind = "HEAD"
                    # target = "%sitem_dropped"
                    """.formatted(T)));
            c.yes("one with no hook point has no target", manifest.contains("# id = \"handlers-listener\"\n# kind = \"HEAD\"\n# target = \"TODO\"\n"));
            c.yes("one with several has none either", manifest.contains("# id = \"handlers-on-hand\"\n# kind = \"HEAD\"\n# target = \"TODO\"\n"));
            c.yes("nothing is written for a listener NeoForge never calls", !manifest.contains("Handlers.never") && !manifest.contains("Handlers.onOwn"));

            String helper = Files.readString(mod.resolve("destroy-block.rs"));
            c.yes("the helper takes the hook point's parameters", helper.contains("\nfn m_destroy_block(game: &mut crate::game::Game<'_>, pos: [i32; 3], canceled: &mut bool) {\n"));
            c.yes("it stands in for both listeners", helper.contains("//   Handlers.onBreak, which listens to BlockEvent.BreakEvent (addListener somewhere)\n")
                    && helper.contains("//   Other.alsoBreak, which listens to BlockEvent.BreakEvent"));
            c.yes("a priority is said", helper.contains("//     Its priority is HIGH: Horizon calls mods in their own order.\n"));
            c.same("a listener that doesn't hear cancelled events is told to check, the other isn't", 1, count(helper, "check `*canceled` first"));
            c.yes("what Horizon says of the hook point, on one line", helper.contains("// What Horizon says of it: A player breaks a block.\n"));
            c.yes("it leaves when the mod is off, and runs the initializer", helper.contains("    if !game.native_on(m::ID) {\n        return;\n    }\n")
                    && helper.contains("    crate::game::port_m_game::ensure_init(game);\n"));
            c.same("a TODO a listener", 2, count(helper, "    // TODO: port "));
            c.yes("it does nothing else", helper.endsWith("    let _ = (game, pos, canceled, &m::ID);\n}\n"));
            c.same("no decompiled Java can be in it: it has one function", 1, count(helper, "\nfn "));

            String tick = Files.readString(mod.resolve("tick.rs"));
            c.yes("a hook point that is taken keeps its one injection: the helper there is told", tick.contains(
                    "// Added by horizon-port-neoforge: Horizon's hook catalogue names this hook point for ClientTickEvent.Post too.\n// It stands in for:\n"
                    + "//   Handlers.onTick, which listens to ClientTickEvent.Post (addListener somewhere)\n// TODO: port Handlers.onTick here as well.\nfn m_tick("));
            c.yes("and is otherwise as it was", tick.startsWith("// The `tick` hook point.\n") && tick.endsWith("fn m_tick(game: &mut crate::game::Game<'_>) {\n}\n"));
            c.yes("the bundle needs the hooks API the new hook point came in", Files.readString(mod.getParent().getParent().resolve("compatibility.json")).contains("\"hooks\": 14,"));

            // ---- with the approximate answers switched on too ----
            Path loose = bundle(dir.resolve("loose"));
            List<Wired> more = Wiring.apply(loose, "m", a, table, true);
            c.same("the listener with a note is on now", "on", more.get(2).state());
            c.same("in an injection of its own", "item-dropped", more.get(2).injection());
            c.same("nothing else changed", wired.stream().map(Wired::state).filter(s -> s.equals("on")).count() + 1, more.stream().filter(w -> w.state().equals("on")).count());
            String dropped = Files.readString(loose.resolve("item-dropped.rs"));
            c.yes("a hook point that only reads the game takes it so", dropped.contains("\nfn m_item_dropped(game: &crate::game::Game<'_>, player: u128) {\n"));
            c.yes("and doesn't run the initializer, which changes it", !dropped.contains("ensure_init"));
            c.yes("the API it needs is the highest", Files.readString(loose.getParent().getParent().resolve("compatibility.json")).contains("\"hooks\": 30,"));

            // ---- no listener: nothing is touched ----
            Path quiet = bundle(dir.resolve("quiet"));
            Analysis none = new Analysis(new Analysis.Census(0, 0, 0), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), false, List.of());
            c.same("no listener, no answer", List.of(), Wiring.apply(quiet, "m", none, table, false));
            c.same("and the manifest is as it was", MANIFEST, Files.readString(quiet.resolve("horizon-mixin.toml")));
        } finally {
            Checks.delete(dir);
        }

        // ---- hook points a helper can't be written for ----
        Binding game = new Binding("game", "&mut crate::game::Game<'_>", "&mut *self");
        c.same("one that is fine", null, Wiring.unusable(hook(null, "fn native_x(&mut self, stack: Option<&horizon_client::items::ItemStack>, out: &mut Vec<String>)",
                List.of(game, new Binding("stack", "Option<&horizon_client::items::ItemStack>", "stack"), new Binding("out", "&mut Vec<String>", "&mut *out")))));
        c.yes("one that is being prepared", Wiring.unusable(hook("tools/native-hook-fragments/x.json", "fn native_x(&mut self)", List.of(game))).contains("isn't in Horizon's hooks API yet"));
        c.yes("one with no signature", Wiring.unusable(hook(null, null, List.of())).contains("read no signature"));
        c.yes("one that isn't handed the game first", Wiring.unusable(hook(null, "fn native_x(kind: &str)", List.of(new Binding("kind", "&str", "kind")))).contains("isn't handed the game first"));
        c.yes("one with a type that has no path", Wiring.unusable(hook(null, "fn native_x(&mut self, view: &NativeScreenView<'_>)",
                List.of(game, new Binding("view", "&NativeScreenView<'_>", "view")))).contains("`view: &NativeScreenView<'_>` names a type without its path"));

        c.same("a handler's name as an id", "client-events-on-client-tick", Wiring.kebab("ClientEvents-onClientTick"));
        c.same("a lambda's", "pocket-compass-lambda-new-0", Wiring.kebab("PocketCompass-lambda$new$0"));
        c.same("nothing to name it by", "listener", Wiring.kebab("$$"));
        System.exit(c.done());
    }

    private static Listener listener(String event, String owner, String method, boolean registered, String priority, boolean receiveCanceled, String side, String sideFrom,
                                     List<String> notes) {
        return new Listener(event, "com/example/" + owner, method, false, "addListener somewhere", registered, Bus.GAME, "it is added to NeoForge.EVENT_BUS", side, sideFrom,
                priority, receiveCanceled, notes);
    }

    private static Hook hook(String fragment, String signature, List<Binding> bindings) {
        return new Hook("x", T + "x", "SomeEvent", null, 1, null, null, "listen", 1, null, signature, bindings, fragment);
    }

    /** A bundle as horizon-port leaves one: the manifest, its helper and modules, compatibility.json. Returns mixins/m. */
    private static Path bundle(Path dir) throws Exception {
        Path mod = Files.createDirectories(dir.resolve("mixins/m"));
        Files.writeString(mod.resolve("horizon-mixin.toml"), MANIFEST);
        Files.writeString(mod.resolve("tick.rs"), "// The `tick` hook point.\nfn m_tick(game: &mut crate::game::Game<'_>) {\n}\n");
        Files.writeString(mod.resolve("runtime.rs"), "pub const ID: &str = \"m\";\n");
        Files.writeString(mod.resolve("game_side.rs"), "pub fn ensure_init(game: &mut crate::game::Game<'_>) {\n}\n");
        Files.writeString(dir.resolve("compatibility.json"), "{\n  \"game\": \"26.3\",\n  \"hooks\": 1,\n  \"horizon_revision\": \"x\"\n}\n");
        return mod;
    }

    private static int count(String text, String part) {
        int n = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length())) n++;
        return n;
    }
}
