package horizonneo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Declarations for blocks no vanilla template takes, from made-up facts: nothing here is a real mod's or the game's. */
final class DeclarationsTest {
    static final List<Object> PLAIN = List.of(Declarations.BLOCK, Declarations.BEHAVIOUR);
    static final Map<String, Object> STONE_SOUND = Map.of("volume", 1.0, "pitch", 1.0, "break", "minecraft:block.stone.break");
    static final Map<String, Object> GLASS_SOUND = Map.of("volume", 1.0, "pitch", 1.0, "break", "minecraft:block.glass.break");

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("Declarations");
        Map<String, Object> blocks = new LinkedHashMap<>();
        blocks.put("minecraft:air", vanilla(STONE_SOUND, 1.0));
        blocks.put("minecraft:stone", vanilla(STONE_SOUND, 1.0));
        blocks.put("minecraft:glass", vanilla(GLASS_SOUND, 1.0));
        blocks.put("minecraft:soul_sand", vanilla(STONE_SOUND, 0.4));
        blocks.put("minecraft:oak_stairs", Map.of("class", List.of("net.minecraft.world.level.block.StairBlock", Declarations.BLOCK, Declarations.BEHAVIOUR),
                "properties", Map.of("facing", List.of("north")), "default", "facing=north", "speed_factor", 1.0, "jump_factor", 1.0,
                "states", Map.of("facing=north", state(0, true, STONE_SOUND, CUBE(), CUBE(), false))));

        // A machine: two properties, lit when active, a slab-like shape when facing south, one box too many when active and east.
        Map<String, Object> states = new LinkedHashMap<>();
        for (String active : List.of("true", "false")) {
            for (String facing : List.of("north", "south", "east")) {
                List<Object> outline = facing.equals("south") ? List.of(List.of(0.0, 0.0, 0.0, 1.0, 0.5, 1.0)) : CUBE();
                states.put("active=" + active + ",facing=" + facing, state(active.equals("true") ? 13 : 0, true, GLASS_SOUND, outline, outline, false));
            }
        }
        blocks.put("m:machine", modBlock(List.of("com.example.Machine", Declarations.BLOCK, Declarations.BEHAVIOUR),
                Map.of("active", List.of("true", "false"), "facing", List.of("north", "south", "east")), "active=false,facing=north", states, "m:machine"));
        List<Object> many = new ArrayList<>();
        for (int i = 0; i < 17; i++) many.add(List.of(0.0, 0.0, 0.0, 1.0, (i + 1) / 17.0, 1.0));
        blocks.put("m:crowded", modBlock(PLAIN, Map.of(), "", Map.of("", state(0, false, Map.of("break", "m:own.break"), many, CUBE(), true)), "minecraft:air"));
        blocks.put("m:stair", modBlock(List.of("com.example.Stair", "net.minecraft.world.level.block.StairBlock", Declarations.BLOCK, Declarations.BEHAVIOUR),
                Map.of(), "", Map.of("", state(0, true, STONE_SOUND, CUBE(), CUBE(), false)), "m:stair"));
        Map<String, Object> sticky = new LinkedHashMap<>(modBlock(PLAIN, Map.of(), "", Map.of("", state(0, true, STONE_SOUND, CUBE(), CUBE(), false)), "m:sticky"));
        sticky.put("speed_factor", 0.2);
        blocks.put("m:sticky", sticky);

        Map<String, Object> items = Map.of(
                "m:machine", Map.of("class", "net.minecraft.world.item.BlockItem", "max_stack_size", 16L, "rarity", "rare", "block", "m:machine",
                        "components", Map.of("minecraft:max_stack_size", 16L, "m:energy", Map.of())));
        Map<String, Object> facts = Map.of("blocks", blocks, "items", items);

        c.same("plain templates: Block itself with no state property, not air", List.of("minecraft:glass", "minecraft:soul_sand", "minecraft:stone"),
                Declarations.plainTemplates(blocks));
        c.same("sounds: the first vanilla block by id with the same events, whatever its class, not air", "minecraft:oak_stairs", Declarations.sounds(STONE_SOUND, blocks));
        c.same("sounds no vanilla block has", null, Declarations.sounds(Map.of("break", "m:own.break"), blocks));

        Path dir = Files.createTempDirectory("declarations-test-");
        try {
            Declarations.Made made = Declarations.declare(facts, dir, List.of("m:machine", "m:crowded", "m:stair", "m:sticky", "m:gone"));
            c.same("declared: the plain ones", List.of("m:machine", "m:crowded"), made.declared());
            c.yes("a block of a vanilla subclass stays left out, with why", String.valueOf(made.left().get("m:stair")).contains("StairBlock"));
            c.yes("so does one no plain template has the factors of", String.valueOf(made.left().get("m:sticky")).contains("speed and jump factors"));
            c.same("and one the facts don't have", "not in the facts", made.left().get("m:gone"));

            Map<String, Object> machine = read(dir.resolve("data/m/horizon/block/machine.json"));
            c.same("the template whose sounds are its own", "minecraft:glass", machine.get("template"));
            c.same("its properties, as Horizon names them", 3.5, Json.object(machine.get("properties")).get("hardness"));
            c.same("its sounds", "minecraft:glass", Json.object(machine.get("properties")).get("sounds"));
            c.same("its own state properties: a boolean, and an enum in the facts' order",
                    Map.of("active", Map.of("type", "boolean"), "facing", Map.of("type", "enum", "values", List.of("north", "south", "east"))),
                    machine.get("state_properties"));
            c.same("its default state", Map.of("active", "false", "facing", "north"), machine.get("default_state"));
            c.same("light by state: on a tie, the default state's as default, the others by both properties", Map.of("properties", List.of("active", "facing"),
                    "values", Map.of("true,east", 13L, "true,north", 13L, "true,south", 13L), "default", 0L), machine.get("state_light"));
            c.same("its outline by state", Map.of("properties", List.of("active", "facing"),
                    "values", Map.of("false,south", List.of(List.of(0L, 0L, 0L, 1L, 0.5, 1L)), "true,south", List.of(List.of(0L, 0L, 0L, 1L, 0.5, 1L))),
                    "default", List.of(List.of(0L, 0L, 0L, 1L, 1L, 1L))), roundNumbers(machine.get("shapes")));
            c.same("no collision table when it is the outline", null, machine.get("collision_shapes"));
            c.same("its item's stack size and rarity", Map.of("minecraft:max_stack_size", 16L, "minecraft:rarity", "rare"), machine.get("components"));
            List<String> notes = made.notes().get("m:machine");
            c.yes("its class's code is said", notes.stream().anyMatch(n -> n.contains("com.example.Machine")));
            c.yes("and its item's own components", notes.stream().anyMatch(n -> n.contains("m:energy")));

            Map<String, Object> crowded = read(dir.resolve("data/m/horizon/block/crowded.json"));
            c.same("more than 16 boxes: no outline, said", null, crowded.get("shapes"));
            c.yes("said", made.notes().get("m:crowded").stream().anyMatch(n -> n.contains("up to 17 boxes")));
            c.same("its collision, a full cube, isn't written either", null, crowded.get("collision_shapes"));
            c.same("random ticks in every state", true, Json.object(crowded.get("properties")).get("random_ticks"));
            c.same("occludes nothing", false, Json.object(crowded.get("properties")).get("occludes"));
            c.yes("sounds no vanilla block has: the template's are kept, and said", !Json.object(crowded.get("properties")).containsKey("sounds")
                    && made.notes().get("m:crowded").stream().anyMatch(n -> n.contains("m:own.break")));
            c.same("its item isn't one of the same id: none", false, crowded.get("item"));
            c.yes("no file for a block left out", !Files.exists(dir.resolve("data/m/horizon/block/stair.json")));
        } finally {
            Checks.delete(dir);
        }
        System.exit(c.done());
    }

    static List<Object> CUBE() {
        return List.of(List.of(0.0, 0.0, 0.0, 1.0, 1.0, 1.0));
    }

    static Map<String, Object> vanilla(Map<String, Object> sound, double speed) {
        return Map.of("class", PLAIN, "properties", Map.of(), "default", "", "speed_factor", speed, "jump_factor", 1.0,
                "states", Map.of("", state(0, true, sound, CUBE(), CUBE(), false)));
    }

    static Map<String, Object> modBlock(List<Object> classes, Map<String, Object> properties, String def, Map<String, Object> states, String item) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("class", classes);
        b.put("properties", properties);
        b.put("default", def);
        b.put("item", item);
        b.put("explosion_resistance", 6.0);
        b.put("friction", 0.6);
        b.put("speed_factor", 1.0);
        b.put("jump_factor", 1.0);
        b.put("states", states);
        return b;
    }

    static Map<String, Object> state(int light, boolean occludes, Map<String, Object> sound, List<Object> outline, List<Object> collision, boolean ticks) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("hardness", 3.5);
        s.put("emission", (long) light);
        s.put("requires_tool", true);
        s.put("can_occlude", occludes);
        s.put("random_ticks", ticks);
        s.put("map_color_id", 11L);
        s.put("sound", sound);
        s.put("outline", outline);
        s.put("collision", collision);
        return s;
    }

    static Map<String, Object> read(Path file) throws Exception {
        return Json.object(Json.parse(Files.readString(file, StandardCharsets.UTF_8)));
    }

    /** JSON reads 1.0 back as 1: numbers that are whole compare as Long. */
    static Object roundNumbers(Object o) {
        if (o instanceof Double d && d == Math.rint(d)) return d.longValue();
        if (o instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.forEach((k, v) -> out.put(String.valueOf(k), roundNumbers(v)));
            return out;
        }
        if (o instanceof List<?> l) return l.stream().map(DeclarationsTest::roundNumbers).toList();
        return o;
    }
}
