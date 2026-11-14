package horizonneo;

import horizonneo.Analysis.Listener;
import horizonneo.EventTable.Binding;
import horizonneo.EventTable.Hook;
import horizonneo.EventTable.Level;
import horizonneo.EventTable.Match;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The mod's NeoForge event listeners, put on Horizon's hook points in a bundle.
 *
 * A listener is switched on only when all of this holds: NeoForge really calls it (it is registered,
 * on the bus its event is posted on); the event table's answer for its event is EXACT (or
 * APPROXIMATE, when the user asked for those too); and the hook point is one horizon-port read a
 * signature for in the Horizon source, whose first parameter is the game. It then gets an
 * `[[injection]]` at the head of the hook point and a helper that does nothing yet, with a TODO:
 * the handler's own Java takes a NeoForge event, so horizon-port doesn't translate it.
 *
 * Every other listener that NeoForge calls is written into the manifest commented out, the way
 * horizon-port writes a Mixin it leaves off: `# Off:`, the reason, a TODO, and the injection it
 * would be, with the hook point when the table names one.
 */
final class Wiring {
    private Wiring() {}

    /**
     * What became of one listener. level: the table's answer, null when it wasn't asked (NeoForge
     * never calls the listener). hook: the hook point named for it, null when none. state: "on",
     * "off" or "none" (nothing written for it). injection: its id in the manifest. why: in words.
     */
    record Wired(Listener listener, Level level, String hook, String state, String injection, String why) {}

    /** The phrase the analysis uses for a listener added to a bus its event isn't posted on. */
    static final String WRONG_BUS = " its event is posted on the ";

    private static final Pattern ID = Pattern.compile("(?m)^(?:# )?id = \"([^\"]+)\"$");

    /**
     * Writes the listeners into the bundle's manifest and helpers, and returns what became of each,
     * in the analysis's order. approximate: also switch on the APPROXIMATE answers.
     */
    static List<Wired> apply(Path mod, String modId, Analysis analysis, EventTable table, boolean approximate) throws IOException {
        Path manifestFile = mod.resolve("horizon-mixin.toml");
        String manifest = Files.readString(manifestFile, StandardCharsets.UTF_8);
        Map<String, Object> root;
        try {
            root = Toml.parse(manifest).root();
        } catch (Toml.Problem e) {
            throw new Bundle.Unexpected("horizon-mixin.toml can't be read: " + e.getMessage());
        }
        String module = null;
        boolean gameSide = false;
        for (Map<String, Object> m : Toml.tables(root.get("module"))) {
            if ("runtime".equals(Toml.text(m, "id"))) module = Toml.text(m, "name");
            if ("game_side".equals(Toml.text(m, "id"))) {
                Path source = mod.resolve(String.valueOf(Toml.text(m, "source")));
                gameSide = Files.isRegularFile(source) && Files.readString(source, StandardCharsets.UTF_8).contains("pub fn ensure_init(");
            }
        }
        if (module == null) throw new Bundle.Unexpected("no [[module]] with the id `runtime`");
        // The injections that are on already, by the function they are on.
        Map<String, String[]> taken = new LinkedHashMap<>();
        for (Map<String, Object> i : Toml.tables(root.get("injection"))) {
            if (Toml.text(i, "target") != null) taken.put(Toml.text(i, "target"), new String[] {Toml.text(i, "id"), Toml.text(i, "helper")});
        }
        Set<String> ids = new HashSet<>();
        Matcher used = ID.matcher(manifest);
        while (used.find()) ids.add(used.group(1));

        List<Wired> out = new ArrayList<>();
        Map<String, List<Listener>> onHook = new LinkedHashMap<>();
        Map<String, Hook> hooks = new LinkedHashMap<>();
        List<String[]> offBlocks = new ArrayList<>();
        for (Listener l : analysis.listeners()) {
            if (!l.registered() || l.event() == null) {
                out.add(new Wired(l, null, null, "none", null, l.event() == null ? "its event couldn't be read" : "nothing registers it, so NeoForge doesn't call it"));
                continue;
            }
            String wrongBus = l.notes().stream().filter(n -> n.contains(WRONG_BUS)).findFirst().orElse(null);
            Match match = table.lookup(l.event());
            // The hook point named for the event itself; one for another phase, or that says it isn't its counterpart, isn't the listener's.
            boolean named = match.level() == Level.EXACT || match.level() == Level.APPROXIMATE || match.level() == Level.PENDING;
            Hook hook = named && match.hooks().size() == 1 ? match.hooks().get(0) : null;
            String why = match.why();
            boolean sure = match.level() == Level.EXACT || (approximate && match.level() == Level.APPROXIMATE);
            String unusable = hook == null ? null : unusable(hook);
            String sides = hook == null ? null : sides(l, hook);
            if (wrongBus != null) {
                why = "NeoForge never calls it: " + wrongBus;
                sure = false;
            } else if (sure && sides != null && !approximate) {
                why = why + ", but " + sides + ". `--allow-approximate` switches such listeners on";
                sure = false;
            } else if (sure && unusable != null) {
                why = why + ", but " + unusable;
                sure = false;
            } else if (match.level() == Level.APPROXIMATE && !approximate) {
                why = why + ". `--allow-approximate` switches such listeners on";
            }
            if (match.level() == Level.OWN) {
                out.add(new Wired(l, match.level(), null, "none", null, why));
                continue;
            }
            if (sure) {
                onHook.computeIfAbsent(hook.name(), k -> new ArrayList<>()).add(l);
                hooks.put(hook.name(), hook);
                out.add(new Wired(l, match.level(), hook.name(), "on", null, why));
                continue;
            }
            String id = unique(kebab(ClassData.simple(l.handlerClass()) + "-" + (l.lambda() || l.handlerMethod() == null ? "listener" : l.handlerMethod())), ids);
            List<String> block = new ArrayList<>();
            block.add("# Off: " + Report.handler(l) + " listens to NeoForge's " + eventName(l.event()) + ".");
            block.add("#   " + sentence(why));
            block.add("#   TODO: " + todo(match, hook, unusable != null));
            block.add("# [[injection]]");
            block.add("# id = \"" + id + "\"");
            block.add("# kind = \"HEAD\"");
            block.add("# target = \"" + (hook != null && hook.target() != null && hook.fragment() == null ? hook.target() : "TODO") + "\"");
            offBlocks.add(block.toArray(new String[0]));
            out.add(new Wired(l, match.level(), hook == null ? null : hook.name(), "off", id, why));
        }

        StringBuilder added = new StringBuilder();
        int api = 0;
        for (Map.Entry<String, List<Listener>> e : onHook.entrySet()) {
            Hook hook = hooks.get(e.getKey());
            if (hook.api() != null) api = Math.max(api, hook.api());
            String[] there = taken.get(hook.target());
            String id;
            if (there != null) {
                // One injection a function: the helper that is on it already takes these listeners too.
                id = there[0];
                Path helper = mod.resolve(there[1]);
                Files.writeString(helper, merged(Files.readString(helper, StandardCharsets.UTF_8), hook, e.getValue()), StandardCharsets.UTF_8);
            } else {
                id = unique(hook.name().replace('_', '-'), ids);
                String function = modId.replaceAll("[^A-Za-z0-9_]", "_") + "_" + hook.name();
                Files.writeString(mod.resolve(id + ".rs"), helper(hook, e.getValue(), function, module, gameSide), StandardCharsets.UTF_8);
                added.append("\n[[injection]]\nid = \"").append(id).append("\"\nkind = \"HEAD\"\ntarget = \"").append(hook.target())
                        .append("\"\nhelper = \"").append(id).append(".rs\"\nbindings = [\n");
                for (Binding b : hook.bindings()) {
                    added.append("  { name = \"").append(b.name()).append("\", ty = \"").append(b.type()).append("\", value = \"").append(b.value()).append("\" },\n");
                }
                added.append("]\n");
            }
            for (int i = 0; i < out.size(); i++) {
                Wired w = out.get(i);
                if (w.state().equals("on") && w.hook().equals(hook.name()) && w.injection() == null) {
                    out.set(i, new Wired(w.listener(), w.level(), w.hook(), "on", id, w.why()));
                }
            }
        }
        if (added.length() > 0 || !offBlocks.isEmpty()) {
            StringBuilder text = new StringBuilder(manifest.stripTrailing()).append("\n\n# The mod's NeoForge event listeners, put on hook points by horizon-port-neoforge from Horizon's hook\n")
                    .append("# catalogue (native-hooks.json). On: the catalogue names one hook point for the event, with nothing more to read.\n");
            text.append(added);
            for (String[] block : offBlocks) text.append('\n').append(String.join("\n", block)).append('\n');
            Files.writeString(manifestFile, text, StandardCharsets.UTF_8);
        }
        if (api > 0) needs(mod.getParent().getParent().resolve("compatibility.json"), api);
        return out;
    }

    /** Why a hook point can't be written a helper for, in words; null when it can. */
    static String unusable(Hook hook) {
        if (hook.fragment() != null) return "it isn't in Horizon's hooks API yet";
        if (hook.target() == null || hook.signature() == null || hook.bindings().isEmpty()) {
            return "horizon-port read no signature for `" + hook.name() + "` in this Horizon source, so its helper can't be written";
        }
        Binding first = hook.bindings().get(0);
        if (!first.name().equals("game") || !(first.type().equals("&mut crate::game::Game<'_>") || first.type().equals("&crate::game::Game<'_>"))) {
            return "`" + hook.name() + "` isn't handed the game first (`" + hook.signature() + "`), and this tool only writes helpers for hook points that are";
        }
        // A helper is pasted into Horizon's source: a type it names has to be found from anywhere there.
        for (Binding b : hook.bindings()) {
            Matcher type = Pattern.compile("(?<![\\w:])[A-Z]\\w*").matcher(b.type());
            while (type.find()) {
                if (!Set.of("Option", "Vec", "String", "Box", "Result").contains(type.group())) {
                    return "its parameter `" + b.name() + ": " + b.type() + "` names a type without its path, so a helper can't be written for it blindly";
                }
            }
        }
        return null;
    }

    private static String todo(Match match, Hook hook, boolean unusable) {
        return switch (match.level()) {
            case EXACT, APPROXIMATE -> unusable ? "write the helper for `" + hook.name() + "` by hand (HOOKS.md of `horizon-port hooks` has its bindings), then uncomment this."
                    : "read what Horizon says of `" + hook.name() + "`; if it is what the handler needs, write its helper and uncomment this.";
            case SEVERAL -> "pick the hook point the handler needs, put its target here, and write its helper.";
            case PHASE -> "see whether the handler can do its work at that other point; if not, Horizon needs a hook point for this event.";
            case PENDING -> "wait for that hook point to be in Horizon's hooks API (or build on the Horizon that has it), then put its target here.";
            case DATA -> "write what the handler builds as data, in a content mod (.hmod).";
            default -> "find where Horizon does this, or ask for a hook point; then put its target here and write its helper.";
        };
    }

    // ---- a helper ----

    /** The helper of a hook point that listeners are put on: it does nothing yet. */
    static String helper(Hook hook, List<Listener> listeners, String function, String module, boolean gameSide) {
        StringBuilder b = new StringBuilder();
        b.append("// The `").append(hook.name()).append("` hook point").append(hook.side() == null ? "" : " (" + hook.side() + ")")
                .append(", for the mod's NeoForge listeners. Written by horizon-port-neoforge:\n");
        b.append("// Horizon's hook catalogue (native-hooks.json) names it for ").append(hook.label()).append(".\n");
        b.append(standsFor(hook, listeners));
        if (hook.doc() != null && !hook.doc().isBlank()) b.append("//\n// What Horizon says of it: ").append(hook.doc().replaceAll("\\s+", " ").strip()).append('\n');
        b.append("fn ").append(function).append('(');
        List<String> names = new ArrayList<>();
        for (int i = 0; i < hook.bindings().size(); i++) {
            Binding binding = hook.bindings().get(i);
            b.append(i == 0 ? "" : ", ").append(binding.name()).append(": ").append(binding.type());
            names.add(binding.name());
        }
        b.append(") {\n");
        b.append("    use crate::game::").append(module).append(" as m;\n");
        b.append("    // Turned off in Options > Mods: nothing until the next build leaves it out.\n");
        b.append("    if !game.native_on(m::ID) {\n        return;\n    }\n");
        if (gameSide && hook.bindings().get(0).type().startsWith("&mut ")) {
            b.append("    // The mod's data folder and its initializer, the first time.\n");
            b.append("    crate::game::").append(module).append("_game::ensure_init(game);\n");
        }
        for (Listener l : listeners) {
            b.append("    // TODO: port ").append(Report.handler(l)).append(" here. Its Java takes a NeoForge event, so it isn't translated (report/translation.md).\n");
        }
        b.append("    let _ = (").append(String.join(", ", names)).append(", &m::ID);\n}\n");
        return b.toString();
    }

    /** The comment lines that say which listeners a helper stands in for, and what to mind for each. */
    private static String standsFor(Hook hook, List<Listener> listeners) {
        StringBuilder b = new StringBuilder("// It stands in for:\n");
        String cancel = hook.bindings().stream().filter(x -> x.type().equals("&mut bool") && x.name().startsWith("cancel")).map(Binding::name).findFirst().orElse(null);
        for (Listener l : listeners) {
            b.append("//   ").append(Report.handler(l)).append(", which listens to ").append(eventName(l.event())).append(" (").append(l.registeredBy()).append(")\n");
            if (!l.priority().equals("NORMAL")) b.append("//     Its priority is ").append(l.priority()).append(": Horizon calls mods in their own order.\n");
            if (cancel != null && !l.receiveCanceled()) b.append("//     NeoForge doesn't call it once the event is cancelled: check `*").append(cancel).append("` first.\n");
            if (l.sideFrom() != null && hook.side() != null && !l.side().equals(hook.side())) {
                b.append("//     The mod has it on the ").append(l.side()).append(" only (").append(l.sideFrom()).append("); the hook point is ")
                        .append(hook.side()).append("-side: check game.native_window().\n");
            }
        }
        return b.toString();
    }

    /** A helper that is on the hook point already, with these listeners added to what it stands in for. */
    static String merged(String helper, Hook hook, List<Listener> listeners) {
        List<String> lines = new ArrayList<>(List.of(helper.split("\n", -1)));
        int fn = 0;
        while (fn < lines.size() && !lines.get(fn).startsWith("fn ") && !lines.get(fn).startsWith("pub fn ")) fn++;
        List<String> more = new ArrayList<>();
        more.add("//");
        more.add("// Added by horizon-port-neoforge: Horizon's hook catalogue names this hook point for " + hook.label() + " too.");
        more.addAll(List.of(standsFor(hook, listeners).stripTrailing().split("\n")));
        for (Listener l : listeners) more.add("// TODO: port " + Report.handler(l) + " here as well.");
        lines.addAll(Math.min(fn, lines.size()), more);
        return String.join("\n", lines);
    }

    // ---- the hooks API a bundle needs ----

    /** Raises compatibility.json's `hooks` to the API the hook points switched on came in, when it names one. */
    static void needs(Path compatibility, int api) throws IOException {
        if (!Files.isRegularFile(compatibility)) return;
        String text = Files.readString(compatibility, StandardCharsets.UTF_8);
        Matcher hooks = Pattern.compile("(\"hooks\"\\s*:\\s*)(\\d+)").matcher(text);
        if (hooks.find() && Integer.parseInt(hooks.group(2)) < api) {
            Files.writeString(compatibility, text.substring(0, hooks.start()) + hooks.group(1) + api + text.substring(hooks.end()), StandardCharsets.UTF_8);
        }
    }

    // ---- words ----

    /** `net/neoforged/.../ServerTickEvent$Post` as `ServerTickEvent.Post`. */
    static String eventName(String eventClass) {
        return eventClass.substring(eventClass.lastIndexOf('/') + 1).replace('$', '.');
    }

    /**
     * Why a listener and the hook point named for its event don't sit on the same side, in words; null
     * when nothing says so. The mod restricts the listener to one side (`@OnlyIn`, a @Mod class or an
     * @EventBusSubscriber for that side) and Horizon's catalogue gives the hook point the other one.
     */
    static String sides(Listener l, Hook hook) {
        boolean opposite = ("client".equals(l.side()) && "server".equals(hook.side())) || ("server".equals(l.side()) && "client".equals(hook.side()));
        if (!opposite) return null;
        return "the mod keeps this listener to the " + l.side() + (l.sideFrom() == null ? "" : " (" + l.sideFrom() + ")") + " and Horizon's catalogue gives `" + hook.name()
                + "` the " + hook.side() + " side: it isn't certain that the hook point tells what the listener hears";
    }

    /** `ClientEvents-onClientTick` as `client-events-on-client-tick`. */
    static String kebab(String name) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                // A new word: after a small letter or a digit, or at the last capital of a run (`HUDOverlay` is hud-overlay).
                boolean afterCapital = i > 0 && Character.isUpperCase(name.charAt(i - 1));
                boolean beforeSmall = i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1));
                if (b.length() > 0 && b.charAt(b.length() - 1) != '-' && (!afterCapital || beforeSmall)) b.append('-');
                b.append(Character.toLowerCase(c));
            } else if (c < 128 && Character.isLetterOrDigit(c)) {
                b.append(c);
            } else if (b.length() > 0 && b.charAt(b.length() - 1) != '-') {
                b.append('-');
            }
        }
        while (b.length() > 0 && b.charAt(b.length() - 1) == '-') b.setLength(b.length() - 1);
        return b.length() == 0 ? "listener" : b.toString();
    }

    static String unique(String id, Set<String> ids) {
        String candidate = id;
        for (int n = 2; !ids.add(candidate); n++) candidate = id + "-" + n;
        return candidate;
    }

    private static String sentence(String words) {
        String s = Character.toUpperCase(words.charAt(0)) + words.substring(1);
        return s.endsWith(".") ? s : s + ".";
    }
}
