package horizonneo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The machines' helpers, from made-up machines facts: nothing here is a real mod's. */
final class MachinesTest {
    public static void main(String[] args) throws Exception {
        Checks c = new Checks("Machines");
        Map<String, Map<String, Object>> blocks = new TreeMap<>();
        blocks.put("m:press", block("m:press_type", true, Map.of(
                "slots", 2L,
                "faces", Map.of(
                        "up", Map.of("slots", 1L, "reaches", List.of(0L), "inputs", List.of(0L), "outputs", List.of()),
                        "north", Map.of("slots", 1L, "reaches", List.of(0L), "inputs", List.of(), "outputs", List.of()),
                        "down", Map.of("slots", 1L, "reaches", List.of(1L), "inputs", List.of(), "outputs", List.of(1L)))),
                Map.of("unsided", Map.of("capacity", 1000L, "insert_at_most", 0L),
                        "faces", Map.of("west", Map.of("capacity", 1000L, "insert_at_most", 100L, "extract_at_most", 0L),
                                "east", Map.of("capacity", 1000L, "insert_at_most", 50L, "extract_at_most", 40L)),
                        "filled", 40L)));
        blocks.put("m:lamp", block("m:lamp_type", false, Map.of(), Map.of()));
        Map<String, Object> crate = block("m:crate_type", false, Map.of("slots", 300L, "faces", Map.of()), Map.of());
        blocks.put("m:crate", crate);
        Map<String, Object> broken = new LinkedHashMap<>(block(null, false, Map.of(), Map.of()));
        broken.put("unread", Map.of("block_entity", "java.lang.IllegalStateException: no level"));
        blocks.put("m:broken", broken);

        Map<String, List<String>> notes = new TreeMap<>();
        Machines.Body entity = Machines.body("block_entity", blocks, notes);
        c.same("block_entity: each block whose type was read", 3, entity.count());
        c.yes("its type", entity.text().contains("\"m:press\" => \"m:press_type\","));
        c.yes("one not read is said, with why", notes.get("m:broken").get(0).contains("no level"));

        Machines.Body ticks = Machines.body("block_entity_ticks", blocks, notes);
        c.same("ticks: only those NeoForge ticks", 1, ticks.count());
        c.yes("set for them", ticks.text().contains("\"m:press\"") && !ticks.text().contains("\"m:lamp\""));

        Machines.Body container = Machines.body("block_entity_container", blocks, notes);
        c.same("a container for the block with slots that fit; the one with too many is commented out", 1, container.count());
        c.yes("faces in Direction's order: down, up, north, south, west, east",
                container.text().contains("faces: [vec![1], vec![0], vec![0], Vec::new(), Vec::new(), Vec::new()],"));
        c.yes("inputs and outputs, from every face", container.text().contains("inputs: vec![0],") && container.text().contains("outputs: vec![1],"));
        c.yes("too many slots: commented out", container.text().contains("        // \"m:crate\" => Some("));
        c.yes("and said", notes.get("m:crate").stream().anyMatch(n -> n.contains("300 item slots")));
        c.yes("a slot that takes items through one face of those that reach it is said",
                notes.get("m:press").stream().anyMatch(n -> n.contains("slot 0 takes items through up only") && n.contains("north, up")));
        c.yes("no menu, said", notes.get("m:press").stream().anyMatch(n -> n.contains("MenuProvider")));

        Machines.Body energy = Machines.body("block_entity_energy", blocks, notes);
        c.same("energy: the press", 1, energy.count());
        c.yes("the highest rate in, from the faces that took some", energy.text().contains("max_insert: 100,"));
        c.yes("the faces that took and gave", energy.text().contains("insert_faces: [false, false, false, false, true, true],")
                && energy.text().contains("extract_faces: [false, false, false, false, false, true],"));
        c.yes("faces at different rates are said", notes.get("m:press").stream().anyMatch(n -> n.contains("different rates")));
        c.yes("an extract measured on a buffer not filled up is said", notes.get("m:press").stream().anyMatch(n -> n.contains("filled to 40 of 1000")));

        c.same("no fluids: nothing written", 0, Machines.body("block_entity_fluids", blocks, notes).count());

        EventTable.Hook hook = new EventTable.Hook("block_entity_ticks", "x::native_block_entity_ticks", "block_entity_ticks", null, 0, null, null, null, 15, null,
                "fn native_block_entity_ticks(&self, block: &str, on: &mut bool)",
                List.of(new EventTable.Binding("game", "&crate::game::Game<'_>", "&*self"), new EventTable.Binding("block", "&str", "block"),
                        new EventTable.Binding("on", "&mut bool", "&mut *on")), null);
        c.yes("a hook point without the parameter a helper sets isn't used", String.valueOf(Machines.unusable(hook, ticks.uses())).contains("no parameter `ticks`"));

        c.same("Rust strings are quoted", "\"a\\\"b\"", Machines.rust("a\"b"));
        // The game's ids dump read (the facts' items, the machines file's game_ids) are what the bridge gives the
        // stand-ins before the mod loads; the fluids observed are given when the machines file has none.
        Map<String, Object> machines = new LinkedHashMap<>();
        machines.put("vanilla_items", List.of("minecraft:air", "minecraft:stone"));
        machines.put("game_ids", Map.of("component", "minecraft:damage", "components", "minecraft:stone=max_stack_size:64,other"));
        Map<String, String> given = Machines.given(machines);
        c.same("items from the facts", "minecraft:air,minecraft:stone", given.get("item"));
        c.same("components from the machines file", "minecraft:stone=max_stack_size:64,other", given.get("components"));
        c.same("no fluids there", null, given.get("fluid"));
        Map<String, String> f = new LinkedHashMap<>();
        for (String[] m : BridgeRust.NEEDED) f.put(m[0], "f_" + m[0]);
        String module = BridgeRust.module("super::java", ModInfo.minimal("m"), f, given, false);
        c.yes("its configs loaded new when Horizon doesn't hand their files over", module.contains("const CONFIG_FILES: bool = false;"));
        c.yes("and read from them when it does", BridgeRust.module("super::java", ModInfo.minimal("m"), f, given, true).contains("const CONFIG_FILES: bool = true;"));
        Machines.Body register = Machines.configBody("register_configs", "port_m_bridge", "m");
        c.yes("each config listed for Horizon, in its kind", register.text().contains("crate::game::port_m_bridge::configs()") && register.text().contains("crate::game::NativeConfig::new(\"m\", kind, &text).file(&file)"));
        c.yes("a file read handed to the bridge, the mod's own", Machines.configBody("config_reloading", "port_m_bridge", "m").text().contains("if config.mod_id == \"m\" {\n        crate::game::port_m_bridge::config(&config.file, &config.text, true);"));
        c.yes("the bridge gives the items", module.contains("j::f_known(text(\"item\"), text(\"minecraft:air,minecraft:stone\"))"));
        c.yes("and the items' components", module.contains("j::f_known(text(\"components\"), text(\"minecraft:stone=max_stack_size:64,other\"))"));
        c.yes("and the fluids observed", module.contains("j::f_known(text(\"fluid\"), text(" + Machines.rust(BridgeRust.FLUIDS) + "))"));
        c.same("nothing given without the machines file's ids", Map.of(), Machines.given(Map.of()));
        // The facts' tags (those holding the mod's values, and the "c" ones) and namespaces, as Port puts them in.
        Map<String, Object> tagged = new LinkedHashMap<>();
        tagged.put("tags", Map.of("item", new java.util.TreeMap<>(Map.of("c:ingots/osmium", List.of("m:ingot"), "minecraft:beacon_payment_items", List.of("m:ingot")))));
        tagged.put("namespaces", List.of("m"));
        tagged.put("block_ids", List.of("minecraft:air", "m:press"));
        Map<String, String> tags = Machines.given(tagged);
        c.same("the items' tags", "c:ingots/osmium=m:ingot;minecraft:beacon_payment_items=m:ingot", tags.get("tags_item"));
        c.same("no blocks' tags there", null, tags.get("tags_block"));
        c.same("the mod's namespaces", "m", tags.get("namespaces"));
        c.same("every block's id", "minecraft:air,m:press", tags.get("block"));
        c.same("the furnace fuels from the machines file", "minecraft:coal=1600;m:pellet=200",
                Machines.given(Map.of("game_ids", Map.of("furnace_fuels", "minecraft:coal=1600;m:pellet=200"))).get("furnace_fuels"));
        Map<String, String> blockItems = Machines.given(Map.of("game_ids", Map.of("item_blocks", "minecraft:redstone=minecraft:redstone_wire",
                "block_items", "minecraft:redstone_wire=minecraft:redstone", "remainders", "minecraft:lava_bucket=minecraft:bucket")));
        c.same("the game's block items from the machines file", "minecraft:redstone=minecraft:redstone_wire", blockItems.get("item_blocks"));
        c.same("and its blocks' items", "minecraft:redstone_wire=minecraft:redstone", blockItems.get("block_items"));
        c.same("and its items' remainders", "minecraft:lava_bucket=minecraft:bucket", blockItems.get("remainders"));
        System.exit(c.done());
    }

    static Map<String, Object> block(String type, boolean ticks, Map<String, Object> items, Map<String, Object> energy) {
        Map<String, Object> b = new LinkedHashMap<>();
        if (type != null) b.put("block_entity", type);
        b.put("ticks_on_server", ticks);
        b.put("saved", List.of("progress"));
        if (!items.isEmpty()) b.put("items", items);
        if (!energy.isEmpty()) b.put("energy", energy);
        b.put("unread", Map.of());
        return b;
    }
}
