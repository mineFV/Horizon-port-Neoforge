package horizonneo;

import horizonneo.EventTable.Level;
import horizonneo.EventTable.Match;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** The event table, on a made-up hook catalogue: how a label is read, and how sure each answer is. */
final class EventTableTest {
    private EventTableTest() {}

    private static final String T = "horizon_client::game::native_hooks::Game<'_>::native_";

    private static final String CATALOGUE = """
            { "helpers": [], "hooks": [
              { "name": "plain", "target": "%1$splain", "mod_api": { "contract": "listen" } },
              { "name": "destroy_block", "target": "%1$sdestroy_block", "neoforge": "BlockEvent.BreakEvent", "side": "server", "cancellable": true,
                "mod_api": { "contract": "cancel" } },
              { "name": "item_dropped", "target": "%1$sitem_dropped", "neoforge": "ItemTossEvent (only hears: a toss can't be canceled)", "mod_api": {} },
              { "name": "server_tick", "target": "%1$sserver_tick", "neoforge": "ServerTickEvent.Pre / .Post", "mod_api": {} },
              { "name": "render_hand", "target": "%1$srender_hand", "neoforge": "RenderHandEvent", "mod_api": {} },
              { "name": "render_hand_pose", "target": "%1$srender_hand_pose", "neoforge": "RenderHandEvent (PoseStack) / RenderArmEvent", "mod_api": {} },
              { "name": "frame", "target": "%1$sframe", "neoforge": "RenderFrameEvent.Pre", "mod_api": {} },
              { "name": "screen_render_post", "target": "%1$sscreen_render_post", "neoforge": "ScreenEvent.Render.Post", "mod_api": {} },
              { "name": "config_loading", "target": "%1$sconfig_loading", "neoforge": "ModConfigEvent.Loading", "mod_api": {} },
              { "name": "config_reloading", "target": "%1$sconfig_reloading", "neoforge": "ModConfigEvent.Reloading", "mod_api": {} },
              { "name": "config_unloading", "target": "%1$sconfig_unloading", "neoforge": "ModConfigEvent.Unloading", "mod_api": {} },
              { "name": "console_command", "target": "%1$sconsole_command",
                "neoforge": "none (Bukkit's ServerCommandEvent; NeoForge's CommandEvent hears parsed commands only)", "mod_api": {} },
              { "name": "player_chat", "target": "%1$splayer_chat", "neoforge": "net.neoforged.neoforge.event.ServerChatEvent", "mod_api": {} },
              { "name": "reload_listeners", "target": "%1$sreload_listeners", "neoforge": "AddServerReloadListenersEvent", "mod_api": {} },
              { "name": "menu_extra_data", "target": "%1$smenu_extra_data", "neoforge": "IPlayerExtension.openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)", "mod_api": {} },
              { "name": "block_items", "target": "%1$sblock_items", "neoforge": "Capabilities.Item.BLOCK", "mod_api": {} }
            ] }
            """.formatted(T);

    private static final String HOOK_LIST = """
            { "schema": 1, "hooks": [
              { "name": "native_destroy_block", "kind": "hook", "api": 14, "signature": "fn native_destroy_block(&mut self, pos: [i32; 3], canceled: &mut bool)",
                "doc": "A player breaks a block.", "bindings": [
                  { "name": "game", "ty": "&mut crate::game::Game<'_>", "value": "&mut *self" },
                  { "name": "pos", "ty": "[i32; 3]", "value": "pos" },
                  { "name": "canceled", "ty": "&mut bool", "value": "&mut *canceled" } ] },
              { "name": "native_item", "kind": "helper", "api": 3, "signature": "fn native_item(&self) -> u32" },
              { "name": "native_frame", "kind": "hook", "api": 1 }
            ] }
            """;

    private static final String FRAGMENT = """
            { "hooks": [
              { "name": "client_tick_post", "target": "%1$sclient_tick_post", "neoforge": "ClientTickEvent.Post", "mod_api": {} },
              { "name": "redstone", "target": "%1$sredstone", "neoforge": "none (NeoForge has no such event; NeighborNotifyEvent is close)", "mod_api": {} }
            ] }
            """.formatted(T);

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("the event table");

        // ---- reading a label ----
        c.same("one name", List.of("BlockEvent.BreakEvent"), EventTable.counterparts("BlockEvent.BreakEvent"));
        c.same("a name with a note", List.of("ItemTossEvent"), EventTable.counterparts("ItemTossEvent (only hears: a toss can't be canceled)"));
        c.same("a sibling written short", List.of("ServerTickEvent.Pre", "ServerTickEvent.Post"), EventTable.counterparts("ServerTickEvent.Pre / .Post"));
        c.same("two names, a note between", List.of("RenderHandEvent", "RenderArmEvent"), EventTable.counterparts("RenderHandEvent (PoseStack) / RenderArmEvent"));
        c.same("a package is dropped", List.of("ServerChatEvent"), EventTable.counterparts("net.neoforged.neoforge.event.ServerChatEvent (ICancellableEvent)"));
        c.same("a capability", List.of("Capabilities.Item.BLOCK"), EventTable.counterparts("Capabilities.Item.BLOCK"));
        c.same("what isn't an event isn't a name", List.of(), EventTable.counterparts("IPlayerExtension.openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)"));
        c.same("the events a `none` label mentions", List.of("ServerCommandEvent", "CommandEvent"),
                EventTable.mentioned("none (Bukkit's ServerCommandEvent; NeoForge's CommandEvent hears parsed commands only)"));
        c.same("a label's note", "only hears: a toss can't be canceled", EventTable.note("ItemTossEvent (only hears: a toss can't be canceled)"));
        c.same("no note", null, EventTable.note("BlockEvent.BreakEvent"));
        c.same("a note with brackets in it", "canceled is setCanPickup(TriState.FALSE); no forced pickup",
                EventTable.note("ItemEntityPickupEvent.Pre (canceled is setCanPickup(TriState.FALSE); no forced pickup)"));
        c.same("the package a label spells out", "net.neoforged.neoforge.event", EventTable.givenPackage("net.neoforged.neoforge.event.ServerChatEvent (ICancellableEvent)"));
        c.same("none spelled out", null, EventTable.givenPackage("PlayerEvent.Clone (not cancellable)"));
        c.yes("an event's name", EventTable.isName("ScreenEvent.Render.Post"));
        c.yes("not one", !EventTable.isName("NeoForge.EVENT_BUS") && !EventTable.isName("") && !EventTable.isName("ServerTickEvent.Pré"));

        // ---- how sure each answer is ----
        EventTable table = EventTable.build(CATALOGUE, HOOK_LIST, List.of("not JSON", FRAGMENT), List.of("tools/native-hook-fragments/broken.json", "tools/native-hook-fragments/client_tick.json"));
        c.same("hook points listed", 16, table.hooks());
        String events = "net/neoforged/neoforge/event/", client = "net/neoforged/neoforge/client/event/";

        Match exact = table.lookup(events + "level/BlockEvent$BreakEvent");
        c.same("named alone, with no note", Level.EXACT, exact.level());
        c.same("its hook point", "destroy_block", exact.hooks().get(0).name());
        c.same("the API it came in, from horizon-port's list", Integer.valueOf(14), exact.hooks().get(0).api());
        c.same("its parameters, from that list", List.of("game", "pos", "canceled"), exact.hooks().get(0).bindings().stream().map(EventTable.Binding::name).toList());
        c.same("its side, from the catalogue", "server", exact.hooks().get(0).side());
        c.same("in words", "Horizon's catalogue names `destroy_block` for it", exact.why());

        Match noted = table.lookup(events + "entity/item/ItemTossEvent");
        c.same("named with a note", Level.APPROXIMATE, noted.level());
        c.yes("the note is said", noted.why().contains("\"only hears: a toss can't be canceled\""));
        Match shared = table.lookup(events + "tick/ServerTickEvent$Post");
        c.same("a hook point shared by two events", Level.APPROXIMATE, shared.level());
        c.yes("that is said", shared.why().contains("for other events too"));
        c.same("an event two hook points are named for", Level.SEVERAL, table.lookup(client + "RenderHandEvent").level());
        c.same("both are given", List.of("render_hand", "render_hand_pose"), table.lookup(client + "RenderHandEvent").hooks().stream().map(EventTable.Hook::name).toList());
        Match parent = table.lookup("net/neoforged/fml/event/config/ModConfigEvent");
        c.same("a parent whose events all have hook points", Level.SEVERAL, parent.level());
        c.same("all of them", 3, parent.hooks().size());
        Match otherPhase = table.lookup(client + "RenderFrameEvent$Post");
        c.same("only the other phase has one", Level.PHASE, otherPhase.level());
        c.yes("which is said", otherPhase.why().contains("only for RenderFrameEvent.Pre (`frame`)"));
        c.same("only an event nested in it has one", Level.PHASE, table.lookup(client + "ScreenEvent$Render").level());
        c.same("a hook point without a signature is still the event's", Level.EXACT, table.lookup(client + "RenderFrameEvent$Pre").level());
        c.same("and has no parameters", List.of(), table.lookup(client + "RenderFrameEvent$Pre").hooks().get(0).bindings());
        Match pending = table.lookup(client + "ClientTickEvent$Post");
        c.same("a hook point that is being prepared", Level.PENDING, pending.level());
        c.yes("with the file that says so", pending.why().contains("tools/native-hook-fragments/client_tick.json") && pending.hooks().get(0).fragment() != null);
        c.same("the other phase of one being prepared has nothing", Level.NONE, table.lookup(client + "ClientTickEvent$Pre").level());
        Match none = table.lookup(events + "CommandEvent");
        c.same("an event a label says has no counterpart", Level.NOTED, none.level());
        c.yes("with the label", none.why().contains("`console_command` is \"none (Bukkit's ServerCommandEvent"));
        c.same("a fragment's `none` label prepares nothing", Level.NONE, table.lookup(events + "level/NeighborNotifyEvent").level());
        c.same("an event vanilla made into data", Level.DATA, table.lookup(events + "village/VillagerTradesEvent").level());
        c.same("an event the catalogue doesn't know", Level.NONE, table.lookup("net/neoforged/fml/event/lifecycle/FMLClientSetupEvent").level());
        c.same("an event of the mod's own", Level.OWN, table.lookup("com/example/mod/ServerTickEvent$Post").level());
        c.same("a class of that name in another package isn't it", Level.NONE, table.lookup("net/neoforged/neoforge/client/event/ServerChatEvent").level());
        c.same("the one in the package the label spells out is", Level.EXACT, table.lookup(events + "ServerChatEvent").level());
        Match renamed = table.lookup(events + "AddReloadListenerEvent");
        c.same("an event NeoForge renamed is never the surest answer", Level.APPROXIMATE, renamed.level());
        c.same("its new name", "AddServerReloadListenersEvent", renamed.newName());
        c.same("a capability under its old name", "Capabilities.Item.BLOCK", table.lookup("net/neoforged/neoforge/capabilities/Capabilities$ItemHandler$BLOCK").newName());

        // ---- what it can't be built from ----
        c.refuses("a catalogue that isn't JSON", "native-hooks.json can't be read", () -> EventTable.build("nope", null, List.of(), List.of()));
        c.refuses("a catalogue with no hook point", "lists no hook point", () -> EventTable.build("{\"hooks\": []}", null, List.of(), List.of()));
        c.refuses("a hook list that isn't JSON", "isn't JSON", () -> EventTable.build(CATALOGUE, "nope", List.of(), List.of()));
        c.same("no hook list: the answers stand, without signatures", null, EventTable.build(CATALOGUE, null, List.of(), List.of()).lookup(events + "level/BlockEvent$BreakEvent").hooks().get(0).signature());

        // ---- from a source folder ----
        Path dir = Files.createTempDirectory("table-test-");
        try {
            c.refuses("a folder with no catalogue", "has no native-hooks.json", () -> EventTable.read(dir, null));
            Files.writeString(dir.resolve("native-hooks.json"), CATALOGUE);
            c.same("a source with no fragments", Level.NONE, EventTable.read(dir, HOOK_LIST).lookup(client + "ClientTickEvent$Post").level());
            Path fragments = Files.createDirectories(dir.resolve("tools/native-hook-fragments"));
            Files.writeString(fragments.resolve("README.md"), "not a fragment");
            Files.writeString(fragments.resolve("client_tick.json"), FRAGMENT);
            c.same("its fragments are read", Level.PENDING, EventTable.read(dir, HOOK_LIST).lookup(client + "ClientTickEvent$Post").level());
        } finally {
            Checks.delete(dir);
        }
        System.exit(c.done());
    }
}
