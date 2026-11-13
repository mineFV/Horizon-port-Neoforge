package horizonneo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Which event bus an event goes to. NeoForge posts an event on the mod bus when its class implements
 * IModBusEvent, and on the game bus (NeoForge.EVENT_BUS) otherwise. Whether a class implements it is
 * written in NeoForge's own jars, not in the mod's, so the answer comes from one of:
 *
 * - NeoForge's jars, when they are given (`--neoforge-jar`): the class's real ancestry;
 * - NeoForge's documentation, for the few events it names as being on one bus or the other.
 *
 * Anything else is UNKNOWN. A name's look ("Register...Event") is never taken as an answer.
 */
final class BusFacts {
    enum Bus { MOD, GAME, UNKNOWN }

    /** from: where the answer comes from, in words; null when the bus is unknown. */
    record Answer(Bus bus, String from) {}

    /** The interface that marks a mod bus event. */
    static final String MARKER = "net/neoforged/fml/event/IModBusEvent";

    /** Where an answer comes from: the event class's superclasses and interfaces, or the documentation. */
    static final String FROM_ANCESTRY = "the event class's own ancestry";
    static final String FROM_DOCS = "NeoForge's documentation";

    private static final Answer UNKNOWN = new Answer(Bus.UNKNOWN, null);

    /**
     * What NeoForge's "Events" page (26.1) says by name, as class names without their packages: the mod
     * lifecycle's events and its "other mod bus events", and its examples of game bus events.
     */
    private static final Set<String> DOCUMENTED_MOD = Set.of(
            "NewRegistryEvent", "DataPackRegistryEvent$NewRegistry", "RegisterEvent",
            "RegisterColorHandlersEvent$BlockTintSources", "RegisterColorHandlersEvent$ItemTintSources", "RegisterColorHandlersEvent$ColorResolvers",
            "ModelEvent$BakingCompleted", "TextureAtlasStitchedEvent");
    private static final Set<String> DOCUMENTED_GAME = Set.of("LivingEvent$LivingJumpEvent", "RenderNameTagEvent$CanRender", "MobDespawnEvent");

    /** A class of NeoForge's jars: its superclass and its interfaces. */
    private final Map<String, List<String>> ancestry;

    private BusFacts(Map<String, List<String>> ancestry) {
        this.ancestry = ancestry;
    }

    /** Only what the documentation says. */
    static BusFacts documented() {
        return new BusFacts(Map.of());
    }

    /** With NeoForge's jars (its own, FML's, the event bus's: any of them helps), read for their classes' ancestry. */
    static BusFacts fromJars(List<Path> jars) throws IOException {
        Map<String, List<String>> ancestry = new HashMap<>();
        for (Path jar : jars) {
            if (!Files.isRegularFile(jar)) throw new IOException("--neoforge-jar: " + jar + " isn't a file");
            int read = 0;
            try (ZipFile zip = new ZipFile(jar.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    if (e.isDirectory() || !e.getName().endsWith(".class")) continue;
                    try {
                        ClassData c = ClassData.parse(ModJar.read(zip, e, Analyzer.MAX_CLASS));
                        List<String> up = new ArrayList<>(c.interfaces);
                        if (c.superName != null) up.add(c.superName);
                        ancestry.put(c.name, List.copyOf(up));
                        read++;
                    } catch (IOException unreadable) {
                        // A class this tool can't read tells nothing; the others still do.
                    }
                }
            } catch (java.util.zip.ZipException e) {
                throw new IOException("--neoforge-jar: " + jar.getFileName() + " isn't a jar (a zip file)");
            }
            if (read == 0) throw new IOException("--neoforge-jar: " + jar.getFileName() + " holds no class");
        }
        return new BusFacts(ancestry);
    }

    /** Whether NeoForge's jars were given. */
    boolean hasJars() {
        return !ancestry.isEmpty();
    }

    /**
     * The bus of `event` (an internal name). `own`: the mod's own classes, in case the event is one of
     * them, or extends one.
     */
    Answer of(String event, Map<String, ClassData> own) {
        Bus fromAncestry = walk(event, own, new HashSet<>());
        if (fromAncestry != Bus.UNKNOWN) return new Answer(fromAncestry, FROM_ANCESTRY);
        // FML has only a mod bus: its lifecycle and config events are the documentation's "mod lifecycle".
        if (event.startsWith("net/neoforged/fml/event/")) return new Answer(Bus.MOD, FROM_DOCS);
        if (event.startsWith("net/neoforged/")) {
            String simple = event.substring(event.lastIndexOf('/') + 1);
            if (DOCUMENTED_MOD.contains(simple)) return new Answer(Bus.MOD, FROM_DOCS);
            if (DOCUMENTED_GAME.contains(simple)) return new Answer(Bus.GAME, FROM_DOCS);
        }
        return UNKNOWN;
    }

    /** MOD when an ancestor is the marker; GAME when every ancestor is known and none is; else UNKNOWN. */
    private Bus walk(String name, Map<String, ClassData> own, Set<String> seen) {
        if (name.equals(MARKER)) return Bus.MOD;
        // Java's own classes and the event bus's can't lead to FML's marker: FML is built on them.
        if (name.startsWith("java/") || name.startsWith("net/neoforged/bus/api/")) return Bus.GAME;
        if (!seen.add(name)) return Bus.GAME;
        List<String> up = ancestry.get(name);
        if (up == null && own.containsKey(name)) {
            ClassData c = own.get(name);
            up = new ArrayList<>(c.interfaces);
            if (c.superName != null) up.add(c.superName);
        }
        if (up == null) return Bus.UNKNOWN;
        boolean unknown = false;
        for (String parent : up) {
            Bus bus = walk(parent, own, seen);
            if (bus == Bus.MOD) return Bus.MOD;
            if (bus == Bus.UNKNOWN) unknown = true;
        }
        return unknown ? Bus.UNKNOWN : Bus.GAME;
    }
}
