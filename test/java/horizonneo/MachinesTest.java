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
