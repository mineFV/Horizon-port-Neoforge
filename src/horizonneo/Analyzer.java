package horizonneo;

import horizonneo.Analysis.AccessFile;
import horizonneo.Analysis.ApiArea;
import horizonneo.Analysis.Census;
import horizonneo.Analysis.ConfigUse;
import horizonneo.Analysis.EntryPoint;
import horizonneo.Analysis.GameClass;
import horizonneo.Analysis.Injector;
import horizonneo.Analysis.Listener;
import horizonneo.Analysis.MixinClass;
import horizonneo.Analysis.Registration;
import horizonneo.Analysis.Registry;
import horizonneo.BusFacts.Bus;
import horizonneo.ClassData.Kind;
import horizonneo.ClassData.Op;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads a NeoForge mod's jar into an `Analysis`: its `@Mod` classes, its event listeners with the bus
 * and the side of each, what it registers, its configs, its Mixin classes and its access transformers.
 *
 * It goes by names, annotations and the order of the steps in each method (`ClassData`). Where that
 * isn't enough to be sure (a listener kept in a variable, a bus passed around), it says it couldn't
 * read it. It doesn't guess.
 */
final class Analyzer {
    /** The most one class file may unpack to, and all of a jar's classes together. */
    static final int MAX_CLASS = 16 << 20;
    private static final long MAX_CLASSES = 1L << 30;

    // NeoForge's, FML's and the event bus's names.
    static final String MOD = "Lnet/neoforged/fml/common/Mod;";
    static final String SUBSCRIBER = "Lnet/neoforged/fml/common/EventBusSubscriber;";
    /** Where the same annotation was before it got a file of its own. */
    static final String SUBSCRIBER_NESTED = "Lnet/neoforged/fml/common/Mod$EventBusSubscriber;";
    static final String SUBSCRIBE = "Lnet/neoforged/bus/api/SubscribeEvent;";
    static final String ONLY_IN = "Lnet/neoforged/api/distmarker/OnlyIn;";
    static final String EVENT_BUS = "net/neoforged/bus/api/IEventBus";
    static final String BASE_EVENT = "net/neoforged/bus/api/Event";
    static final String PRIORITY = "net/neoforged/bus/api/EventPriority";
    static final String NEOFORGE = "net/neoforged/neoforge/common/NeoForge";
    static final String MOD_CONTAINER = "net/neoforged/fml/ModContainer";
    static final String FML_MOD_CONTAINER = "net/neoforged/fml/javafmlmod/FMLModContainer";
    static final String DIST = "net/neoforged/api/distmarker/Dist";
    static final String DEFERRED_REGISTER = "net/neoforged/neoforge/registries/DeferredRegister";
    static final String CONFIG_TYPE = "net/neoforged/fml/config/ModConfig$Type";
    static final String CONFIG_SCREEN = "net/neoforged/neoforge/client/gui/IConfigScreenFactory";
    static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    static final String DEFAULT_TRANSFORMER = "META-INF/accesstransformer.cfg";

    private static final String EVENT_BUS_TYPE = "L" + EVENT_BUS + ";";
    private static final String CONSUMER = "java/util/function/Consumer";

    /** What NeoForge hands a `@Mod` class's constructor (its "Mod Files" page). */
    private static final Set<String> HANDED = Set.of(EVENT_BUS, MOD_CONTAINER, FML_MOD_CONTAINER, DIST);

    /** Mixin's annotations on a method that mark it without making it an injector. */
    private static final Set<String> MIXIN_MARKS = Set.of("Shadow", "Unique", "Final", "Mutable", "Intrinsic", "SoftOverride", "Dynamic", "Debug");

    /** Registers NeoForge names by what they hold. */
    private static final Map<String, String> NAMED_REGISTERS = Map.of(
            "createBlocks", "BLOCK", "createItems", "ITEM", "createDataComponents", "DATA_COMPONENT_TYPE", "createEntities", "ENTITY_TYPE");

    /** The helpers that always register the game's own class. */
    private static final Map<String, String> SIMPLE_REGISTRATIONS = Map.of(
            "registerSimpleBlock", "net/minecraft/world/level/block/Block",
            "registerSimpleItem", "net/minecraft/world/item/Item",
            "registerSimpleBlockItem", "net/minecraft/world/item/BlockItem");

    /** Parts of NeoForge's API, by the package their classes are in. */
    private static final String[][] AREAS = {
        {"net/neoforged/neoforge/network/", "custom network payloads"},
        {"net/neoforged/neoforge/capabilities/", "capabilities"},
        {"net/neoforged/neoforge/attachment/", "data attachments"},
        {"net/neoforged/neoforge/registries/datamaps/", "data maps"},
        {"net/neoforged/neoforge/fluids/", "NeoForge's fluid API"},
        {"net/neoforged/neoforge/energy/", "NeoForge's energy API"},
        {"net/neoforged/neoforge/items/", "NeoForge's item handlers"},
        {"net/neoforged/neoforge/transfer/", "NeoForge's transfer API"},
        {"net/neoforged/neoforge/data/", "data generators"},
        {"net/neoforged/neoforge/common/data/", "data generators"},
        {"net/neoforged/neoforge/gametest/", "game tests"},
    };

    private final ModJar jar;
    private final ModsToml toml;
    private final ModInfo info;
    private final BusFacts facts;

    /** The mod's own classes by name, in name order, so that everything below comes out in a fixed order. */
    private final Map<String, ClassData> classes = new TreeMap<>();
    /** The small text files the analysis reads: Mixin configs and access transformers, by entry name. */
    private final Map<String, String> texts = new HashMap<>();
    private final Set<String> mixinNames = new HashSet<>();
    private final List<Listener> listeners = new ArrayList<>();
    private final List<Registered> registered = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private int assets, data;

    /** `bus.register(...)` seen in the code: the class registered, as an object or as itself, and on which bus. */
    private record Registered(String className, boolean instance, Bus bus, String busWords, ClassData by, String words) {}

    /** A step that puts an event bus on the stack: where, and which bus as far as that step tells. */
    private record Site(int index, Bus bus, String words) {}

    private record Side(String side, String from) {}

    private record BusOf(Bus bus, String from) {}

    private Analyzer(ModJar jar, NeoForgeFrontend frontend, BusFacts facts) {
        this.jar = jar;
        this.toml = frontend.toml();
        this.info = frontend.info();
        this.facts = facts;
    }

    static Analysis analyze(ModJar jar, NeoForgeFrontend frontend, BusFacts facts) throws IOException {
        Analyzer a = new Analyzer(jar, frontend, facts);
        a.readJar();
        List<MixinClass> mixins = a.mixins();
        List<EntryPoint> entryPoints = a.entryPoints();
        a.busCalls();
        a.subscribed();
        a.listeners.sort(Comparator.comparing(Listener::handlerClass));
        return new Analysis(new Census(a.classes.size(), a.assets, a.data), entryPoints, a.listeners, a.registries(), a.configs(),
                a.configScreens(), mixins, a.accessTransformers(), a.gameClasses(), a.apiAreas(), facts.hasJars(), a.warnings);
    }

    // ---- the jar ----

    private void readJar() throws IOException {
        Set<String> wanted = new HashSet<>(info.mixinConfigs());
        wanted.addAll(toml.accessTransformers());
        wanted.add(DEFAULT_TRANSFORMER);
        long left = MAX_CLASSES;
        List<String> nested = new ArrayList<>();
        try (ZipFile zip = new ZipFile(jar.path().toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (e.isDirectory()) continue;
                if (name.startsWith("assets/")) assets++;
                else if (name.startsWith("data/")) data++;
                if (name.endsWith(".jar")) nested.add(name);
                if (wanted.contains(name)) texts.put(name, new String(ModJar.read(zip, e, ModJar.MAX_TEXT), StandardCharsets.UTF_8));
                // Classes under META-INF are other Java versions' copies or other jars' leftovers.
                if (!name.endsWith(".class") || name.startsWith("META-INF/") || name.endsWith("module-info.class")) continue;
                try {
                    byte[] bytes = ModJar.read(zip, e, MAX_CLASS);
                    left -= bytes.length;
                    if (left >= 0) {
                        ClassData c = ClassData.parse(bytes);
                        classes.put(c.name, c);
                    }
                } catch (IOException unreadable) {
                    warnings.add(name + " couldn't be read (" + unreadable.getMessage() + "): what it does isn't in this report");
                }
                if (left < 0) throw new IOException(jar.name() + " holds more than 1 GiB of classes: refused");
            }
        }
        // Libraries and other mods a jar carries inside itself (NeoForge's jar-in-jar) are jars of their own.
        if (!nested.isEmpty()) {
            boolean one = nested.size() == 1;
            warnings.add("the jar holds " + nested.size() + (one ? " nested jar (" : " nested jars (")
                    + String.join(", ", nested.subList(0, Math.min(3, nested.size()))) + (nested.size() > 3 ? ", ..." : "") + "): "
                    + (one ? "it isn't opened, so what it does" : "they aren't opened, so what they do") + " isn't in this report");
        }
    }

    // ---- entry points ----

    private List<EntryPoint> entryPoints() {
        List<EntryPoint> out = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (ModsToml.Mod m : toml.mods()) ids.add(m.modId());
        for (ClassData c : classes.values()) {
            ClassData.Annotation mod = c.annotation(MOD);
            if (mod == null) continue;
            String id = mod.text("value");
            if (id == null || !ids.contains(id)) {
                warnings.add("@Mod(\"" + id + "\") on " + ClassData.simple(c.name) + " names no mod of " + ModsToml.PATH);
            }
            List<ClassData.Method> constructors = c.methods.stream()
                    .filter(m -> m.name().equals("<init>") && (m.access() & ClassData.PUBLIC) != 0).toList();
            if (constructors.size() != 1) {
                warnings.add(ClassData.simple(c.name) + " has " + constructors.size() + " public constructors: NeoForge wants exactly one on a @Mod class");
            }
            List<String> parameters = new ArrayList<>(), unknown = new ArrayList<>();
            if (!constructors.isEmpty()) {
                for (String p : ClassData.parameters(constructors.get(0).descriptor())) {
                    String type = ClassData.internal(p);
                    String shown = type == null ? p : ClassData.simple(type);
                    parameters.add(shown);
                    if (type == null || !HANDED.contains(type)) unknown.add(shown);
                }
            }
            String side = oneSide(mod.list("dist"));
            out.add(new EntryPoint(c.name, id == null ? "" : id, side == null ? "both" : side, parameters, unknown));
        }
        return out;
    }

    // ---- listeners: addListener and register ----

    private void busCalls() {
        for (ClassData c : classes.values()) {
            if (mixinNames.contains(c.name)) continue;
            boolean modClass = c.annotation(MOD) != null;
            for (ClassData.Method m : c.methods) {
                List<Op> ops = m.code();
                Map<Integer, String> slots = slots(m);
                boolean modConstructor = modClass && m.name().equals("<init>");
                int window = 0;
                for (int i = 0; i < ops.size(); i++) {
                    Op op = ops.get(i);
                    if (op.kind() != Kind.CALL || !op.owner().equals(EVENT_BUS)) continue;
                    Site site = site(ops, window, i, slots, modConstructor);
                    if (op.name().equals("addListener")) added(c, m, ops, site, window, i);
                    else if (op.name().equals("register") && op.descriptor().equals("(Ljava/lang/Object;)V")) registered(c, m, ops, site, window, i);
                    else continue;
                    window = i + 1;
                }
            }
        }
    }

    /** `bus.addListener(...)` at step `i`: one listener, read from the steps between the bus and the call. */
    private void added(ClassData c, ClassData.Method m, List<Op> ops, Site site, int window, int i) {
        Op call = ops.get(i);
        int from = site.index() >= 0 ? site.index() + 1 : window;
        Op lambda = last(ops, from, i, o -> o.kind() == Kind.LAMBDA && ((ClassData.Lambda) o.value()).functionalInterface().equals(CONSUMER));
        Op type = last(ops, from, i, o -> o.kind() == Kind.TYPE);
        Op priority = last(ops, from, i, o -> o.kind() == Kind.GET_STATIC && o.owner().equals(PRIORITY));
        List<String> parameters = ClassData.parameters(call.descriptor());
        List<String> notes = new ArrayList<>();

        // The event: the class handed over when the call takes one, else the type the handler takes.
        String event = null;
        if (parameters.contains("Ljava/lang/Class;") && type != null) {
            event = type.owner();
        } else if (lambda != null) {
            List<String> taken = ClassData.parameters(((ClassData.Lambda) lambda.value()).instantiatedType());
            String named = taken.size() == 1 ? ClassData.internal(taken.get(0)) : null;
            if (named != null && !named.equals(BASE_EVENT)) event = named;
        }
        if (event == null) notes.add("the event couldn't be read at the call");

        boolean receiveCanceled = false;
        if (parameters.contains("Z")) {
            Op flag = last(ops, from, i, o -> o.kind() == Kind.NUMBER);
            if (flag != null) receiveCanceled = ((Number) flag.value()).intValue() != 0;
            else notes.add("whether it receives cancelled events couldn't be read");
        }

        String handlerClass = c.name, handlerMethod = null;
        ClassData.Method handler = null;
        if (lambda != null) {
            handlerClass = lambda.owner();
            handlerMethod = lambda.name();
            handler = method(classes.get(handlerClass), handlerMethod, lambda.descriptor());
        } else {
            notes.add("the handler isn't a lambda or a method reference written at the call, so it wasn't read");
        }
        Side side = side(classes.get(handlerClass), handler, c, event);
        BusOf bus = bus(event, site.bus(), site.words(), notes);
        listeners.add(new Listener(event, handlerClass, handlerMethod, handlerMethod != null && handlerMethod.startsWith("lambda$"),
                "addListener in " + where(c, m), true, bus.bus(), bus.from(), side.side(), side.from(),
                priority == null ? "NORMAL" : priority.name(), receiveCanceled, notes));
    }

    /** `bus.register(x)` at step `i`: which class is registered, read from the step just before the call. */
    private void registered(ClassData c, ClassData.Method m, List<Op> ops, Site site, int window, int i) {
        int from = site.index() >= 0 ? site.index() + 1 : window;
        Op argument = i - 1 >= from ? ops.get(i - 1) : null;
        String target = null;
        boolean instance = true;
        if (argument != null) {
            if (argument.kind() == Kind.CALL && argument.name().equals("<init>")) {
                target = argument.owner();
            } else if (argument.kind() == Kind.TYPE) {
                target = argument.owner();
                instance = false;
            } else if (argument.kind() == Kind.LOCAL && (Integer) argument.value() == 0 && !m.isStatic()) {
                target = c.name;
            }
        }
        if (target == null) {
            warnings.add(where(c, m) + " registers something on an event bus that couldn't be read: its listeners, if it has any, show as not registered");
            return;
        }
        String shown = instance ? (target.equals(c.name) ? "this" : "new " + ClassData.simple(target) + "(...)") : ClassData.simple(target) + ".class";
        registered.add(new Registered(target, instance, site.bus(), site.words(), c, "register(" + shown + ") in " + where(c, m)));
    }

    /** The last step in [from, to) that puts an event bus on the stack. */
    private Site site(List<Op> ops, int from, int to, Map<Integer, String> slots, boolean modConstructor) {
        for (int i = to - 1; i >= from; i--) {
            Op op = ops.get(i);
            switch (op.kind()) {
                case GET_STATIC -> {
                    if (op.owner().equals(NEOFORGE) && op.name().equals("EVENT_BUS")) return new Site(i, Bus.GAME, "it is added to NeoForge.EVENT_BUS");
                    if (EVENT_BUS_TYPE.equals(op.descriptor())) return new Site(i, Bus.UNKNOWN, null);
                }
                case GET_FIELD -> {
                    if (EVENT_BUS_TYPE.equals(op.descriptor())) return new Site(i, Bus.UNKNOWN, null);
                }
                case LOCAL -> {
                    if (EVENT_BUS_TYPE.equals(slots.get((Integer) op.value()))) {
                        // Only a @Mod class's constructor is sure to be handed the mod's own bus.
                        return modConstructor ? new Site(i, Bus.MOD, "it is added to the bus NeoForge hands the @Mod class's constructor")
                                : new Site(i, Bus.UNKNOWN, null);
                    }
                }
                case CALL -> {
                    if (op.descriptor().endsWith(")" + EVENT_BUS_TYPE)) {
                        boolean container = op.name().equals("getEventBus") && (op.owner().equals(MOD_CONTAINER) || op.owner().equals(FML_MOD_CONTAINER));
                        return container ? new Site(i, Bus.MOD, "it is added to the mod's own bus (ModContainer.getEventBus)") : new Site(i, Bus.UNKNOWN, null);
                    }
                }
                default -> {
                }
            }
        }
        return new Site(-1, Bus.UNKNOWN, null);
    }

    // ---- listeners: @SubscribeEvent ----

    private void subscribed() {
        for (ClassData c : classes.values()) {
            boolean subscriber = c.annotation(SUBSCRIBER) != null || c.annotation(SUBSCRIBER_NESTED) != null;
            for (ClassData.Method m : c.methods) {
                ClassData.Annotation subscribe = m.annotation(SUBSCRIBE);
                if (subscribe == null) continue;
                List<String> notes = new ArrayList<>();
                List<String> taken = ClassData.parameters(m.descriptor());
                String event = taken.size() == 1 ? ClassData.internal(taken.get(0)) : null;
                if (event == null) notes.add("a listener takes one event, and this method doesn't");

                String by;
                boolean isRegistered;
                Bus site = Bus.UNKNOWN;
                String siteWords = null;
                ClassData registrar = null;
                if (subscriber) {
                    by = "@EventBusSubscriber on " + ClassData.simple(c.name);
                    isRegistered = m.isStatic();
                    if (!isRegistered) notes.add("@EventBusSubscriber registers a class's static methods, and this one isn't static");
                } else {
                    Registered fitting = null, other = null;
                    for (Registered r : registered) {
                        if (!r.className().equals(c.name)) continue;
                        if (r.instance() != m.isStatic()) fitting = r;
                        else other = r;
                    }
                    if (fitting != null) {
                        by = fitting.words();
                        isRegistered = true;
                        site = fitting.bus();
                        siteWords = fitting.busWords();
                        registrar = fitting.by();
                    } else if (other != null) {
                        by = other.words();
                        isRegistered = false;
                        notes.add(m.isStatic() ? "the class is registered as an object, which registers its non-static methods only"
                                : "the class itself is registered, which registers its static methods only");
                    } else {
                        by = "nothing found that registers " + ClassData.simple(c.name);
                        isRegistered = false;
                    }
                }
                String priority = subscribe.values().get("priority") instanceof ClassData.EnumConstant p ? p.name() : "NORMAL";
                boolean receiveCanceled = Boolean.TRUE.equals(subscribe.values().get("receiveCanceled"));
                Side side = side(c, m, registrar, event);
                BusOf bus = bus(event, site, siteWords, notes);
                listeners.add(new Listener(event, c.name, m.name(), false, by, isRegistered, bus.bus(), bus.from(), side.side(), side.from(),
                        priority, receiveCanceled, notes));
            }
        }
    }

    // ---- bus and side ----

    /** The bus of a listener: where it is registered when the code shows it, else what is known of its event. */
    private BusOf bus(String event, Bus site, String siteWords, List<String> notes) {
        BusFacts.Answer type = event == null ? new BusFacts.Answer(Bus.UNKNOWN, null) : facts.of(event, classes);
        if (site != Bus.UNKNOWN && type.bus() != Bus.UNKNOWN && site != type.bus()) {
            notes.add("it is added to the " + word(site) + " bus, but by " + type.from() + " its event is posted on the " + word(type.bus()) + " bus");
            return new BusOf(Bus.UNKNOWN, null);
        }
        if (site != Bus.UNKNOWN) return new BusOf(site, siteWords);
        return new BusOf(type.bus(), type.from());
    }

    static String word(Bus bus) {
        return switch (bus) {
            case MOD -> "mod";
            case GAME -> "game";
            case UNKNOWN -> "unknown";
        };
    }

    /** The side a listener runs on, from the first thing that says: its own marks, its class's, who registers it, its event. */
    private Side side(ClassData handlerClass, ClassData.Method handler, ClassData registrar, String event) {
        String s;
        if (handler != null && (s = onlyIn(handler.annotations())) != null) return new Side(s, "@OnlyIn on the method");
        if (handlerClass != null) {
            if ((s = onlyIn(handlerClass.annotations)) != null) return new Side(s, "@OnlyIn on its class");
            ClassData.Annotation subscriber = handlerClass.annotation(SUBSCRIBER) != null ? handlerClass.annotation(SUBSCRIBER) : handlerClass.annotation(SUBSCRIBER_NESTED);
            if (subscriber != null && (s = oneSide(subscriber.list("value"))) != null) return new Side(s, "its class's @EventBusSubscriber names that side");
            if ((s = modSide(handlerClass)) != null) return new Side(s, "its class is a @Mod class for that side only");
        }
        if (registrar != null && registrar != handlerClass) {
            if ((s = modSide(registrar)) != null) return new Side(s, "it is registered by a @Mod class for that side only");
            if ((s = onlyIn(registrar.annotations)) != null) return new Side(s, "it is registered by an @OnlyIn class");
        }
        if (event != null && event.contains("/client/")) return new Side("client", "its event is a client event");
        return new Side("common", null);
    }

    private static String modSide(ClassData c) {
        ClassData.Annotation mod = c.annotation(MOD);
        return mod == null ? null : oneSide(mod.list("dist"));
    }

    private static String onlyIn(List<ClassData.Annotation> annotations) {
        for (ClassData.Annotation a : annotations) if (a.type().equals(ONLY_IN)) return oneSide(a.list("value"));
        return null;
    }

    /** The one side a list of `Dist` values names; null when it names none, both, or something else. */
    private static String oneSide(List<Object> dists) {
        if (dists.size() != 1 || !(dists.get(0) instanceof ClassData.EnumConstant dist)) return null;
        return switch (dist.name()) {
            case "CLIENT" -> "client";
            case "DEDICATED_SERVER" -> "server";
            default -> null;
        };
    }

    // ---- registries ----

    /** A DeferredRegister being read: its entries are added as their calls are found. */
    private static final class Draft {
        final String kind, namespace, declaredIn, field;
        boolean onModBus;
        final List<Registration> entries = new ArrayList<>();

        Draft(String kind, String namespace, String declaredIn, String field) {
            this.kind = kind;
            this.namespace = namespace;
            this.declaredIn = declaredIn;
            this.field = field;
        }
    }

    private List<Registry> registries() {
        // First the registers themselves: `FIELD = DeferredRegister.create...(...)`.
        Map<String, Draft> drafts = new LinkedHashMap<>();
        for (ClassData c : classes.values()) {
            for (ClassData.Method m : c.methods) {
                List<Op> ops = m.code();
                int window = 0;
                for (int i = 0; i < ops.size(); i++) {
                    Op op = ops.get(i);
                    if (op.kind() != Kind.CALL || !op.owner().equals(DEFERRED_REGISTER) || !op.name().startsWith("create") || !"static".equals(op.value())) continue;
                    Op namespace = last(ops, window, i, o -> o.kind() == Kind.STRING);
                    String kind = NAMED_REGISTERS.get(op.name());
                    if (kind == null && op.name().equals("create")) {
                        Op key = last(ops, window, i, o -> o.kind() == Kind.GET_STATIC && (o.owner().endsWith("Registries") || o.owner().endsWith("Registries$Keys")));
                        if (key != null) kind = key.name();
                    }
                    Op store = i + 1 < ops.size() ? ops.get(i + 1) : null;
                    if (store != null && store.kind() == Kind.PUT_STATIC) {
                        drafts.put(store.owner() + "#" + store.name(),
                                new Draft(kind, namespace == null ? null : (String) namespace.value(), store.owner(), store.name()));
                    } else {
                        warnings.add(where(c, m) + " makes a DeferredRegister that isn't kept in a static field: what is registered with it wasn't read");
                    }
                    window = i + 1;
                }
            }
        }
        // Then what is registered with each.
        for (ClassData c : classes.values()) {
            boolean unread = false;
            for (ClassData.Method m : c.methods) {
                List<Op> ops = m.code();
                int window = 0;
                for (int i = 0; i < ops.size(); i++) {
                    Op op = ops.get(i);
                    if (op.kind() != Kind.CALL || !op.owner().startsWith(DEFERRED_REGISTER) || !op.name().startsWith("register") || "static".equals(op.value())) continue;
                    int at = lastIndex(ops, window, i, o -> o.kind() == Kind.GET_STATIC && drafts.containsKey(o.owner() + "#" + o.name()));
                    window = i + 1;
                    if (at < 0) {
                        unread = true;
                        continue;
                    }
                    Draft draft = drafts.get(ops.get(at).owner() + "#" + ops.get(at).name());
                    if (op.descriptor().equals("(" + EVENT_BUS_TYPE + ")V")) {
                        draft.onModBus = true;
                        continue;
                    }
                    Op name = first(ops, at + 1, i, o -> o.kind() == Kind.STRING);
                    Op lambda = last(ops, at + 1, i, o -> o.kind() == Kind.LAMBDA);
                    Op store = i + 1 < ops.size() && ops.get(i + 1).kind() == Kind.PUT_STATIC ? ops.get(i + 1) : null;
                    String[] object = objectClass(c, op, lambda, store);
                    draft.entries.add(new Registration(name == null ? null : (String) name.value(), op.name(), object[0], object[1],
                            store == null ? null : ClassData.simple(store.owner()) + "." + store.name()));
                }
            }
            if (unread) warnings.add(ClassData.simple(c.name) + " registers things with a DeferredRegister that couldn't be told: they aren't in this report");
        }
        List<Registry> out = new ArrayList<>();
        for (Draft d : drafts.values()) out.add(new Registry(d.kind, d.namespace, d.declaredIn, d.field, d.onModBus, d.entries));
        return out;
    }

    /** The class of what a registration makes, and how it was told: {class or null, in words or null}. */
    private String[] objectClass(ClassData c, Op call, Op lambda, Op store) {
        if (lambda != null) {
            if (((ClassData.Lambda) lambda.value()).constructor()) return new String[] {lambda.owner(), "the constructor named at the call"};
            ClassData.Method body = method(classes.get(lambda.owner()), lambda.name(), lambda.descriptor());
            if (body != null) {
                Set<String> made = new LinkedHashSet<>();
                for (Op o : body.code()) if (o.kind() == Kind.NEW) made.add(o.owner());
                if (made.size() == 1) return new String[] {made.iterator().next(), "what the lambda at the call makes"};
            }
        }
        String simple = SIMPLE_REGISTRATIONS.get(call.name());
        if (simple != null) return new String[] {simple, "what " + call.name() + " always makes"};
        if (store != null) {
            ClassData holder = classes.get(store.owner());
            if (holder != null) {
                for (ClassData.Field f : holder.fields) {
                    String declared = f.name().equals(store.name()) ? lastTypeArgument(f.signature()) : null;
                    if (declared != null) return new String[] {declared, "the type its holder field declares, which may be a supertype"};
                }
            }
        }
        return new String[] {null, null};
    }

    /** The last type argument of a field's generic type (`...DeferredBlock<Lcom/x/MyBlock;>;` gives `com/x/MyBlock`), or null. */
    static String lastTypeArgument(String signature) {
        if (signature == null) return null;
        int close = signature.lastIndexOf('>');
        if (close < 1 || signature.charAt(close - 1) != ';') return null;
        int start = close - 2;
        while (start >= 0 && ";<".indexOf(signature.charAt(start)) < 0) start--;
        String argument = signature.substring(start + 1, close - 1);
        if (argument.startsWith("+") || argument.startsWith("-")) argument = argument.substring(1);
        return argument.startsWith("L") ? argument.substring(1) : null;
    }

    // ---- configs ----

    private List<ConfigUse> configs() {
        List<ConfigUse> out = new ArrayList<>();
        for (ClassData c : classes.values()) {
            for (ClassData.Method m : c.methods) {
                List<Op> ops = m.code();
                int window = 0;
                for (int i = 0; i < ops.size(); i++) {
                    Op op = ops.get(i);
                    if (op.kind() != Kind.CALL || !op.name().equals("registerConfig") || !op.owner().startsWith("net/neoforged/fml/")) continue;
                    Op type = last(ops, window, i, o -> o.kind() == Kind.GET_STATIC && o.owner().equals(CONFIG_TYPE));
                    Op spec = last(ops, window, i, o -> o.kind() == Kind.GET_STATIC && o.descriptor().endsWith("ConfigSpec;"));
                    out.add(new ConfigUse(type == null ? null : type.name(),
                            spec == null ? null : ClassData.simple(spec.owner()) + "." + spec.name(), where(c, m)));
                    window = i + 1;
                }
            }
        }
        return out;
    }

    /** The classes that name NeoForge's config screen factory: they make one, or hand one to NeoForge. */
    private List<String> configScreens() {
        List<String> out = new ArrayList<>();
        for (ClassData c : classes.values()) if (c.classRefs.contains(CONFIG_SCREEN)) out.add(c.name);
        return out;
    }

    // ---- Mixin classes ----

    private List<MixinClass> mixins() {
        Map<String, MixinClass> out = new LinkedHashMap<>();
        for (String config : jar.manifestMixins()) {
            if (info.mixinConfigs().contains(config)) continue;
            warnings.add("the jar's manifest names the Mixin config " + config + " (`MixinConfigs`), which " + ModsToml.PATH
                    + " doesn't list: whether NeoForge applies it isn't known here, so the classes it lists are taken as not applied");
        }
        for (String config : info.mixinConfigs()) {
            String text = texts.get(config);
            if (text == null) {
                warnings.add("the Mixin config " + config + " is listed in " + ModsToml.PATH + " but isn't in the jar");
                continue;
            }
            Map<String, Object> json;
            try {
                json = Json.object(Json.parse(text));
            } catch (Json.Invalid e) {
                warnings.add("the Mixin config " + config + " isn't JSON this tool can read (" + e.getMessage() + ")");
                continue;
            }
            String pack = Json.text(json, "package");
            String prefix = pack == null || pack.isEmpty() ? "" : pack.replace('.', '/') + "/";
            for (String environment : List.of("mixins", "client", "server")) {
                for (Object listed : Json.array(json.get(environment))) {
                    if (!(listed instanceof String name)) continue;
                    String className = prefix + name.replace('.', '/');
                    ClassData c = classes.get(className);
                    if (c == null) warnings.add(config + " lists the Mixin class " + className.replace('/', '.') + ", which isn't in the jar");
                    else out.putIfAbsent(className, mixinClass(c, config, environment));
                }
            }
        }
        for (ClassData c : classes.values()) {
            if (c.annotation(MIXIN) != null && !out.containsKey(c.name)) out.put(c.name, mixinClass(c, null, null));
        }
        mixinNames.addAll(out.keySet());
        return List.copyOf(out.values());
    }

    private static MixinClass mixinClass(ClassData c, String config, String environment) {
        List<String> targets = new ArrayList<>();
        ClassData.Annotation mixin = c.annotation(MIXIN);
        if (mixin != null) {
            for (Object v : mixin.list("value")) {
                if (v instanceof ClassData.TypeName type) targets.add(ClassData.internal(type.descriptor()) != null ? ClassData.internal(type.descriptor()) : type.descriptor());
            }
            for (Object v : mixin.list("targets")) if (v instanceof String name) targets.add(name.replace('.', '/'));
        }
        List<Injector> injectors = new ArrayList<>();
        for (ClassData.Method m : c.methods) {
            for (ClassData.Annotation a : m.annotations()) {
                if (!a.type().startsWith("Lorg/spongepowered/asm/mixin/") && !a.type().startsWith("Lcom/llamalad7/mixinextras/")) continue;
                String kind = a.type().substring(a.type().lastIndexOf('/') + 1, a.type().length() - 1);
                if (MIXIN_MARKS.contains(kind)) continue;
                List<String> methods = new ArrayList<>();
                for (Object v : a.list("method")) if (v instanceof String name) methods.add(name);
                // @Accessor and @Invoker name their target as `value`, when they name it at all.
                if (methods.isEmpty() && a.text("value") != null) methods.add(a.text("value"));
                String at = null, atTarget = null;
                if (!a.list("at").isEmpty() && a.list("at").get(0) instanceof ClassData.Annotation first) {
                    at = first.text("value");
                    atTarget = first.text("target");
                    if (atTarget != null && atTarget.isEmpty()) atTarget = null;
                }
                injectors.add(new Injector(kind, m.name(), methods, at, atTarget, Boolean.TRUE.equals(a.values().get("cancellable"))));
            }
        }
        return new MixinClass(c.name, config, environment, targets, injectors);
    }

    // ---- access transformers ----

    private List<AccessFile> accessTransformers() {
        List<AccessFile> out = new ArrayList<>();
        Set<String> declared = new LinkedHashSet<>(toml.accessTransformers());
        for (String file : declared) {
            if (texts.containsKey(file)) out.add(new AccessFile(file, true, entries(texts.get(file))));
            else warnings.add("the access transformer " + file + " is listed in " + ModsToml.PATH + " but isn't in the jar");
        }
        if (!declared.contains(DEFAULT_TRANSFORMER) && texts.containsKey(DEFAULT_TRANSFORMER)) {
            out.add(new AccessFile(DEFAULT_TRANSFORMER, false, entries(texts.get(DEFAULT_TRANSFORMER))));
        }
        return out;
    }

    /** An access transformer's entries: one a line, `#` starting a comment. */
    private static List<String> entries(String text) {
        List<String> out = new ArrayList<>();
        for (String line : text.split("\\R")) {
            int comment = line.indexOf('#');
            String entry = (comment < 0 ? line : line.substring(0, comment)).strip();
            if (!entry.isEmpty()) out.add(entry);
        }
        return out;
    }

    // ---- the rest of the mod's code ----

    private List<GameClass> gameClasses() {
        List<GameClass> out = new ArrayList<>();
        for (ClassData c : classes.values()) {
            if (mixinNames.contains(c.name)) continue;
            String up = c.superName;
            Set<String> seen = new HashSet<>();
            while (up != null && classes.containsKey(up) && seen.add(up)) up = classes.get(up).superName;
            if (up == null || !(up.startsWith("net/minecraft/") || up.startsWith("com/mojang/"))) continue;
            Set<String> methods = new LinkedHashSet<>();
            for (ClassData.Method m : c.methods) {
                if (!m.name().startsWith("<") && !m.name().startsWith("lambda$") && (m.access() & ClassData.SYNTHETIC) == 0) methods.add(m.name());
            }
            out.add(new GameClass(c.name, up, List.copyOf(methods)));
        }
        return out;
    }

    private List<ApiArea> apiAreas() {
        Map<String, List<String>> usedBy = new LinkedHashMap<>();
        for (String[] area : AREAS) {
            for (ClassData c : classes.values()) {
                if (c.classRefs.stream().anyMatch(ref -> ref.startsWith(area[0]))) {
                    List<String> users = usedBy.computeIfAbsent(area[1], k -> new ArrayList<>());
                    if (!users.contains(c.name)) users.add(c.name);
                }
            }
        }
        List<ApiArea> out = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : usedBy.entrySet()) out.add(new ApiArea(e.getKey(), e.getValue()));
        return out;
    }

    // ---- small things ----

    /** A method's parameters by the local slot each is in. */
    private static Map<Integer, String> slots(ClassData.Method m) {
        Map<Integer, String> slots = new HashMap<>();
        int slot = m.isStatic() ? 0 : 1;
        for (String p : ClassData.parameters(m.descriptor())) {
            slots.put(slot, p);
            slot += p.equals("J") || p.equals("D") ? 2 : 1;
        }
        return slots;
    }

    private static ClassData.Method method(ClassData c, String name, String descriptor) {
        if (c == null) return null;
        for (ClassData.Method m : c.methods) if (m.name().equals(name) && m.descriptor().equals(descriptor)) return m;
        return null;
    }

    /** Where a step is, in words: "PocketCompass's constructor", "ModRegistry's static initializer", "Net.register". */
    private static String where(ClassData c, ClassData.Method m) {
        String simple = ClassData.simple(c.name);
        if (m.name().equals("<init>")) return simple + "'s constructor";
        if (m.name().equals("<clinit>")) return simple + "'s static initializer";
        return simple + "." + m.name();
    }

    private static Op last(List<Op> ops, int from, int to, Predicate<Op> wanted) {
        int i = lastIndex(ops, from, to, wanted);
        return i < 0 ? null : ops.get(i);
    }

    private static int lastIndex(List<Op> ops, int from, int to, Predicate<Op> wanted) {
        for (int i = to - 1; i >= from; i--) if (wanted.test(ops.get(i))) return i;
        return -1;
    }

    private static Op first(List<Op> ops, int from, int to, Predicate<Op> wanted) {
        for (int i = from; i < to; i++) if (wanted.test(ops.get(i))) return ops.get(i);
        return null;
    }
}
