package horizonneo;

import horizonneo.Analysis.AccessFile;
import horizonneo.Analysis.ApiArea;
import horizonneo.Analysis.ConfigUse;
import horizonneo.Analysis.EntryPoint;
import horizonneo.Analysis.GameClass;
import horizonneo.Analysis.Injector;
import horizonneo.Analysis.Listener;
import horizonneo.Analysis.MixinClass;
import horizonneo.Analysis.Registration;
import horizonneo.Analysis.Registry;
import horizonneo.BusFacts.Bus;
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

/**
 * What an analysed mod's parts become in Horizon, as far as this tool knows yet, written as
 * report.md (to read) and report.json (the same, for programs).
 *
 * Each part gets one of horizon-port's statuses, plus `info` for what needs no conversion:
 *
 * - auto: known in Horizon, the wiring can be generated;
 * - content: data and assets, a content mod (.hmod);
 * - needs-mapping: this kind of thing carries over, but its Horizon counterpart isn't looked up yet;
 * - needs-translation: no direct equivalent, someone has to rewrite it;
 * - unsupported: outside what this converter covers;
 * - info: nothing to convert.
 *
 * A status is never a guess: what isn't known to carry over is not `auto` or `content`.
 */
final class Report {
    private Report() {}

    /** One part of the mod and what becomes of it. horizon and note may be null. */
    record Feature(String category, String source, String detail, String horizon, String status, String note) {}

    private static final String[][] STATUSES = {
        {"auto", "Known in Horizon: the wiring can be generated."},
        {"content", "Data and assets: a content mod (.hmod)."},
        {"needs-mapping", "Carries over, but its Horizon counterpart isn't looked up yet."},
        {"needs-translation", "No direct equivalent: someone has to rewrite it."},
        {"unsupported", "Outside what this converter covers."},
        {"info", "Nothing to convert."},
    };

    private static final String LATER_EVENTS = "Its Horizon hook point comes from Horizon's hook catalogue when the mod is converted (`port`).";
    private static final String LATER_MIXINS = "Matched against Horizon's own functions by horizon-port when the mod is converted (`port`).";
    private static final String LATER_CONTENT = "Carried over by Horizon's horizon-port-pack when the mod is converted (`port`).";
    private static final String BY_HAND = "Not converted: it goes into PORTING.md as work to do by hand.";

    /** The game's packages a mod's class may extend into, and what such a class is; anything else is plain code. */
    private static final String[][] FAMILIES = {
        {"net/minecraft/world/level/block/entity/", "a block entity", "unsupported"},
        {"net/minecraft/world/level/block/", "a block", "needs-translation"},
        {"net/minecraft/world/item/", "an item", "needs-translation"},
        {"net/minecraft/world/entity/", "an entity", "unsupported"},
        {"net/minecraft/client/gui/", "a screen or a widget", "unsupported"},
        {"net/minecraft/world/inventory/", "a menu", "unsupported"},
        {"net/minecraft/client/renderer/", "rendering code", "unsupported"},
        {"net/minecraft/client/model/", "rendering code", "unsupported"},
        {"net/minecraft/client/particle/", "rendering code", "unsupported"},
        {"net/minecraft/world/level/levelgen/", "world generation in code", "unsupported"},
    };

    /** Writes dir/report.md and dir/report.json for a jar that was only analysed. */
    static void write(Path dir, ModJar jar, NeoForgeFrontend frontend, Analysis analysis) throws IOException {
        write(dir, jar, frontend, analysis, null);
    }

    /** The same; `converted` is what `port` made of the mod, null when it was only analysed. */
    static void write(Path dir, ModJar jar, NeoForgeFrontend frontend, Analysis analysis, Bundle.Outcome converted) throws IOException {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("report.md"), markdown(jar, frontend, analysis, converted), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("report.json"), Json.write(json(jar, frontend, analysis, converted)), StandardCharsets.UTF_8);
    }

    /** One line for the terminal. */
    static String summary(NeoForgeFrontend frontend, Analysis a) {
        ModInfo info = frontend.info();
        Map<String, Integer> counts = counts(features(a));
        List<String> parts = new ArrayList<>();
        for (String[] s : STATUSES) if (counts.get(s[0]) > 0) parts.add(counts.get(s[0]) + " " + s[0]);
        return info.name() + " " + info.version() + ": " + a.census().classes() + " classes, " + a.entryPoints().size() + " @Mod classes, "
                + a.listeners().size() + " event listeners, " + a.registries().stream().mapToInt(r -> r.entries().size()).sum() + " registrations, "
                + a.mixins().size() + " Mixin classes" + (parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")");
    }

    // ---- what each part becomes ----

    static List<Feature> features(Analysis a) {
        return features(a, null);
    }

    static List<Feature> features(Analysis a, Bundle.Outcome converted) {
        List<Feature> out = new ArrayList<>();
        for (EntryPoint e : a.entryPoints()) {
            out.add(new Feature("entrypoint", dotted(e.className()), "@Mod(\"" + e.modId() + "\"), " + sideWords(e.side()),
                    "the mod's row in Options > Mods", "auto",
                    e.unknownParameters().isEmpty() ? null
                            : "Its constructor takes " + String.join(", ", e.unknownParameters()) + ", which NeoForge doesn't hand a @Mod class."));
        }
        for (int i = 0; i < a.listeners().size(); i++) out.add(feature(a.listeners().get(i), wired(converted, i)));
        Set<String> registeredClasses = new HashSet<>();
        for (Registry r : a.registries()) {
            for (Registration e : r.entries()) {
                if (e.objectClass() != null) registeredClasses.add(e.objectClass());
                out.add(carried(feature(r, e, a), r, e, converted == null ? null : converted.content()));
            }
        }
        for (ConfigUse c : a.configs()) {
            out.add(new Feature("config", or(c.spec(), "?"), "a " + or(c.type(), "?") + " config, registered in " + c.registeredIn(), null, "needs-translation",
                    "Horizon reads config files for native mods (its `register_configs` hook); this converter doesn't write them yet."));
        }
        for (MixinClass m : a.mixins()) {
            String targets = m.targets().isEmpty() ? "?" : String.join(", ", m.targets().stream().map(ClassData::simple).toList());
            // A Mixin class no config lists is never applied: there is nothing of it to convert.
            String status = m.config() == null ? "info" : "needs-mapping";
            String note = m.config() == null ? "No Mixin config lists this class, so Mixin doesn't apply it." : LATER_MIXINS;
            boolean matched = converted != null && m.config() != null;
            if (m.injectors().isEmpty()) {
                out.add(new Feature("mixin", dotted(m.className()), "a Mixin into " + targets + " with no injector", null,
                        matched ? "needs-translation" : status,
                        matched ? "No injector: it adds members or an interface to a class of the game, which has to be done by hand." : note));
            }
            for (Injector i : m.injectors()) {
                String source = dotted(m.className()) + "." + i.handler();
                if (matched) out.add(matched(source, injectorWords(i, targets), converted.handlers().get(m.className() + "." + i.handler())));
                else out.add(new Feature("mixin", source, injectorWords(i, targets), null, status, note));
            }
        }
        for (AccessFile f : a.accessTransformers()) {
            out.add(new Feature("access", f.file(), f.lines().size() + (f.lines().size() == 1 ? " access transformer entry" : " access transformer entries")
                    + (f.declared() ? "" : " (the metadata doesn't list this file)"),
                    null, "info", "Nothing to convert on its own: a native mod's code sits inside Horizon's game module and reads its fields directly."));
        }
        for (GameClass g : a.gameClasses()) {
            if (registeredClasses.contains(g.className())) continue;
            String[] family = family(g.gameSuper());
            out.add(new Feature("class", dotted(g.className()), "extends " + ClassData.simple(g.gameSuper()) + (family == null ? "" : ": " + family[1]),
                    null, family == null ? "needs-translation" : family[2], family != null && family[2].equals("unsupported") ? BY_HAND : null));
        }
        for (ApiArea area : a.apiAreas()) {
            boolean buildTime = area.area().equals("data generators") || area.area().equals("game tests");
            out.add(new Feature("api", String.join(", ", area.usedBy().stream().map(ClassData::simple).toList()), "uses " + area.area(), null,
                    buildTime ? "info" : "unsupported", buildTime ? "It runs when the mod is built or tested, not in the game." : BY_HAND));
        }
        if (a.census().assets() + a.census().data() > 0) {
            Content.Result content = converted == null ? null : converted.content();
            out.add(new Feature("content", "the jar's resources", count(a.census().assets(), "asset file") + ", " + count(a.census().data(), "data file"),
                    content != null && content.packed() ? "`" + content.file() + "`" : "a content mod (.hmod)", "content",
                    content == null ? LATER_CONTENT : sentence(List.of(content.packed() ? "in `" + content.file() + "`, " + content.why() : content.why()))));
        }
        return out;
    }

    /**
     * A Mixin handler after `port`: horizon-port's status for it, unless the bundle doesn't have it
     * switched on, in which case it can't be `auto`.
     */
    private static Feature matched(String source, String detail, Bundle.Handler h) {
        if (h == null || h.status() == null) {
            return new Feature("mixin", source, detail, null, "needs-mapping", "horizon-port's report doesn't name it"
                    + (h == null || h.state().equals("absent") ? ", and it isn't in the bundle." : "."));
        }
        String where = switch (h.state()) {
            case "on" -> "On, in the injection `" + h.injection() + "`.";
            case "off" -> "Written commented out, as `" + h.injection() + "`.";
            default -> "Not in the bundle.";
        };
        String status = h.state().equals("on") || !h.status().equals("auto") ? h.status() : "needs-translation";
        return new Feature("mixin", source, detail, h.horizon(), status, h.note() == null ? where : where + " " + h.note());
    }

    /** What `port` made of the listener at that place in the analysis; null when it wasn't converted, or with no event table. */
    private static Wiring.Wired wired(Bundle.Outcome converted, int index) {
        return converted == null || index >= converted.listeners().size() ? null : converted.listeners().get(index);
    }

    private static Feature feature(Listener l, Wiring.Wired w) {
        String event = l.event() == null ? "an event that couldn't be read" : ClassData.simple(l.event());
        String detail = event + ", " + (l.bus() == Bus.UNKNOWN ? "bus not known" : Analyzer.word(l.bus()) + " bus") + ", " + sideWords(l.side());
        List<String> notes = new ArrayList<>(l.notes());
        String status = "needs-mapping";
        if (w != null && !(w.state().equals("none") && w.level() == null)) {
            // After `port`: the event table's answer, and where the listener is in the bundle.
            String hook = w.hook() == null ? null : "hook `" + w.hook() + "`";
            switch (w.state()) {
                case "on" -> {
                    notes.add("on, in the injection `" + w.injection() + "`: its handler is still to port");
                    return new Feature("listener", handler(l), detail, hook, "auto", sentence(notes));
                }
                case "off" -> notes.add("written commented out, as `" + w.injection() + "`");
                default -> { }
            }
            notes.add(w.why());
            status = switch (w.level()) {
                case EXACT, APPROXIMATE, SEVERAL, PHASE, PENDING -> "needs-mapping";
                case DATA -> "content";
                default -> "needs-translation";
            };
            return new Feature("listener", handler(l), detail, hook, status, sentence(notes));
        }
        if (l.registered()) {
            notes.add(LATER_EVENTS);
        } else if (l.registeredBy().startsWith("@EventBusSubscriber")) {
            // Sure: NeoForge takes such a class's static methods and nothing else.
            status = "info";
            notes.add(0, "not registered, so NeoForge doesn't call it");
        } else {
            // Not sure: a registration this didn't read may still exist.
            notes.add(0, "no registration that covers it was found: if nothing registers it, NeoForge never calls it");
            notes.add(LATER_EVENTS);
        }
        return new Feature("listener", handler(l), detail, null, status, sentence(notes));
    }

    /**
     * A block or item that is plain content, after `port`: whether the content mod declares it, and
     * that the vanilla template it copies there is a guess.
     */
    private static Feature carried(Feature f, Registry r, Registration e, Content.Result content) {
        if (content == null || !f.status().equals("content") || r.namespace() == null || e.name() == null) return f;
        if (!content.packed() && !content.state().equals("todo")) {
            return new Feature(f.category(), f.source(), f.detail(), f.horizon(), "content", "Not carried over: " + content.why() + ".");
        }
        String id = r.namespace() + ":" + e.name(), kind = "BLOCK".equals(r.kind()) ? "block" : "item";
        Content.Declared declared = null;
        for (Content.Declared d : content.declared()) {
            // A block's own item goes with its block.
            if (d.id().equals(id) && (d.kind().equals(kind) || d.kind().equals("block"))) declared = d;
        }
        String where = content.packed() ? "`" + content.file() + "`" : "`" + content.folder() + "/`, which isn't packed";
        if (declared == null) {
            return new Feature(f.category(), f.source(), f.detail(), null, "needs-translation",
                    "The content mod doesn't declare it: the jar has no blockstate or item model that Horizon's carry takes it from.");
        }
        return new Feature(f.category(), f.source(), f.detail(), content.packed() ? "`" + content.file() + "`" : null, content.packed() ? "content" : "needs-mapping",
                "Declared in " + where + ", as a copy of `" + declared.template() + "`: Horizon's guess, to check against the mod's code.");
    }

    private static Feature feature(Registry r, Registration e, Analysis a) {
        String kind = or(r.kind(), "?");
        String source = e.holderField() != null ? e.holderField() : ClassData.simple(r.declaredIn()) + "." + r.field();
        String detail = kind + " " + id(r, e) + (e.objectClass() == null ? "" : " (" + ClassData.simple(e.objectClass()) + ")");
        if (!kind.equals("BLOCK") && !kind.equals("ITEM")) {
            return new Feature("registry", source, detail, null, "needs-translation", "Registered in code: this converter only carries blocks and items so far.");
        }
        if (e.objectClass() == null) {
            return new Feature("registry", source, detail, null, "needs-translation", "What it registers couldn't be read, so it can't be said to be plain content.");
        }
        if (e.classFrom() != null && e.classFrom().contains("supertype")) {
            return new Feature("registry", source, detail, null, "needs-translation",
                    "What it makes couldn't be read: " + ClassData.simple(e.objectClass()) + " is only " + e.classFrom() + ".");
        }
        if (!e.objectClass().startsWith("net/minecraft/")) {
            for (GameClass g : a.gameClasses()) {
                if (g.className().equals(e.objectClass()) && !g.methods().isEmpty()) {
                    return new Feature("registry", source, detail, null, "needs-translation", "Its class has behaviour of its own (" + String.join(", ", g.methods())
                            + "): the block or item itself is content, that behaviour needs native code.");
                }
            }
        }
        return new Feature("registry", source, detail, "a content mod (.hmod)", "content", LATER_CONTENT);
    }

    static Map<String, Integer> counts(List<Feature> features) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String[] s : STATUSES) counts.put(s[0], 0);
        for (Feature f : features) counts.merge(f.status(), 1, Integer::sum);
        return counts;
    }

    private static String[] family(String gameClass) {
        for (String[] f : FAMILIES) if (gameClass.startsWith(f[0])) return f;
        return null;
    }

    // ---- report.md ----

    static String markdown(ModJar jar, NeoForgeFrontend frontend, Analysis a) {
        return markdown(jar, frontend, a, null);
    }

    static String markdown(ModJar jar, NeoForgeFrontend frontend, Analysis a, Bundle.Outcome converted) {
        ModInfo info = frontend.info();
        ModsToml toml = frontend.toml();
        List<Feature> features = features(a, converted);
        Map<String, Integer> counts = counts(features);
        StringBuilder b = new StringBuilder();
        b.append("# ").append(info.name()).append(' ').append(info.version()).append(": what it does, and what converts to Horizon\n\n");
        b.append('`').append(jar.name()).append("`, a NeoForge mod (`").append(info.id()).append("`). Declared licence: ")
                .append(toml.declaredLicense() == null ? "none" : "`" + toml.declaredLicense() + "`")
                .append(converted == null ? " (the `license` command says whether it may be ported).\n\n"
                        : " (horizon-port's licence rules allow porting it: see `PORTING.md`).\n\n");
        if (converted == null) {
            b.append("Read from the jar's class files and resources: names, annotations and the order of the calls in each method. ")
                    .append("Nothing was decompiled, and nothing of the mod was run.\n\n");
        } else {
            b.append("The Mixin matching and the Java to Rust translation are horizon-port's, done on a copy of the jar; the source it decompiled ")
                    .append("for that has been deleted. Everything else was read from the jar's class files and resources: names, annotations and ")
                    .append("the order of the calls in each method. Nothing of the mod was compiled or run.\n\n");
        }
        b.append("| Status | Parts | Meaning |\n|---|---:|---|\n");
        for (String[] s : STATUSES) b.append("| ").append(s[0]).append(" | ").append(counts.get(s[0])).append(" | ").append(s[1]).append(" |\n");

        if (converted != null) conversion(b, a, converted);

        b.append("\n## Entry points\n\n");
        if (a.entryPoints().isEmpty()) {
            b.append("No class has `@Mod`: the jar's metadata names a mod, but nothing starts it.\n");
        } else {
            b.append("| Class | Mod | Side | Its constructor takes |\n|---|---|---|---|\n");
            for (EntryPoint e : a.entryPoints()) {
                row(b, code(dotted(e.className())), code(e.modId()), e.side(), e.parameters().isEmpty() ? "nothing" : String.join(", ", e.parameters()));
            }
        }

        b.append("\n## Event listeners\n\n");
        if (a.listeners().isEmpty()) {
            b.append("None.\n");
        } else {
            b.append("| Event | Bus | Side | Handler | Registered by | Status |\n|---|---|---|---|---|---|\n");
            for (int i = 0; i < a.listeners().size(); i++) {
                Listener l = a.listeners().get(i);
                row(b, l.event() == null ? "not read" : code(ClassData.simple(l.event())), l.bus() == Bus.UNKNOWN ? "not known" : Analyzer.word(l.bus()), l.side(),
                        code(handler(l)), l.registered() ? l.registeredBy() : "not registered (" + l.registeredBy() + ")", feature(l, wired(converted, i)).status());
            }
            List<String> remarks = new ArrayList<>();
            for (Listener l : a.listeners()) {
                List<String> said = new ArrayList<>(l.notes());
                if (!l.priority().equals("NORMAL")) said.add("priority " + l.priority());
                if (l.receiveCanceled()) said.add("it also receives cancelled events");
                if (!said.isEmpty()) remarks.add("- " + code(handler(l)) + ": " + sentence(said));
            }
            if (!remarks.isEmpty()) b.append('\n').append(String.join("\n", remarks)).append('\n');
            long unknown = a.listeners().stream().filter(l -> l.bus() == Bus.UNKNOWN).count();
            if (unknown > 0 && !a.neoforgeJars()) {
                b.append("\nThe bus of ").append(unknown).append(unknown == 1 ? " listener isn't" : " listeners isn't")
                        .append(" known. Which bus an event is posted on is written in NeoForge's own jars, not in the mod's: ")
                        .append("give them with `--neoforge-jar` to have it read. It is never guessed from an event's name.\n");
            }
            b.append("\nHow each bus and side was told:\n\n");
            for (Listener l : a.listeners()) {
                b.append("- ").append(code(handler(l))).append(": ")
                        .append(l.bus() == Bus.UNKNOWN ? "bus not known" : Analyzer.word(l.bus()) + " bus (" + l.busFrom() + ")")
                        .append("; ").append(l.sideFrom() == null ? "either side (nothing restricts it)" : l.side() + " side (" + l.sideFrom() + ")").append(".\n");
            }
        }

        b.append("\n## Registries\n\n");
        if (a.registries().isEmpty()) {
            b.append("No `DeferredRegister`.\n");
        } else {
            b.append("| Registry | Id | Class | Registered with | Kept in | Status |\n|---|---|---|---|---|---|\n");
            for (Registry r : a.registries()) {
                for (Registration e : r.entries()) {
                    row(b, or(r.kind(), "not read"), code(id(r, e)), e.objectClass() == null ? "not read" : code(ClassData.simple(e.objectClass())),
                            code(e.how()), e.holderField() == null ? "" : code(e.holderField()),
                            carried(feature(r, e, a), r, e, converted == null ? null : converted.content()).status());
                }
                if (r.entries().isEmpty()) row(b, or(r.kind(), "not read"), "nothing read", "", "", code(ClassData.simple(r.declaredIn()) + "." + r.field()), "");
            }
            for (Registry r : a.registries()) {
                if (!r.onModBus()) {
                    b.append("\n`").append(ClassData.simple(r.declaredIn())).append('.').append(r.field())
                            .append("` isn't seen being put on the mod bus (`register(bus)`): NeoForge registers nothing from it unless that happens somewhere this didn't read.\n");
                }
            }
        }

        b.append("\n## Configs\n\n");
        if (a.configs().isEmpty()) {
            b.append("None registered.\n");
        } else {
            b.append("| Type | Spec | Registered in |\n|---|---|---|\n");
            for (ConfigUse c : a.configs()) row(b, or(c.type(), "not read"), c.spec() == null ? "not read" : code(c.spec()), c.registeredIn());
        }

        b.append("\n## Mixins\n\n");
        if (a.mixins().isEmpty()) {
            b.append("None.\n");
        } else {
            b.append("| Mixin class | Into | Injector | Listed in |\n|---|---|---|---|\n");
            for (MixinClass m : a.mixins()) {
                String targets = m.targets().isEmpty() ? "not read" : String.join(", ", m.targets().stream().map(t -> code(ClassData.simple(t))).toList());
                String listed = m.config() == null ? "no config" : code(m.config()) + " (" + m.environment() + ")";
                if (m.injectors().isEmpty()) row(b, code(ClassData.simple(m.className())), targets, "none", listed);
                for (Injector i : m.injectors()) row(b, code(ClassData.simple(m.className())), targets, injectorWords(i, null), listed);
            }
        }

        b.append("\n## Access transformers\n\n");
        if (a.accessTransformers().isEmpty()) {
            b.append("None.\n");
        } else {
            for (AccessFile f : a.accessTransformers()) {
                b.append('`').append(f.file()).append("`, ").append(f.lines().size()).append(f.lines().size() == 1 ? " entry" : " entries")
                        .append(f.declared() ? "" : " (in the jar, but the metadata doesn't list it)").append(":\n\n");
                for (int i = 0; i < f.lines().size() && i < 40; i++) b.append("- `").append(f.lines().get(i)).append("`\n");
                if (f.lines().size() > 40) b.append("- ... and ").append(f.lines().size() - 40).append(" more\n");
            }
        }

        b.append("\n## The rest of its code\n\n");
        if (a.gameClasses().isEmpty() && a.apiAreas().isEmpty()) b.append("No class extends a class of the game, and no other part of NeoForge's API is used.\n");
        if (!a.gameClasses().isEmpty()) {
            b.append("Classes that extend the game's:\n\n| Class | Extends | Methods of its own |\n|---|---|---|\n");
            for (GameClass g : a.gameClasses()) {
                row(b, code(ClassData.simple(g.className())), code(ClassData.simple(g.gameSuper())), g.methods().isEmpty() ? "none" : String.join(", ", g.methods()));
            }
        }
        if (!a.apiAreas().isEmpty()) {
            b.append("\nOther parts of NeoForge's API:\n\n");
            for (ApiArea area : a.apiAreas()) {
                b.append("- ").append(area.area()).append(", in ").append(String.join(", ", area.usedBy().stream().map(u -> code(ClassData.simple(u))).toList())).append('\n');
            }
        }

        b.append("\n## Content\n\n").append(a.census().classes()).append(a.census().classes() == 1 ? " class, " : " classes, ")
                .append(count(a.census().assets(), "asset file")).append(", ").append(count(a.census().data(), "data file")).append(".\n");

        if (!toml.dependencies().isEmpty()) {
            b.append("\n## Dependencies\n\n| Mod | Type | Versions | Side |\n|---|---|---|---|\n");
            for (ModsToml.Dependency d : toml.dependencies()) row(b, code(d.modId()), d.type(), code(d.versionRange()), d.side().toLowerCase(java.util.Locale.ROOT));
        }

        b.append("\n## Every part and its status\n\n| Part | What | In Horizon | Status | Note |\n|---|---|---|---|---|\n");
        for (Feature f : features) row(b, code(f.source()), f.detail(), or(f.horizon(), ""), f.status(), or(f.note(), ""));

        List<String> warnings = new ArrayList<>(info.warnings());
        warnings.addAll(a.warnings());
        if (!warnings.isEmpty()) {
            b.append("\n## Warnings\n\n");
            for (String w : warnings) b.append("- ").append(w).append(".\n");
        }
        return b.toString();
    }

    /** What `port` made: the bundle's injections, each Mixin handler, and how much Java became Rust. */
    private static void conversion(StringBuilder b, Analysis a, Bundle.Outcome o) {
        b.append("\n## What the conversion made\n\n");
        b.append("- Injections on: ").append(o.on().isEmpty() ? "none" : "`" + String.join("`, `", o.on()) + "`").append(".\n");
        b.append("- Injections written commented out: ").append(o.off().isEmpty() ? "none" : "`" + String.join("`, `", o.off()) + "`").append(".\n");
        Bundle.Translation t = o.translation();
        b.append("- Java translated to Rust: ");
        if (t == null) {
            b.append("horizon-port gave no count.\n");
        } else {
            b.append(t.methods()).append(" of ").append(t.methodsOf()).append(t.methodsOf() == 1 ? " method" : " methods");
            if (t.fields() != null) b.append(" and ").append(t.fields()).append(" of ").append(t.fieldsOf()).append(t.fieldsOf() == 1 ? " static field" : " static fields");
            b.append(", by horizon-port's count (`translation.md` lists each one and why).\n");
        }
        long listeners = a.listeners().stream().filter(Listener::registered).count();
        b.append("- Event listeners on a hook point: ").append(o.listenersOn()).append(" of ").append(listeners)
                .append(o.listeners().isEmpty() && listeners > 0 ? " (the Horizon source given has no hook catalogue, `native-hooks.json`, to go by).\n"
                        : " (from Horizon's hook catalogue; a listener that is on still has its handler to port).\n");
        if (o.listeners().stream().anyMatch(w -> !w.state().equals("none"))) {
            b.append("\n| Listener | NeoForge event | The table's answer | Hook point | In the bundle |\n|---|---|---|---|---|\n");
            for (Wiring.Wired w : o.listeners()) {
                if (w.state().equals("none")) continue;
                row(b, code(handler(w.listener())), Wiring.eventName(w.listener().event()), w.level().name().toLowerCase(java.util.Locale.ROOT),
                        w.hook() == null ? "" : code(w.hook()), w.state().equals("on") ? "on, in `" + w.injection() + "`" : "commented out, as `" + w.injection() + "`");
            }
        }
        if (o.content() != null) {
            Content.Result c = o.content();
            b.append("- Content: ").append(c.packed() ? "`" + c.file() + "`, " + c.why() : c.why()).append(".\n");
        }
        if (!o.handlers().isEmpty()) {
            b.append("\n| Mixin handler | horizon-port's status | In Horizon | In the bundle |\n|---|---|---|---|\n");
            for (Map.Entry<String, Bundle.Handler> e : o.handlers().entrySet()) {
                Bundle.Handler h = e.getValue();
                String where = switch (h.state()) {
                    case "on" -> "on, in `" + h.injection() + "`";
                    case "off" -> "commented out, as `" + h.injection() + "`";
                    default -> "not in it";
                };
                String name = e.getKey();
                row(b, code(ClassData.simple(name.substring(0, name.lastIndexOf('.'))) + name.substring(name.lastIndexOf('.'))),
                        or(h.status(), "not reported"), or(h.horizon(), ""), where);
            }
        }
        if (!o.changes().isEmpty()) {
            b.append("\nChanged after horizon-port wrote the bundle:\n\n");
            for (String c : o.changes()) b.append("- ").append(c).append('\n');
        }
    }

    // ---- report.json ----

    static Map<String, Object> json(ModJar jar, NeoForgeFrontend frontend, Analysis a) {
        return json(jar, frontend, a, null);
    }

    static Map<String, Object> json(ModJar jar, NeoForgeFrontend frontend, Analysis a, Bundle.Outcome converted) {
        ModInfo info = frontend.info();
        ModsToml toml = frontend.toml();
        List<Feature> features = features(a, converted);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("tool", "horizon-port-neoforge");
        root.put("schema", 1);
        root.put("jar", jar.name());
        root.put("mod", map("id", info.id(), "name", info.name(), "version", info.version(), "description", info.description(),
                "loader", info.loader(), "license", toml.declaredLicense(), "authors", toml.mods().get(0).authors()));
        root.put("counts", counts(features));
        if (converted != null) {
            Bundle.Translation t = converted.translation();
            root.put("conversion", map("bundle", converted.modId(), "injectionsOn", converted.on(), "injectionsOff", converted.off(),
                    "translation", t == null ? null : map("methods", t.methods(), "methodsOf", t.methodsOf(), "fields", t.fields(), "fieldsOf", t.fieldsOf()),
                    "listenersOn", (int) converted.listenersOn(), "listeners", (int) a.listeners().stream().filter(Listener::registered).count(),
                    "changes", converted.changes(), "decompiledLinesRemoved", converted.javaLines()));
            Content.Result c = converted.content();
            if (c != null) {
                List<Object> declared = new ArrayList<>();
                for (Content.Declared d : c.declared()) declared.add(map("kind", d.kind(), "id", d.id(), "template", d.template(), "registered", d.registered()));
                root.put("content", map("state", c.state(), "file", c.file(), "folder", c.folder(), "files", c.files(), "summary", c.summary(), "ports", c.ports(),
                        "skipped", c.skipped(), "declared", declared, "undeclared", c.undeclared(), "leftOut", c.leftOut(), "why", c.why()));
            }
        }
        root.put("census", map("classes", a.census().classes(), "assets", a.census().assets(), "data", a.census().data()));

        List<Object> entryPoints = new ArrayList<>();
        for (EntryPoint e : a.entryPoints()) {
            entryPoints.add(map("class", dotted(e.className()), "modId", e.modId(), "side", e.side(), "parameters", e.parameters(),
                    "unknownParameters", e.unknownParameters()));
        }
        root.put("entryPoints", entryPoints);

        List<Object> listeners = new ArrayList<>();
        for (int i = 0; i < a.listeners().size(); i++) {
            Listener l = a.listeners().get(i);
            Wiring.Wired w = wired(converted, i);
            Map<String, Object> listener = map("event", l.event() == null ? null : dotted(l.event()), "handlerClass", dotted(l.handlerClass()),
                    "handlerMethod", l.handlerMethod(), "lambda", l.lambda(), "registeredBy", l.registeredBy(), "registered", l.registered(),
                    "bus", Analyzer.word(l.bus()), "busFrom", l.busFrom(), "side", l.side(), "sideFrom", l.sideFrom(),
                    "priority", l.priority(), "receiveCanceled", l.receiveCanceled(), "hook", w == null ? null : w.hook(), "status", feature(l, w).status(),
                    "notes", l.notes());
            if (w != null) {
                listener.put("converted", map("level", w.level() == null ? null : w.level().name().toLowerCase(java.util.Locale.ROOT), "bundle", w.state(),
                        "injection", w.injection(), "why", w.why()));
            }
            listeners.add(listener);
        }
        root.put("listeners", listeners);

        List<Object> registries = new ArrayList<>();
        for (Registry r : a.registries()) {
            List<Object> entries = new ArrayList<>();
            for (Registration e : r.entries()) {
                Feature f = carried(feature(r, e, a), r, e, converted == null ? null : converted.content());
                entries.add(map("id", id(r, e), "how", e.how(), "class", e.objectClass() == null ? null : dotted(e.objectClass()), "classFrom", e.classFrom(),
                        "holder", e.holderField(), "status", f.status(), "note", f.note()));
            }
            registries.add(map("registry", r.kind(), "namespace", r.namespace(), "declaredIn", dotted(r.declaredIn()), "field", r.field(),
                    "onModBus", r.onModBus(), "entries", entries));
        }
        root.put("registries", registries);

        List<Object> configs = new ArrayList<>();
        for (ConfigUse c : a.configs()) configs.add(map("type", c.type(), "spec", c.spec(), "registeredIn", c.registeredIn()));
        root.put("configs", configs);

        List<Object> mixins = new ArrayList<>();
        for (MixinClass m : a.mixins()) {
            List<Object> injectors = new ArrayList<>();
            for (Injector i : m.injectors()) {
                Map<String, Object> injector = map("annotation", i.annotation(), "handler", i.handler(), "methods", i.methods(), "at", i.at(),
                        "atTarget", i.atTarget(), "cancellable", i.cancellable());
                Bundle.Handler h = converted == null ? null : converted.handlers().get(m.className() + "." + i.handler());
                if (h != null) {
                    injector.put("converted", map("status", h.status(), "horizon", h.horizon(), "note", h.note(), "bundle", h.state(), "injection", h.injection()));
                }
                injectors.add(injector);
            }
            mixins.add(map("class", dotted(m.className()), "config", m.config(), "environment", m.environment(),
                    "targets", m.targets().stream().map(Report::dotted).toList(), "injectors", injectors));
        }
        root.put("mixins", mixins);

        List<Object> transformers = new ArrayList<>();
        for (AccessFile f : a.accessTransformers()) transformers.add(map("file", f.file(), "declared", f.declared(), "entries", f.lines()));
        root.put("accessTransformers", transformers);

        List<Object> gameClasses = new ArrayList<>();
        for (GameClass g : a.gameClasses()) gameClasses.add(map("class", dotted(g.className()), "extends", dotted(g.gameSuper()), "methods", g.methods()));
        root.put("gameClasses", gameClasses);

        List<Object> areas = new ArrayList<>();
        for (ApiArea area : a.apiAreas()) areas.add(map("area", area.area(), "usedBy", area.usedBy().stream().map(Report::dotted).toList()));
        root.put("apiAreas", areas);

        List<Object> dependencies = new ArrayList<>();
        for (ModsToml.Dependency d : toml.dependencies()) {
            dependencies.add(map("of", d.of(), "modId", d.modId(), "type", d.type(), "versionRange", d.versionRange(), "side", d.side()));
        }
        root.put("dependencies", dependencies);

        List<Object> rows = new ArrayList<>();
        for (Feature f : features) {
            rows.add(map("category", f.category(), "source", f.source(), "detail", f.detail(), "horizon", f.horizon(), "status", f.status(), "note", f.note()));
        }
        root.put("features", rows);
        root.put("neoforgeJars", a.neoforgeJars());
        List<String> warnings = new ArrayList<>(info.warnings());
        warnings.addAll(a.warnings());
        root.put("warnings", warnings);
        return root;
    }

    // ---- words ----

    /** A listener's handler as `Class.method`; a lambda written at the call is named by where it is. */
    static String handler(Listener l) {
        String owner = ClassData.simple(l.handlerClass());
        if (l.handlerMethod() == null) return owner + " (handler not read)";
        return l.lambda() ? owner + " (a lambda)" : owner + "." + l.handlerMethod();
    }

    private static String id(Registry r, Registration e) {
        return or(r.namespace(), "?") + ":" + or(e.name(), "?");
    }

    private static String injectorWords(Injector i, String targets) {
        StringBuilder b = new StringBuilder("@").append(i.annotation());
        if (i.at() != null) b.append('(').append(i.at()).append(')');
        if (!i.methods().isEmpty()) b.append(" on ").append(targets == null ? "" : targets + ".").append(String.join(", ", i.methods()));
        if (i.atTarget() != null) b.append(" at ").append(i.atTarget());
        if (i.cancellable()) b.append(", cancellable");
        return b.toString();
    }

    private static String sideWords(String side) {
        return switch (side) {
            case "client" -> "client only";
            case "server" -> "dedicated server only";
            case "both" -> "both sides";
            default -> "either side";
        };
    }

    /** Notes as one sentence each, null when there are none. */
    private static String sentence(List<String> notes) {
        if (notes.isEmpty()) return null;
        StringBuilder b = new StringBuilder();
        for (String n : notes) {
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(n.charAt(0))).append(n, 1, n.length());
            if (!n.endsWith(".")) b.append('.');
        }
        return b.toString();
    }

    /** "1 asset file", "2 asset files". */
    private static String count(int n, String one) {
        return n + " " + one + (n == 1 ? "" : "s");
    }

    /** `com/example/Mod$Inner` as `com.example.Mod$Inner`: a class's name as Java tools write it. */
    static String dotted(String internalName) {
        return internalName.replace('/', '.');
    }

    private static String or(String value, String fallback) {
        return value != null ? value : fallback;
    }

    private static String code(String text) {
        return "`" + text + "`";
    }

    private static void row(StringBuilder b, String... cells) {
        b.append('|');
        for (String cell : cells) b.append(' ').append(cell.replace("|", "\\|").replaceAll("\\s+", " ")).append(" |");
        b.append('\n');
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
        return m;
    }
}
