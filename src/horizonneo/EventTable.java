package horizonneo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Which Horizon hook point stands for which NeoForge event, and how sure that is.
 *
 * Nothing here is this tool's own opinion. Horizon's hook catalogue, `native-hooks.json` in its
 * source, names each hook point's NeoForge counterpart in a `neoforge` label
 * (`"ServerTickEvent.Pre / ServerTickEvent.Post"`, `"ItemTossEvent (only hears: a toss can't be
 * canceled)"`, `"none (NeoForge has no such event)"`). The table is built from the catalogue of the
 * Horizon source a mod is converted against, each time, so it is never out of step with it.
 *
 * How a label is read (its names, the notes in brackets, `.Post` after `X.Pre`), and the three
 * small lists below (RENAMED, PARENTS, DATA), follow Horizon's own `horizon-port-pack events`
 * (crates/horizon-port-pack/src/events.rs; MIT, Copyright (c) 2026 Ludovic Marie and the Horizon
 * contributors). That command lists a mod's events with their hooks "for a human to check". This
 * goes one step further, and so sorts each answer by how sure it is: only EXACT is switched on.
 */
final class EventTable {
    /** How sure a hook point is for an event, from the most to the least. */
    enum Level {
        /** One hook point, named for this event alone, with no note: its counterpart. */
        EXACT,
        /** One hook point named for it, but with a note to read, shared with other events, or under a newer name. */
        APPROXIMATE,
        /** Several hook points are named for it: which one a listener needs isn't told. */
        SEVERAL,
        /** Only another phase of it (`.Pre` for `.Post`), or events nested in it, have hook points. */
        PHASE,
        /** A hook point Horizon is preparing names it; it isn't in the hooks API yet. */
        PENDING,
        /** A hook point's label says why it is not this event's counterpart. */
        NOTED,
        /** No hook point is needed: vanilla made what the event built into data. */
        DATA,
        /** The catalogue has nothing for it. */
        NONE,
        /** Not NeoForge's event at all: a class of the mod's own, or of another mod. */
        OWN
    }

    /** A parameter of a hook point, as an `[[injection]]` binds it. */
    record Binding(String name, String type, String value) {}

    /**
     * A hook point. label: the catalogue's `neoforge` text. note: what it says in brackets, null when
     * nothing. events: how many events the label names. api, doc, signature, bindings: from
     * horizon-port's reading of Horizon's source; null (and no bindings) when it gave none.
     * fragment: the file of a hook point that is only being prepared, else null.
     */
    record Hook(String name, String target, String label, String note, int events, String givenPackage, String side, String contract,
                Integer api, String doc, String signature, List<Binding> bindings, String fragment) {
        Hook {
            bindings = List.copyOf(bindings);
        }
    }

    /** The answer for one event. why: in words, for the bundle and the report. newName: the name it was found under, when NeoForge renamed it. */
    record Match(Level level, List<Hook> hooks, String why, String newName) {
        Match {
            hooks = List.copyOf(hooks);
        }
    }

    /** Names NeoForge changed after 1.21.1: an event the catalogue lacks is looked up again under its new one. */
    private static final String[][] RENAMED = {
        {"Capabilities.ItemHandler.", "Capabilities.Item."},
        {"Capabilities.EnergyStorage.", "Capabilities.Energy."},
        {"AddReloadListenerEvent", "AddServerReloadListenersEvent"},
        {"ModelEvent.RegisterAdditional", "ModelEvent.RegisterStandalone"},
        {"RecipesUpdatedEvent", "RecipesReceivedEvent"},
        {"RenderTooltipEvent.Color", "RenderTooltipEvent.Texture"},
        {"AnvilRepairEvent", "AnvilCraftEvent.Pre"},
    };

    /** Events that are only the parent of the ones NeoForge documents under them. */
    private static final Map<String, List<String>> PARENTS = Map.of("ModConfigEvent", List.of("Loading", "Reloading", "Unloading"));

    /** Events NeoForge dropped because vanilla made what they built into data, with the registries a port writes instead. */
    private static final Map<String, List<String>> DATA = Map.of(
            "VillagerTradesEvent", List.of("trade_set", "villager_trade"),
            "WandererTradesEvent", List.of("trade_set", "villager_trade"));

    private final Map<String, List<Hook>> exact = new TreeMap<>();
    private final Map<String, List<Hook>> noted = new TreeMap<>();
    private final Map<String, List<Hook>> pending = new TreeMap<>();
    private final int hooks;

    private EventTable(int hooks) {
        this.hooks = hooks;
    }

    /** How many hook points the catalogue lists. */
    int hooks() {
        return hooks;
    }

    /** How many NeoForge events have a hook point named for them. */
    int events() {
        return exact.size();
    }

    // ---- building it ----

    /**
     * The table of a Horizon source folder. hookList: horizon-port's `hooks --json` for that source
     * (each hook point's signature and bindings), or null to go without.
     */
    static EventTable read(Path horizonSrc, String hookList) throws IOException {
        Path catalogue = horizonSrc.resolve("native-hooks.json");
        if (!Files.isRegularFile(catalogue)) throw new IOException(horizonSrc + " has no native-hooks.json: it isn't Horizon's source, or is older than its hook catalogue");
        List<String> fragments = new ArrayList<>();
        List<String> fragmentNames = new ArrayList<>();
        Path dir = horizonSrc.resolve("tools").resolve("native-hook-fragments");
        if (Files.isDirectory(dir)) {
            List<Path> files;
            try (Stream<Path> s = Files.list(dir)) {
                files = s.filter(f -> f.getFileName().toString().endsWith(".json")).sorted().toList();
            }
            for (Path f : files) {
                fragments.add(Files.readString(f, StandardCharsets.UTF_8));
                fragmentNames.add("tools/native-hook-fragments/" + f.getFileName());
            }
        }
        return build(Files.readString(catalogue, StandardCharsets.UTF_8), hookList, fragments, fragmentNames);
    }

    /** The same, from the texts themselves. */
    static EventTable build(String catalogue, String hookList, List<String> fragments, List<String> fragmentNames) throws IOException {
        Map<String, Map<String, Object>> read = new LinkedHashMap<>();
        if (hookList != null) {
            try {
                for (Object o : Json.array(Json.object(Json.parse(hookList)).get("hooks"))) {
                    Map<String, Object> h = Json.object(o);
                    String name = Json.text(h, "name");
                    if ("hook".equals(Json.text(h, "kind")) && name != null && name.startsWith("native_")) read.put(name.substring("native_".length()), h);
                }
            } catch (Json.Invalid e) {
                throw new IOException("horizon-port's list of hook points isn't JSON (" + e.getMessage() + ")");
            }
        }
        List<Object> listed;
        try {
            listed = Json.array(Json.object(Json.parse(catalogue)).get("hooks"));
        } catch (Json.Invalid e) {
            throw new IOException("native-hooks.json can't be read (" + e.getMessage() + ")");
        }
        if (listed.isEmpty()) throw new IOException("native-hooks.json lists no hook point");
        EventTable table = new EventTable(listed.size());
        for (Object o : listed) table.add(Json.object(o), read, null);
        for (int i = 0; i < fragments.size(); i++) {
            try {
                for (Object o : Json.array(Json.object(Json.parse(fragments.get(i))).get("hooks"))) table.add(Json.object(o), Map.of(), fragmentNames.get(i));
            } catch (Json.Invalid e) {
                // A fragment that can't be read prepares nothing this can tell of.
            }
        }
        return table;
    }

    private void add(Map<String, Object> entry, Map<String, Map<String, Object>> read, String fragment) {
        String name = Json.text(entry, "name"), label = Json.text(entry, "neoforge");
        if (name == null || label == null) return;
        boolean none = label.toLowerCase(java.util.Locale.ROOT).startsWith("none");
        List<String> names = none ? mentioned(label) : counterparts(label);
        Map<String, Object> seen = read.getOrDefault(name, Map.of());
        List<Binding> bindings = new ArrayList<>();
        for (Object b : Json.array(seen.get("bindings"))) {
            Map<String, Object> binding = Json.object(b);
            String n = Json.text(binding, "name"), type = Json.text(binding, "ty"), value = Json.text(binding, "value");
            if (n != null && type != null && value != null) bindings.add(new Binding(n, type, value));
        }
        Hook hook = new Hook(name, Json.text(entry, "target"), label, none ? null : note(label), names.size(), none ? null : givenPackage(label),
                Json.text(entry, "side"), Json.text(Json.object(entry.get("mod_api")), "contract"),
                seen.get("api") instanceof Number api ? Integer.valueOf(api.intValue()) : null, Json.text(seen, "doc"), Json.text(seen, "signature"), bindings, fragment);
        Map<String, List<Hook>> into = fragment != null ? pending : none ? noted : exact;
        // A fragment's `none (...)` label prepares nothing for the events it mentions.
        if (fragment != null && none) return;
        for (String n : names) into.computeIfAbsent(n, k -> new ArrayList<>()).add(hook);
    }

    // ---- looking an event up ----

    /** The answer for an event's class, by its internal name (`net/neoforged/neoforge/event/tick/ServerTickEvent$Post`). */
    Match lookup(String eventClass) {
        if (!eventClass.startsWith("net/neoforged/")) {
            return new Match(Level.OWN, List.of(), "it isn't one of NeoForge's events: its own code posts it, so that code has to be ported with it", null);
        }
        String name = eventClass.substring(eventClass.lastIndexOf('/') + 1).replace('$', '.');
        Match found = find(name, eventClass);
        if (found.level() != Level.NONE) return found;
        for (String[] renamed : RENAMED) {
            if (!name.startsWith(renamed[0])) continue;
            String newName = renamed[1] + name.substring(renamed[0].length());
            Match under = find(newName, null);
            if (under.level() == Level.NONE) continue;
            // An event under another name may carry something else: never the surest answer.
            Level level = under.level() == Level.EXACT ? Level.APPROXIMATE : under.level();
            return new Match(level, under.hooks(), "NeoForge has renamed it " + newName + ", and " + under.why(), newName);
        }
        if (DATA.containsKey(name)) {
            return new Match(Level.DATA, List.of(), "NeoForge dropped it: what it built is data now, in the registries " + String.join(" and ", DATA.get(name)), null);
        }
        return found;
    }

    private Match find(String name, String eventClass) {
        List<Hook> hooks = exact.get(name);
        if (hooks != null && hooks.size() == 1) {
            Hook h = hooks.get(0);
            String said = "Horizon's catalogue names `" + h.name() + "` for it";
            if (h.givenPackage() != null && eventClass != null && !eventClass.replace('/', '.').replace('$', '.').equals(h.givenPackage() + "." + name)) {
                return new Match(Level.NONE, List.of(), "the catalogue's " + name + " is " + h.givenPackage() + "'s, another class of that name", null);
            }
            if (h.note() != null) return new Match(Level.APPROXIMATE, hooks, said + ", with a note to read first: \"" + h.note() + "\"", null);
            if (h.events() > 1) {
                return new Match(Level.APPROXIMATE, hooks, said + ", and for other events too (\"" + h.label() + "\"): a handler has to tell them apart by the hook point's parameters", null);
            }
            return new Match(Level.EXACT, hooks, said, null);
        }
        if (hooks != null) return new Match(Level.SEVERAL, hooks, "Horizon's catalogue names " + names(hooks) + " for it: which one a handler needs depends on what it does", null);
        if (PARENTS.containsKey(name)) {
            Set<Hook> all = new LinkedHashSet<>();
            boolean every = true;
            for (String child : PARENTS.get(name)) {
                List<Hook> of = exact.get(name + "." + child);
                if (of == null) every = false;
                else all.addAll(of);
            }
            if (every) {
                return new Match(Level.SEVERAL, List.copyOf(all), "it is the parent of " + String.join(", ", PARENTS.get(name).stream().map(c -> name + "." + c).toList())
                        + ", which have " + names(List.copyOf(all)) + ": a handler of the parent hears each of them", null);
            }
        }
        List<Hook> prepared = pending.get(name);
        if (prepared != null) {
            return new Match(Level.PENDING, prepared, "Horizon is preparing " + names(prepared) + " for it (" + prepared.get(0).fragment()
                    + "), which isn't in its hooks API yet", null);
        }
        Set<Hook> related = new LinkedHashSet<>();
        List<String> relatedNames = new ArrayList<>();
        for (Map.Entry<String, List<Hook>> e : exact.entrySet()) {
            String k = e.getKey();
            boolean nested = k.startsWith(name + ".");
            int a = name.lastIndexOf('.'), b = k.lastIndexOf('.');
            boolean sibling = a > 0 && b > 0 && name.substring(0, a).equals(k.substring(0, b)) && phase(name.substring(a + 1)) && phase(k.substring(b + 1));
            if (nested || sibling) {
                related.addAll(e.getValue());
                relatedNames.add(k);
            }
        }
        if (!related.isEmpty()) {
            return new Match(Level.PHASE, List.copyOf(related), "Horizon's catalogue has no hook point for it, only for " + String.join(", ", relatedNames)
                    + " (" + names(List.copyOf(related)) + ")", null);
        }
        List<Hook> mentions = noted.get(name);
        if (mentions != null) {
            return new Match(Level.NOTED, mentions, "Horizon's catalogue says it has no counterpart: `" + mentions.get(0).name() + "` is \"" + mentions.get(0).label() + "\"", null);
        }
        return new Match(Level.NONE, List.of(), "Horizon's catalogue has no hook point for it", null);
    }

    private static boolean phase(String s) {
        return s.equals("Pre") || s.equals("Post");
    }

    private static String names(List<Hook> hooks) {
        return "`" + String.join("`, `", hooks.stream().map(Hook::name).distinct().toList()) + "`";
    }

    // ---- reading a label ----

    /** An event (`...Event`, nested or not) or a capability (`Capabilities.X.Y`). */
    static boolean isName(String s) {
        if (s.isEmpty()) return false;
        String first = s.contains(".") ? s.substring(0, s.indexOf('.')) : s;
        if (!first.endsWith("Event") && !first.equals("Capabilities")) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c < 128 && (Character.isLetterOrDigit(c) || c == '.' || c == '_'))) return false;
        }
        return true;
    }

    /**
     * The names a label gives (`ServerTickEvent.Pre / .Post`): notes in brackets and a leading
     * package dropped, a name that starts with `.` taken as a sibling of the one before it.
     */
    static List<String> counterparts(String label) {
        List<String> out = new ArrayList<>();
        for (String part : plain(label).split(" / ")) {
            part = part.strip();
            String full;
            if (part.startsWith(".") && !out.isEmpty()) {
                String previous = out.get(out.size() - 1);
                if (!previous.contains(".")) continue;
                full = previous.substring(0, previous.lastIndexOf('.')) + part;
            } else {
                full = withoutPackage(part);
            }
            if (isName(full)) out.add(full);
        }
        return out;
    }

    /** The names a `none (...)` label mentions. */
    static List<String> mentioned(String label) {
        List<String> out = new ArrayList<>();
        for (String word : label.split("[^A-Za-z0-9._]+")) {
            while (word.endsWith(".")) word = word.substring(0, word.length() - 1);
            if (isName(word)) out.add(word);
        }
        return out;
    }

    /** What a label says in brackets, null when nothing. */
    static String note(String label) {
        List<String> notes = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (c == '(') {
                if (depth++ == 0) start = i + 1;
            } else if (c == ')' && depth > 0 && --depth == 0) {
                notes.add(label.substring(start, i).strip());
            }
        }
        return notes.isEmpty() ? null : String.join("; ", notes);
    }

    /** The package a label spells out before its one name (`net.neoforged.neoforge.event.ServerChatEvent`), else null. */
    static String givenPackage(String label) {
        String[] parts = plain(label).split(" / ");
        if (parts.length != 1) return null;
        String part = parts[0].strip(), name = withoutPackage(part);
        return name.length() < part.length() && isName(name) ? part.substring(0, part.length() - name.length() - 1) : null;
    }

    /** A label without what is in brackets. */
    private static String plain(String label) {
        StringBuilder plain = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);
            else if (depth == 0) plain.append(c);
        }
        return plain.toString();
    }

    /** `net.neoforged.neoforge.event.ServerChatEvent` as `ServerChatEvent`: the parts that start with a small letter go. */
    private static String withoutPackage(String name) {
        String[] parts = name.split("\\.");
        int from = 0;
        while (from < parts.length && !parts[from].isEmpty() && Character.isLowerCase(parts[from].charAt(0))) from++;
        return String.join(".", List.of(parts).subList(from, parts.length));
    }
}
