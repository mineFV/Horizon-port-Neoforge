package horizonneo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Declarations for the blocks horizon-port-pack's carry left out because no vanilla template has their
 * state properties. Horizon takes a block that declares state properties of its own beside its template's
 * (docs/mods.md), so such a block is declared on a plain template that has none, with every value the facts
 * give: its properties and default state, hardness, resistance, friction, tool need, map colour, sounds,
 * light by state, shapes by state, occlusion, random ticks, and its item's stack size and rarity.
 *
 * Only what the facts say is written, and only a block a plain template can stand for: one whose first
 * vanilla class is `Block` itself, with the template's speed and jump factors. What the template can't
 * carry (its own code, a value that differs between states where Horizon takes one, a shape of more than 16
 * boxes) is said for each block, never made up. A block that can't be declared stays left out, with why.
 */
final class Declarations {
    private Declarations() {}

    static final String BLOCK = "net.minecraft.world.level.block.Block", BEHAVIOUR = "net.minecraft.world.level.block.state.BlockBehaviour";
    static final List<List<Double>> CUBE = List.of(List.of(0.0, 0.0, 0.0, 1.0, 1.0, 1.0));

    /**
     * What was made. declared: the blocks declared, `namespace:name`. notes: for each, what its declaration
     * doesn't reproduce. left: the blocks still not declared, each with why.
     */
    record Made(List<String> declared, Map<String, List<String>> notes, Map<String, String> left) {}

    /** Declares each of `blocks` (`namespace:name`) in the carried folder from the facts. */
    static Made declare(Map<String, Object> facts, Path carried, Collection<String> blocks) throws IOException {
        Map<String, Object> all = Json.object(facts.get("blocks")), items = Json.object(facts.get("items"));
        List<String> plain = plainTemplates(all);
        List<String> declared = new ArrayList<>();
        Map<String, List<String>> notes = new TreeMap<>();
        Map<String, String> left = new TreeMap<>();
        for (String id : blocks) {
            Map<String, Object> b = Json.object(all.get(id));
            if (b.isEmpty()) {
                left.put(id, "not in the facts");
                continue;
            }
            List<String> why = new ArrayList<>();
            Map<String, Object> file = block(id, b, all, items, plain, why);
            if (file == null) {
                left.put(id, String.join("; ", why));
                continue;
            }
            int colon = id.indexOf(':');
            Path to = carried.resolve("data").resolve(id.substring(0, colon)).resolve("horizon/block").resolve(id.substring(colon + 1) + ".json");
            Files.createDirectories(to.getParent());
            Files.writeString(to, Json.write(file), StandardCharsets.UTF_8);
            declared.add(id);
            if (!why.isEmpty()) notes.put(id, why);
        }
        return new Made(declared, notes, left);
    }

    /** The vanilla blocks of `Block` itself with no state property, by id: what a block of its own can stand on. */
    static List<String> plainTemplates(Map<String, Object> all) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Object> e : new TreeMap<>(all).entrySet()) {
            if (!e.getKey().startsWith("minecraft:") || e.getKey().endsWith("air")) continue;
            Map<String, Object> b = Json.object(e.getValue());
            if (vanillaClass(b).equals(BLOCK) && Json.object(b.get("properties")).isEmpty() && Json.object(b.get("states")).size() == 1) out.add(e.getKey());
        }
        return out;
    }

    /** The first vanilla class in a block's class chain: what a template has to be. */
    static String vanillaClass(Map<String, Object> b) {
        for (Object c : Json.array(b.get("class"))) {
            if (String.valueOf(c).startsWith("net.minecraft.")) return String.valueOf(c);
        }
        return "";
    }

    /** The declaration of one block, or null with why in `why`. */
    private static Map<String, Object> block(String id, Map<String, Object> b, Map<String, Object> all, Map<String, Object> items,
                                             List<String> plain, List<String> why) {
        List<Object> classes = Json.array(b.get("class"));
        if (!vanillaClass(b).equals(BLOCK)) {
            why.add("its vanilla class is " + vanillaClass(b) + ", whose behaviour a plain template doesn't have");
            return null;
        }
        Map<String, Object> states = Json.object(b.get("states"));
        Map<String, List<String>> properties = new TreeMap<>();
        for (Map.Entry<String, Object> p : Json.object(b.get("properties")).entrySet()) {
            List<String> values = new ArrayList<>();
            for (Object v : Json.array(p.getValue())) values.add(String.valueOf(v));
            properties.put(p.getKey(), values);
        }
        Map<String, Object> def = Json.object(states.get(String.valueOf(b.get("default"))));
        if (states.isEmpty() || def.isEmpty()) {
            why.add("the facts have no default state for it");
            return null;
        }
        double speed = number(b.get("speed_factor")), jump = number(b.get("jump_factor"));
        String sounds = sounds(def.get("sound"), all);
        String template = null;
        for (String t : plain) {
            Map<String, Object> tb = Json.object(all.get(t));
            if (number(tb.get("speed_factor")) != speed || number(tb.get("jump_factor")) != jump) continue;
            if (template == null) template = t;
            if (sounds != null && sounds.equals(t)) {
                template = t;
                break;
            }
        }
        if (template == null) {
            why.add("no plain vanilla block has its speed and jump factors (" + speed + ", " + jump + ")");
            return null;
        }

        Map<String, Object> file = new LinkedHashMap<>();
        file.put("template", template);
        why.add("declared by this tool on " + template + (properties.isEmpty() ? "" : ", with state properties of its own ("
                + String.join(", ", properties.keySet()) + ")") + ": horizon-port-pack's carry had left it out");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("hardness", same(states, "hardness", def, why, "hardness"));
        props.put("explosion_resistance", number(b.get("explosion_resistance")));
        props.put("friction", number(b.get("friction")));
        props.put("requires_tool", same(states, "requires_tool", def, why, "tool need"));
        props.put("map_color", same(states, "map_color_id", def, why, "map colour"));
        if (sounds != null) props.put("sounds", sounds);
        else why.add("sounds: no vanilla block has its sound events (" + soundName(def.get("sound")) + "), so the template's are kept");
        if (!uniform(states, "sound")) why.add("sounds differ between its states; the default state's are used");

        Set<Object> ticks = values(states, "random_ticks");
        if (ticks.equals(Set.of(true))) props.put("random_ticks", true);
        else if (ticks.size() > 1) {
            Map<String, Object> by = byOneProperty(states, "random_ticks", properties);
            if (by != null) props.put("random_ticks", by);
            else why.add("random ticks differ between its states in a way one property doesn't say");
        }
        Set<Object> occlude = values(states, "can_occlude");
        if (occlude.equals(Set.of(false))) props.put("occludes", false);
        else if (occlude.size() > 1) why.add("it occludes in some states and not others; Horizon takes one answer, so it occludes as the template does");
        file.put("properties", props);

        if (!properties.isEmpty()) {
            Map<String, Object> declared = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> p : properties.entrySet()) {
                boolean bool = p.getValue().size() == 2 && new LinkedHashSet<>(p.getValue()).equals(Set.of("true", "false"));
                declared.put(p.getKey(), bool ? Map.of("type", "boolean") : Map.of("type", "enum", "values", p.getValue()));
            }
            file.put("state_properties", declared);
            Map<String, Object> defaults = new LinkedHashMap<>();
            for (String part : String.valueOf(b.get("default")).split(",")) {
                int eq = part.indexOf('=');
                if (eq > 0) defaults.put(part.substring(0, eq), part.substring(eq + 1));
            }
            file.put("default_state", defaults);
        }

        Set<Object> light = values(states, "emission");
        if (light.size() == 1) {
            props.put("light", ((Number) light.iterator().next()).intValue());
        } else {
            file.put("state_light", table(states, properties, String.valueOf(b.get("default")), s -> ((Number) s.get("emission")).intValue()));
        }

        String defaultState = String.valueOf(b.get("default"));
        Map<String, Object> outline = shapes(states, properties, defaultState, "outline", why), collision = shapes(states, properties, defaultState, "collision", why);
        if (outline != null) file.put("shapes", outline);
        if (collision != null && !(outline != null ? collision.equals(outline) : sameShapes(states, "collision", "outline"))) file.put("collision_shapes", collision);

        Map<String, Object> item = Json.object(items.get(String.valueOf(b.get("item"))));
        if (!id.equals(b.get("item")) || item.isEmpty()) {
            file.put("item", false);
            if (!"minecraft:air".equals(b.get("item"))) why.add("its item is " + b.get("item") + ", not one of the same id: declared without an item");
        } else {
            Map<String, Object> components = new LinkedHashMap<>();
            long stack = (long) number(item.get("max_stack_size"));
            if (stack != 64) components.put("minecraft:max_stack_size", stack);
            if (!"common".equals(item.get("rarity"))) components.put("minecraft:rarity", item.get("rarity"));
            if (!components.isEmpty()) file.put("components", components);
            List<String> own = new ArrayList<>();
            for (String c : Json.object(item.get("components")).keySet()) if (!c.startsWith("minecraft:")) own.add(c);
            if (!own.isEmpty()) why.add("its item's components of the mod's own (" + String.join(", ", own) + ") need a native mod to register them");
        }
        if (classes.size() > 2) why.add("class " + classes.get(0) + " (behaviour beyond " + BLOCK + "): its code isn't ported");
        return file;
    }

    // ---- values by state ----

    /** A value the same in every state: the default state's, and said when it isn't. */
    private static Object same(Map<String, Object> states, String key, Map<String, Object> def, List<String> why, String what) {
        if (!uniform(states, key)) why.add(what + " differs between its states; the default state's is used");
        Object v = def.get(key);
        return v instanceof Number n && !(v instanceof Double) ? n.longValue() : v;
    }

    private static boolean uniform(Map<String, Object> states, String key) {
        return values(states, key).size() <= 1;
    }

    private static Set<Object> values(Map<String, Object> states, String key) {
        Set<Object> out = new LinkedHashSet<>();
        for (Object s : states.values()) out.add(Json.object(s).get(key));
        return out;
    }

    /** A state's values of `properties` (sorted by name), as a table key: `v1,v2`. */
    private static String key(String state, Collection<String> properties) {
        Map<String, String> v = new HashMap<>();
        for (String part : state.split(",")) {
            int eq = part.indexOf('=');
            if (eq > 0) v.put(part.substring(0, eq), part.substring(eq + 1));
        }
        List<String> out = new ArrayList<>();
        for (String p : properties) out.add(v.get(p));
        return String.join(",", out);
    }

    /**
     * A table by every property: the most common value as default (the default state's, when it is one of the
     * most common), the others by state.
     */
    private static Map<String, Object> table(Map<String, Object> states, Map<String, List<String>> properties, String defaultState,
                                             java.util.function.Function<Map<String, Object>, Object> value) {
        Map<Object, Integer> counts = new LinkedHashMap<>();
        for (Object s : states.values()) counts.merge(value.apply(Json.object(s)), 1, Integer::sum);
        int most = counts.values().stream().max(Integer::compare).orElseThrow();
        Object mine = value.apply(Json.object(states.get(defaultState)));
        Object common = counts.get(mine) == most ? mine : counts.entrySet().stream().filter(e -> e.getValue() == most).findFirst().orElseThrow().getKey();
        Map<String, Object> values = new TreeMap<>();
        for (Map.Entry<String, Object> s : states.entrySet()) {
            Object v = value.apply(Json.object(s.getValue()));
            if (!v.equals(common)) values.put(key(s.getKey(), properties.keySet()), v);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        if (!values.isEmpty()) {
            out.put("properties", new ArrayList<>(properties.keySet()));
            out.put("values", values);
        }
        out.put("default", common);
        return out;
    }

    /** `{property, values}`: the states that tick, when one property alone says which. */
    private static Map<String, Object> byOneProperty(Map<String, Object> states, String key, Map<String, List<String>> properties) {
        for (String p : properties.keySet()) {
            Map<String, Set<Object>> by = new TreeMap<>();
            for (Map.Entry<String, Object> s : states.entrySet()) by.computeIfAbsent(key(s.getKey(), List.of(p)), k -> new LinkedHashSet<>()).add(Json.object(s.getValue()).get(key));
            if (by.values().stream().allMatch(v -> v.size() == 1)) {
                List<String> yes = new ArrayList<>();
                by.forEach((v, t) -> { if (t.contains(true)) yes.add(v); });
                return Map.of("property", p, "values", yes);
            }
        }
        return null;
    }

    /** A shape table, or null when every state is a full cube (the template's) or a state has more boxes than Horizon takes. */
    private static Map<String, Object> shapes(Map<String, Object> states, Map<String, List<String>> properties, String defaultState, String key, List<String> why) {
        int most = 0;
        boolean cube = true;
        for (Object s : states.values()) {
            List<Object> boxes = Json.array(Json.object(s).get(key));
            most = Math.max(most, boxes.size());
            if (!boxes(boxes).equals(CUBE)) cube = false;
        }
        if (cube) return null;
        if (most > 16) {
            why.add((key.equals("outline") ? "outline" : "collision") + ": up to " + most + " boxes in a state, and Horizon takes 16, so the template's full cube is kept");
            return null;
        }
        return table(states, properties, defaultState, s -> boxes(Json.array(s.get(key))));
    }

    private static boolean sameShapes(Map<String, Object> states, String a, String b) {
        for (Object s : states.values()) if (!boxes(Json.array(Json.object(s).get(a))).equals(boxes(Json.array(Json.object(s).get(b))))) return false;
        return true;
    }

    private static List<List<Double>> boxes(List<Object> raw) {
        List<List<Double>> out = new ArrayList<>();
        for (Object box : raw) {
            List<Double> b = new ArrayList<>();
            for (Object d : Json.array(box)) b.add(number(d));
            out.add(b);
        }
        return out;
    }

    // ---- sounds ----

    /** The first vanilla block (by id, not air) whose default state has exactly these sound events, volume and pitch. */
    static String sounds(Object sound, Map<String, Object> all) {
        if (!(sound instanceof Map<?, ?>)) return null;
        for (Map.Entry<String, Object> e : new TreeMap<>(all).entrySet()) {
            if (!e.getKey().startsWith("minecraft:") || e.getKey().endsWith("air")) continue;
            Map<String, Object> b = Json.object(e.getValue());
            Object theirs = Json.object(Json.object(b.get("states")).get(String.valueOf(b.get("default")))).get("sound");
            if (sound.equals(theirs)) return e.getKey();
        }
        return null;
    }

    private static String soundName(Object sound) {
        return String.valueOf(Json.object(sound).get("break"));
    }

    private static double number(Object o) {
        return o instanceof Number n ? n.doubleValue() : Double.NaN;
    }
}
