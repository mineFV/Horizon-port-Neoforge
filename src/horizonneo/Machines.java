package horizonneo;

import horizonneo.EventTable.Binding;
import horizonneo.EventTable.Hook;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The mod's machines on Horizon's block-entity hook points, from what NeoForge gave of them in a server (`dump`'s
 * machines file, written by this tool's exporter beside Horizon's oracle).
 *
 * What NeoForge gave is certain, and switched on: which block has a block entity and of which type
 * (`block_entity`), which tick on the server (`block_entity_ticks`), a container's slots and what each face
 * reaches, takes and gives (`block_entity_container`), an energy buffer's capacity, limits and faces
 * (`block_entity_energy`), fluid tanks' capacities (`block_entity_fluids`). What the block entity does is its own
 * code: `block_entity_tick` is on, and does nothing until its TODO is filled in, as a listener's helper does.
 * What wasn't measured stays out, said in a comment: a face slot no item went into, fluid tanks' faces, menus and
 * screens.
 *
 * The facts are those of a block placed alone, in its default state, as new: a block entity whose sides depend on
 * its facing or its settings (Mekanism's side configuration) is read as it starts.
 */
final class Machines {
    private Machines() {}

    /** The hook points written, in order. */
    static final List<String> HOOKS = List.of("block_entity", "block_entity_ticks", "block_entity_tick", "block_entity_container",
            "block_entity_energy", "block_entity_fluids");

    /** Horizon's API that gives a block a block entity whatever its template (the declared machines are on plain ones). */
    static final int ANY_TEMPLATE = 129;

    /** Horizon's limits (`native_block_entity_container`, `native_block_entity_fluids`). */
    static final int MAX_SLOTS = 256, MAX_TANKS = 16, MAX_TANK = 1_000_000;

    /** NeoForge's Direction order, which is Horizon's for faces. */
    static final List<String> FACES = List.of("down", "up", "north", "south", "west", "east");

    /**
     * What was written. on: hook point to its injection id; off: hook point to why it is commented out. counts:
     * hook point to how many blocks it answers for. notes: by block, what isn't carried over. menus: blocks whose
     * menu isn't opened, with why. types: block entity types. bridged: the ticking types the bridge runs, with their
     * block; bridgeProblem: why there is no bridge, or null.
     */
    record Made(Map<String, String> on, Map<String, String> off, Map<String, Integer> counts, Map<String, List<String>> notes, int blocks, int types,
                int menuTypes, Map<String, String> bridged, String bridgeProblem) {}

    static Map<String, Object> read(Path file) throws IOException {
        Map<String, Object> m;
        try {
            m = Json.object(Json.parse(Files.readString(file, StandardCharsets.UTF_8)));
        } catch (Json.Invalid e) {
            throw new IOException(file + " isn't JSON: " + e.getMessage());
        }
        if (Json.text(m, "failed") != null) throw new IOException(file + ": the machines exporter failed in NeoForge (" + Json.text(m, "failed") + ")");
        return m;
    }

    /** Writes the helpers and their injections into the bundle's mod folder, and the hooks API they need. */
    static Made apply(Path mod, String modId, Map<String, Object> machines, EventTable table) throws IOException {
        return apply(mod, modId, machines, table, null);
    }

    /**
     * java: this tool's translation of the jar (JarRust), or null. When it has what the bridge needs (BridgeRust), the
     * bundle gets it as two modules, the translation and Horizon's side of the bridge, and the tick helper runs the
     * block entities whose type one block makes through it.
     */
    static Made apply(Path mod, String modId, Map<String, Object> machines, EventTable table, JarRust.Result java) throws IOException {
        bridgeProblem = null;
        Path manifestFile = mod.resolve("horizon-mixin.toml");
        String manifest = Files.readString(manifestFile, StandardCharsets.UTF_8);
        Map<String, Object> root;
        try {
            root = Toml.parse(manifest).root();
        } catch (Toml.Problem e) {
            throw new Bundle.Unexpected("horizon-mixin.toml can't be read: " + e.getMessage());
        }
        String module = null;
        for (Map<String, Object> m : Toml.tables(root.get("module"))) if ("runtime".equals(Toml.text(m, "id"))) module = Toml.text(m, "name");
        if (module == null) throw new Bundle.Unexpected("no [[module]] with the id `runtime`");
        Map<String, String> taken = new LinkedHashMap<>();
        for (Map<String, Object> i : Toml.tables(root.get("injection"))) if (Toml.text(i, "target") != null) taken.put(Toml.text(i, "target"), Toml.text(i, "id"));
        Set<String> ids = new HashSet<>();
        Matcher used = Pattern.compile("(?m)^(?:# )?id = \"([^\"]+)\"$").matcher(manifest);
        while (used.find()) ids.add(used.group(1));

        Map<String, Map<String, Object>> blocks = new TreeMap<>();
        Json.object(machines.get("blocks")).forEach((k, v) -> blocks.put(k, Json.object(v)));
        Map<String, List<String>> notes = new TreeMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> on = new LinkedHashMap<>(), off = new LinkedHashMap<>();
        StringBuilder added = new StringBuilder();
        int api = 0;
        String function = modId.replaceAll("[^A-Za-z0-9_]", "_");
        // The bridge: which ticking types it runs (each made by one block, as `dump` found it), as modules of the bundle.
        Map<String, String> bridged = new TreeMap<>();
        String bridgeModule = null;
        List<String> missing = new ArrayList<>();
        Map<String, String> functions = java == null ? null : BridgeRust.functions(java, missing);
        if (java != null && functions == null) bridgeProblem = "the translation lacks " + String.join(", ", missing);
        if (functions != null) {
            Map<String, List<String>> byType = new TreeMap<>();
            blocks.forEach((id, m) -> {
                String type = Json.text(m, "block_entity");
                if (type != null && Boolean.TRUE.equals(m.get("ticks_on_server"))) byType.computeIfAbsent(type, k -> new ArrayList<>()).add(id);
            });
            byType.forEach((type, made) -> {
                if (made.size() == 1) bridged.put(type, made.get(0));
                else made.forEach(b -> note(notes, b, "its block entity type is made by " + made.size() + " blocks: which one is at a position isn't told by the hook point, so it doesn't tick"));
            });
            bridgeModule = module + "_bridge";
            Files.writeString(mod.resolve("java.rs"), java.rust(), StandardCharsets.UTF_8);
            Files.writeString(mod.resolve("bridge.rs"), BridgeRust.module("super::" + module + "_java", modId, functions), StandardCharsets.UTF_8);
        }
        for (String name : HOOKS) {
            Body body = name.equals("block_entity_tick") && !bridged.isEmpty() ? bridgedTick(bridged, bridgeModule, blocks) : body(name, blocks, notes);
            if (body.count() == 0) continue;
            counts.put(name, body.count());
            Hook hook = table.named(name);
            String why = hook == null ? "this Horizon source's catalogue has no `" + name + "`" : unusable(hook, body.uses());
            if (why == null && taken.containsKey(hook.target())) why = "the hook point already has an injection, `" + taken.get(hook.target()) + "`: merge this helper into its own";
            String id = Wiring.unique(name.replace('_', '-'), ids);
            String helperText = helper(name, hook, body, function + "_" + name, module);
            if (name.equals("block_entity") && !bridged.isEmpty() && bridged.size() == typesTicking(blocks)) {
                // Every ticking type runs through the bridge: an empty compound is what a new block entity starts from.
                helperText = helperText.replace(BLOCK_ENTITY_TODO, "    // It starts empty: the bridge (bridge.rs) makes the mod's own block entity from it, and saves back what the mod saves.\n");
            }
            Files.writeString(mod.resolve(id + ".rs"), helperText, StandardCharsets.UTF_8);
            if (why != null) {
                off.put(name, why);
                added.append("\n# Off: the mod's machines on `").append(name).append("` (").append(id).append(".rs).\n#   ").append(sentence(why))
                        .append("\n#   TODO: put the helper on the hook point by hand, then uncomment this.\n# [[injection]]\n# id = \"").append(id)
                        .append("\"\n# kind = \"HEAD\"\n# target = \"").append(hook == null || hook.target() == null ? "TODO" : hook.target()).append("\"\n# helper = \"")
                        .append(id).append(".rs\"\n");
                continue;
            }
            on.put(name, id);
            if (hook.api() != null) api = Math.max(api, hook.api());
            if (name.equals("block_entity")) api = Math.max(api, ANY_TEMPLATE);
            added.append("\n[[injection]]\nid = \"").append(id).append("\"\nkind = \"HEAD\"\ntarget = \"").append(hook.target())
                    .append("\"\nhelper = \"").append(id).append(".rs\"\nbindings = [\n");
            for (Binding b : hook.bindings()) {
                added.append("  { name = \"").append(b.name()).append("\", ty = \"").append(b.type()).append("\", value = \"").append(b.value()).append("\" },\n");
            }
            added.append("]\n");
        }
        if (bridgeModule != null) {
            added.insert(0, "\n# The mod's Java translated by horizon-port-neoforge, and Horizon's side of the bridge to it (BridgeRust).\n"
                    + "[[module]]\nid = \"java\"\nfile = \"crates/horizon-client/src/game.rs\"\nname = \"" + module + "_java\"\nsource = \"java.rs\"\n\n"
                    + "[[module]]\nid = \"bridge\"\nfile = \"crates/horizon-client/src/game.rs\"\nname = \"" + bridgeModule + "\"\nsource = \"bridge.rs\"\n");
        }
        if (added.length() > 0) {
            String text = manifest.stripTrailing() + "\n\n# The mod's machines, put on Horizon's block-entity hook points by horizon-port-neoforge from what NeoForge gave\n"
                    + "# of them in a server (`dump`). Their helpers carry over what was measured; what the block entities do is a TODO.\n" + added;
            Files.writeString(manifestFile, text, StandardCharsets.UTF_8);
        }
        if (api > 0) Wiring.needs(mod.getParent().getParent().resolve("compatibility.json"), api);
        return new Made(on, off, counts, notes, blocks.size(), Json.object(machines.get("block_entity_types")).size(), Json.array(machines.get("menu_types")).size(),
                bridged, bridgeProblem);
    }

    static final String BLOCK_ENTITY_TODO = "    // TODO: what each block entity saves isn't ported: it starts empty (report/machines.md lists what NeoForge's saved).\n";

    /** How many block entity types tick on the server. */
    private static int typesTicking(Map<String, Map<String, Object>> blocks) {
        Set<String> types = new java.util.TreeSet<>();
        blocks.values().forEach(m -> {
            if (Json.text(m, "block_entity") != null && Boolean.TRUE.equals(m.get("ticks_on_server"))) types.add(Json.text(m, "block_entity"));
        });
        return types.size();
    }

    /** Why the bridge wasn't written, when a translation was given; null otherwise. */
    static String bridgeProblem;

    /** The tick helper's body when the bridge runs some types: each type's block, then the bridge's tick. */
    static Body bridgedTick(Map<String, String> bridged, String bridgeModule, Map<String, Map<String, Object>> blocks) {
        StringBuilder b = new StringBuilder();
        b.append("    // Each block entity type the bridge runs, and the block that makes it (one each, as `dump` found them in NeoForge):\n");
        b.append("    // the mod's own Java, translated, ticks it (bridge.rs).\n");
        b.append("    let block = match entity.id.as_str() {\n");
        bridged.forEach((type, block) -> b.append("        ").append(rust(type)).append(" => ").append(rust(block)).append(",\n"));
        b.append("        _ => return,\n    };\n");
        b.append("    let id = entity.id.clone();\n");
        b.append("    // What the mod's Java asks of the world, answered from this level (bridge.rs, HorizonWorld): \"block x y z\", the\n");
        b.append("    // state there as \"id[key=value,...]\"; \"set x y z key value flags\", a property of the block there set as\n");
        b.append("    // Level.setBlock does (\"true\" when it changed).\n");
        b.append("    let mut world = |question: &[u16]| -> Option<Vec<u16>> {\n");
        b.append("        let question = String::from_utf16_lossy(question);\n");
        b.append("        let words: Vec<&str> = question.split(' ').collect();\n");
        b.append("        let answer = match words.as_slice() {\n");
        b.append("            [\"block\", x, y, z] => {\n");
        b.append("                let (x, y, z) = (x.parse().ok()?, y.parse().ok()?, z.parse().ok()?);\n");
        b.append("                let state = game.pack.assets.registry.state(game.world.block(x, y, z))?;\n");
        b.append("                let properties: Vec<String> = state.properties.iter().map(|(k, v)| format!(\"{}={}\", k, v)).collect();\n");
        b.append("                if properties.is_empty() { state.name.to_string() } else { format!(\"{}[{}]\", state.name, properties.join(\",\")) }\n");
        b.append("            }\n");
        b.append("            [\"set\", x, y, z, key, value, flags] => {\n");
        b.append("                let at = [x.parse().ok()?, y.parse().ok()?, z.parse().ok()?];\n");
        b.append("                game.native_set_block_property_with(at, key, value, flags.parse().ok()?).to_string()\n");
        b.append("            }\n");
        b.append("            _ => return None,\n        };\n");
        b.append("        Some(answer.encode_utf16().collect())\n    };\n");
        b.append("    if let Some(saved) = crate::game::").append(bridgeModule).append("::tick(block, &id, pos, &entity.nbt, &mut world) {\n");
        b.append("        entity.nbt = saved;\n    }\n");
        return new Body(b.toString(), List.of("game", "pos", "entity"), bridged.size());
    }

    /** Why a helper can't be put on the hook point; null when it can. */
    static String unusable(Hook hook, List<String> uses) {
        String why = Wiring.unusable(hook);
        if (why != null) return why;
        List<String> names = hook.bindings().stream().map(Binding::name).toList();
        for (String u : uses) {
            if (!names.contains(u)) return "`" + hook.name() + "` has no parameter `" + u + "` in this Horizon source (" + hook.signature() + "), which its helper sets";
        }
        return null;
    }

    // ---- the helpers' bodies ----

    /** A helper's body, the parameters it uses besides the game, and how many blocks it answers for. */
    record Body(String text, List<String> uses, int count) {}

    static Body body(String hook, Map<String, Map<String, Object>> blocks, Map<String, List<String>> notes) {
        StringBuilder b = new StringBuilder();
        int count = 0;
        switch (hook) {
            case "block_entity" -> {
                b.append("    if entity.is_some() {\n        return;\n    }\n");
                b.append("    // The type NeoForge's EntityBlock.newBlockEntity made for each block, placed alone.\n");
                b.append("    let id = match block {\n");
                for (Map.Entry<String, Map<String, Object>> e : blocks.entrySet()) {
                    String type = Json.text(e.getValue(), "block_entity");
                    if (type == null) {
                        note(notes, e.getKey(), "its block entity's type couldn't be read" + unread(e.getValue(), "block_entity"));
                        continue;
                    }
                    b.append("        ").append(rust(e.getKey())).append(" => ").append(rust(type)).append(",\n");
                    count++;
                }
                b.append("        _ => return,\n    };\n");
                b.append("    // TODO: what each block entity saves isn't ported: it starts empty (report/machines.md lists what NeoForge's saved).\n");
                b.append("    *entity = Some((id.to_string(), horizon_world::nbt::Compound::new()));\n");
                return new Body(b.toString(), List.of("block", "entity"), count);
            }
            case "block_entity_ticks" -> {
                List<String> ticking = new ArrayList<>();
                for (Map.Entry<String, Map<String, Object>> e : blocks.entrySet()) {
                    if (Boolean.TRUE.equals(e.getValue().get("ticks_on_server"))) ticking.add(rust(e.getKey()));
                }
                b.append("    // The blocks whose block entity NeoForge ticks on the server (EntityBlock.getTicker gave one).\n");
                b.append("    if matches!(\n        block,\n        ").append(String.join("\n            | ", ticking)).append("\n    ) {\n        *ticks = true;\n    }\n");
                return new Body(b.toString(), List.of("block", "ticks"), ticking.size());
            }
            case "block_entity_tick" -> {
                Map<String, String> types = new TreeMap<>();
                for (Map<String, Object> m : blocks.values()) {
                    String type = Json.text(m, "block_entity");
                    if (type == null || !Boolean.TRUE.equals(m.get("ticks_on_server"))) continue;
                    String saved = String.join(", ", Json.array(m.get("saved")).stream().map(String::valueOf).toList());
                    types.putIfAbsent(type, (Json.text(m, "class") == null ? "its class" : Json.text(m, "class")) + (saved.isEmpty() ? "" : "; it saves " + saved));
                }
                b.append("    // TODO: port each block entity's tick. Its Java is the mod's own code, which isn't translated yet:\n");
                types.forEach((type, what) -> b.append("    //   ").append(type).append(": ").append(what).append('\n'));
                b.append("    let _ = (pos, entity);\n");
                return new Body(b.toString(), List.of("pos", "entity"), types.size());
            }
            case "block_entity_container" -> {
                b.append("    if container.is_some() {\n        return;\n    }\n");
                b.append("    // Each block's slots as NeoForge's item handler with no side has them, and through each face (down, up, north,\n");
                b.append("    // south, west, east) the slots it reaches, takes items into and gives them from, measured with an item.\n");
                b.append("    // TODO: menus. A block entity's menu isn't opened here (report/machines.md says why for each).\n");
                b.append("    *container = match block {\n");
                for (Map.Entry<String, Map<String, Object>> e : blocks.entrySet()) {
                    String arm = container(e.getKey(), Json.object(e.getValue().get("items")), e.getValue(), notes);
                    if (arm == null) continue;
                    b.append(arm);
                    if (!arm.stripLeading().startsWith("//")) count++;
                }
                b.append("        _ => return,\n    };\n");
                return new Body(b.toString(), List.of("block", "container"), count);
            }
            case "block_entity_energy" -> {
                b.append("    // Each block's energy buffer, as NeoForge's energy handler gave it: capacity, the most one insert and one\n");
                b.append("    // extract moved, and the faces (down, up, north, south, west, east) that took and gave energy.\n");
                b.append("    *energy = match block {\n");
                for (Map.Entry<String, Map<String, Object>> e : blocks.entrySet()) {
                    String arm = energy(e.getKey(), Json.object(e.getValue().get("energy")), notes);
                    if (arm == null) continue;
                    b.append(arm);
                    count++;
                }
                b.append("        _ => return,\n    };\n");
                return new Body(b.toString(), List.of("block", "energy"), count);
            }
            case "block_entity_fluids" -> {
                b.append("    // Each block's fluid tanks, as NeoForge's fluid handler with no side has them, in millibuckets.\n");
                b.append("    // TODO: which tanks each face reaches, takes into and gives from isn't measured: none yet.\n");
                b.append("    *fluids = match block {\n");
                for (Map.Entry<String, Map<String, Object>> e : blocks.entrySet()) {
                    String arm = fluids(e.getKey(), Json.object(e.getValue().get("fluids")), notes);
                    if (arm == null) continue;
                    b.append(arm);
                    count++;
                }
                b.append("        _ => return,\n    };\n");
                return new Body(b.toString(), List.of("block", "fluids"), count);
            }
            default -> throw new IllegalArgumentException(hook);
        }
    }

    private static String container(String block, Map<String, Object> items, Map<String, Object> facts, Map<String, List<String>> notes) {
        if (items.isEmpty()) return null;
        Object slotsRead = items.get("slots");
        if (!(slotsRead instanceof Number n)) {
            note(notes, block, "its item slots are reached through faces only, with no handler for the whole: their count isn't known, so no container is written");
            return null;
        }
        int slots = n.intValue();
        if (slots == 0) return null;
        menuNote(block, facts, notes);
        if (slots > MAX_SLOTS) {
            note(notes, block, "it has " + slots + " item slots: Horizon's containers hold " + MAX_SLOTS + " at most, so its container is written commented out");
        }
        List<Set<Integer>> faces = new ArrayList<>();
        Set<Integer> inputs = new TreeSet<>(), outputs = new TreeSet<>();
        Map<Integer, Set<String>> inputVia = new TreeMap<>(), reachVia = new TreeMap<>();
        int unmapped = 0;
        Map<String, Object> byFace = Json.object(items.get("faces"));
        for (String face : FACES) {
            Map<String, Object> f = Json.object(byFace.get(face));
            Set<Integer> reaches = ints(f.get("reaches"));
            faces.add(reaches);
            for (int s : reaches) reachVia.computeIfAbsent(s, k -> new TreeSet<>()).add(face);
            for (int s : ints(f.get("inputs"))) {
                inputs.add(s);
                inputVia.computeIfAbsent(s, k -> new TreeSet<>()).add(face);
            }
            outputs.addAll(ints(f.get("outputs")));
            if (f.get("unmapped") instanceof Number u) unmapped += u.intValue();
        }
        if (FACES.stream().allMatch(face -> number(Json.object(byFace.get(face)).get("slots")) == 0)) {
            note(notes, block, "no face had an item handler on a new block, so no face reaches its slots here: hoppers and pipes don't (its sides may be "
                    + "set up later, by its facing or its settings)");
        }
        if (unmapped > 0) {
            note(notes, block, unmapped + (unmapped == 1 ? " face slot" : " face slots") + " took none of the items tried, through the face or into the "
                    + "container, so where " + (unmapped == 1 ? "it is" : "they are") + " in the container isn't known: no face reaches " + (unmapped == 1 ? "it" : "them")
                    + " here (an output only a machine fills, most likely)");
        }
        for (Map.Entry<Integer, Set<String>> e : inputVia.entrySet()) {
            if (!e.getValue().equals(reachVia.get(e.getKey()))) {
                note(notes, block, "slot " + e.getKey() + " takes items through " + String.join(", ", e.getValue()) + " only, but Horizon's container has one list of inputs "
                        + "for every face that reaches it (" + String.join(", ", reachVia.get(e.getKey())) + ")");
            }
        }
        String c = slots > MAX_SLOTS ? "        // " : "        ";
        StringBuilder b = new StringBuilder();
        b.append(c).append(rust(block)).append(" => Some(horizon_client::containers::ModContainer {\n");
        b.append(c).append("    slots: ").append(slots).append(",\n");
        b.append(c).append("    faces: [").append(String.join(", ", faces.stream().map(Machines::vec).toList())).append("],\n");
        b.append(c).append("    inputs: ").append(vec(inputs)).append(",\n");
        b.append(c).append("    outputs: ").append(vec(outputs)).append(",\n");
        b.append(c).append("    menu: None,\n");
        b.append(c).append("}),\n");
        return b.toString();
    }

    private static void menuNote(String block, Map<String, Object> facts, Map<String, List<String>> notes) {
        Map<String, Object> menu = Json.object(facts.get("menu"));
        if (menu.isEmpty()) {
            note(notes, block, "its menu isn't opened: neither the block nor its block entity hands NeoForge a MenuProvider, so the mod opens it in its own code");
        } else {
            note(notes, block, "its menu (" + (Json.text(menu, "type") == null ? "a menu without a type" : "`" + Json.text(menu, "type") + "`") + ", "
                    + Json.array(menu.get("slots")).size() + " slots of its own) isn't opened: Horizon opens a mod container's menu only on a block whose template "
                    + "opens one, and no screen of the mod's is ported");
        }
    }

    private static String energy(String block, Map<String, Object> energy, Map<String, List<String>> notes) {
        if (energy.isEmpty()) return null;
        Map<String, Object> unsided = Json.object(energy.get("unsided"));
        Map<String, Object> byFace = Json.object(energy.get("faces"));
        long capacity = number(unsided.get("capacity"));
        boolean[] in = new boolean[6], out = new boolean[6];
        TreeSet<Long> insertLimits = new TreeSet<>(), extractLimits = new TreeSet<>();
        for (int i = 0; i < 6; i++) {
            Map<String, Object> f = Json.object(byFace.get(FACES.get(i)));
            if (f.isEmpty()) continue;
            capacity = Math.max(capacity, number(f.get("capacity")));
            long insert = number(f.get("insert_at_most")), extract = number(f.get("extract_at_most"));
            if (insert > 0) {
                in[i] = true;
                insertLimits.add(insert);
            }
            if (extract > 0) {
                out[i] = true;
                extractLimits.add(extract);
            }
        }
        if (capacity < 1) {
            note(notes, block, "its energy handler has no capacity, so no buffer is written");
            return null;
        }
        if (insertLimits.isEmpty() && extractLimits.isEmpty()) {
            note(notes, block, "no face took or gave energy on a new block, and its handler with no side neither: the buffer is written with no face, "
                    + "and its own code (TODO) is all that fills or drains it");
        }
        if (insertLimits.size() > 1 || extractLimits.size() > 1) {
            note(notes, block, "its faces move energy at different rates (" + insertLimits + " in, " + extractLimits + " out): Horizon's buffer has one of each, the highest is written");
        }
        long filled = number(energy.get("filled"));
        if (!extractLimits.isEmpty() && filled < capacity && extractLimits.contains(filled)) {
            note(notes, block, "it was filled to " + filled + " of " + capacity + " only, so the most one extract moves may be more than the " + filled + " written");
        }
        long maxIn = insertLimits.isEmpty() ? 0 : insertLimits.last(), maxOut = extractLimits.isEmpty() ? 0 : extractLimits.last();
        if (capacity > Integer.MAX_VALUE) capacity = Integer.MAX_VALUE;
        return "        " + rust(block) + " => Some(horizon_client::containers::ModEnergyBuffer {\n"
                + "            capacity: " + capacity + ",\n"
                + "            max_insert: " + maxIn + ",\n"
                + "            max_extract: " + maxOut + ",\n"
                + "            insert_faces: " + bools(in) + ",\n"
                + "            extract_faces: " + bools(out) + ",\n"
                + "        }),\n";
    }

    private static String fluids(String block, Map<String, Object> fluids, Map<String, List<String>> notes) {
        if (fluids.isEmpty()) return null;
        List<Long> tanks = new ArrayList<>();
        for (Object t : Json.array(fluids.get("tanks"))) tanks.add(number(t));
        if (tanks.isEmpty()) {
            note(notes, block, "its fluid tanks are reached through faces only, with no handler for the whole: their capacities aren't known, so none are written");
            return null;
        }
        if (tanks.size() > MAX_TANKS) note(notes, block, "it has " + tanks.size() + " fluid tanks: Horizon keeps the first " + MAX_TANKS);
        if (tanks.stream().anyMatch(t -> t > MAX_TANK)) note(notes, block, "a tank holds more than Horizon's " + MAX_TANK + " millibuckets; Horizon clamps it");
        note(notes, block, "which of its tanks each face reaches isn't measured: no face reaches one (TODO)");
        return "        " + rust(block) + " => Some(horizon_client::containers::ModFluidTanks {\n"
                + "            tanks: vec![" + String.join(", ", tanks.stream().map(String::valueOf).toList()) + "],\n"
                + "            faces: Default::default(),\n"
                + "            inputs: Vec::new(),\n"
                + "            outputs: Vec::new(),\n"
                + "        }),\n";
    }

    // ---- a helper ----

    static String helper(String name, Hook hook, Body body, String function, String module) {
        StringBuilder b = new StringBuilder();
        b.append("// The `").append(name).append("` hook point, for the mod's machines. Written by horizon-port-neoforge from what NeoForge\n");
        b.append("// gave of them in a server (`dump`), each block placed alone, new, in its default state.\n");
        if (hook != null && hook.doc() != null && !hook.doc().isBlank()) b.append("//\n// What Horizon says of it: ").append(hook.doc().replaceAll("\\s+", " ").strip()).append('\n');
        b.append("fn ").append(function).append('(');
        List<Binding> bindings = hook == null ? List.of() : hook.bindings();
        for (int i = 0; i < bindings.size(); i++) b.append(i == 0 ? "" : ", ").append(bindings.get(i).name()).append(": ").append(bindings.get(i).type());
        b.append(") {\n");
        b.append("    use crate::game::").append(module).append(" as m;\n");
        b.append("    // Turned off in Options > Mods: nothing until the next build leaves it out.\n");
        b.append("    if !game.native_on(m::ID) {\n        return;\n    }\n");
        b.append(body.text());
        b.append("}\n");
        return b.toString();
    }

    // ---- words ----

    /** What the bundle's PORTING.md says of the machines. */
    static List<String> words(Made m) {
        List<String> out = new ArrayList<>();
        out.add("The mod's machines, from what NeoForge gave of them in a server (`dump`): " + m.blocks() + (m.blocks() == 1 ? " block" : " blocks")
                + " with a block entity, of " + m.types() + (m.types() == 1 ? " type" : " types") + ", each placed alone and new. On Horizon's block-entity hook points:");
        out.add("");
        out.add("| Hook point | Blocks | State |");
        out.add("|---|---|---|");
        m.counts().forEach((hook, n) -> out.add("| `" + hook + "` | " + n + " | "
                + (m.on().containsKey(hook) ? "on, in `" + m.on().get(hook) + "`" : "off: " + m.off().get(hook).replace("|", "\\|")) + " |"));
        out.add("");
        if (!m.bridged().isEmpty()) {
            out.add("What they do is the mod's own code, translated by this tool (`java.rs`): `block_entity_tick` runs it through the bridge "
                    + "(`bridge.rs`) for " + m.bridged().size() + (m.bridged().size() == 1 ? " type" : " types") + " (" + String.join(", ", m.bridged().keySet().stream().map(t -> "`" + t + "`").toList())
                    + "): the block's own block entity, loaded from what Horizon saved, ticked by the ticker its block gives, saved back after each tick. "
                    + "Not yet: menus and screens (the mod declares " + m.menuTypes() + " menu " + (m.menuTypes() == 1 ? "type" : "types") + "), and what the block "
                    + "entity does to the world beyond its own data (a Level's methods aren't translated). `report/machines.md` lists what isn't, block by block.");
        } else {
            out.add("What they do is the mod's own code" + (m.bridgeProblem() == null ? ", which isn't translated yet" : ", whose bridge isn't written: " + m.bridgeProblem())
                    + ": `block_entity_tick` does nothing until its TODO is filled in, "
                    + "what a block entity saves isn't carried over, and no menu opens (the mod declares " + m.menuTypes() + " menu " + (m.menuTypes() == 1 ? "type" : "types")
                    + ", and no screen is ported). `report/machines.md` lists what isn't, block by block.");
        }
        return out;
    }

    /** report/machines.md: by block, what NeoForge gave and what of it isn't in the bundle. */
    static String report(Map<String, Object> machines, Made m) {
        StringBuilder b = new StringBuilder("# The mod's machines\n\nWhat NeoForge gave of each block with a block entity, placed alone and new in a server ")
                .append("(`dump`), and what of it isn't in the bundle. A ticker's method is the mod's lambda its class was made by, when that is certain (`Tickers`).\n\n| Block | Block entity | Ticks | Its ticker runs | Saves (in NeoForge) | Not carried over |\n|---|---|---|---|---|---|\n");
        Map<String, Object> blocks = new TreeMap<>(Json.object(machines.get("blocks")));
        blocks.forEach((id, v) -> {
            Map<String, Object> o = Json.object(v);
            String saved = String.join(", ", Json.array(o.get("saved")).stream().map(String::valueOf).toList());
            List<String> notes = new ArrayList<>(m.notes().getOrDefault(id, List.of()));
            Json.object(o.get("unread")).forEach((k, why) -> notes.add(k + " couldn't be read (" + why + ")"));
            b.append("| `").append(id).append("` | ").append(Json.text(o, "block_entity") == null ? "" : "`" + Json.text(o, "block_entity") + "`").append(" | ")
                    .append(Boolean.TRUE.equals(o.get("ticks_on_server")) ? "yes" : "no").append(" | ")
                    .append(Json.text(o, "tick") == null ? "" : Json.text(o, "tick").replace("|", "\\|")).append(" | ").append(saved).append(" | ")
                    .append(String.join("; ", notes).replace("|", "\\|")).append(" |\n");
        });
        return b.toString();
    }

    // ---- small things ----

    private static void note(Map<String, List<String>> notes, String block, String what) {
        List<String> l = notes.computeIfAbsent(block, k -> new ArrayList<>());
        if (!l.contains(what)) l.add(what);
    }

    private static String unread(Map<String, Object> block, String key) {
        String why = Json.text(Json.object(block.get("unread")), key);
        return why == null ? "" : " (" + why + ")";
    }

    static String rust(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static Set<Integer> ints(Object array) {
        Set<Integer> out = new TreeSet<>();
        for (Object o : Json.array(array)) if (o instanceof Number n) out.add(n.intValue());
        return out;
    }

    private static long number(Object o) {
        return o instanceof Number n ? n.longValue() : 0;
    }

    private static String vec(Set<Integer> slots) {
        return slots.isEmpty() ? "Vec::new()" : "vec![" + String.join(", ", slots.stream().map(String::valueOf).toList()) + "]";
    }

    private static String bools(boolean[] faces) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < faces.length; i++) b.append(i == 0 ? "" : ", ").append(faces[i]);
        return b.append(']').toString();
    }

    private static String sentence(String words) {
        String s = Character.toUpperCase(words.charAt(0)) + words.substring(1);
        return s.endsWith(".") ? s : s + ".";
    }
}
