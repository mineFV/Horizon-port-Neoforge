package horizonneo;

import horizonneo.Analysis.Listener;
import horizonneo.BusFacts.Bus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The analysis, on the two made-up mods' jars (the program's second and fourth arguments) and on the
 * stand-ins' jar passed off as NeoForge's own (the third): what it reads, and what it owns up to not
 * reading.
 */
final class AnalyzerTest {
    private AnalyzerTest() {}

    private static final String P = "com/example/pocketcompass/";
    private static final String CLIENT_EVENTS = "net/neoforged/neoforge/client/event/";

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("analysis");
        Path stubs = Path.of(args[2]);

        // ---- Pocket Compass, with nothing known of NeoForge but its documentation ----
        ModJar jar = ModJar.open(Path.of(args[1]));
        NeoForgeFrontend frontend = NeoForgeFrontend.read(jar);
        Analysis a = Analyzer.analyze(jar, frontend, BusFacts.documented());

        c.same("what the jar holds", new Analysis.Census(13, 1, 1), a.census());
        c.same("its @Mod classes", List.of(
                new Analysis.EntryPoint(P + "PocketCompass", "pocketcompass", "both", List.of("IEventBus", "ModContainer", "Dist"), List.of()),
                new Analysis.EntryPoint(P + "PocketCompassClient", "pocketcompass", "client", List.of("IEventBus"), List.of())), a.entryPoints());
        c.same("its listeners: handler, event, bus and side", List.of(
                "ClientEvents.onClientTick <- ClientTickEvent.Post [unknown, client]",
                "ClientEvents.onHud <- RenderGuiEvent.Post [unknown, client]",
                "ClientEvents.onClientSetup <- FMLClientSetupEvent [mod, client]",
                "ClientEvents.onChat <- ClientChatReceivedEvent [unknown, client]",
                "ClientEvents.notStatic <- ClientTickEvent.Pre [unknown, client, not registered]",
                "Net.register <- RegisterPayloadHandlersEvent [mod, common]",
                "Orphan.onTick <- ServerTickEvent.Post [unknown, common, not registered]",
                "PocketCompass.onCommonSetup <- FMLCommonSetupEvent [mod, common]",
                "PocketCompass.onBreak <- BlockEvent.BreakEvent [game, common]",
                "PocketCompassClient.onRegisterKeys <- RegisterKeyMappingsEvent [mod, client]",
                "PocketCompassClient.onKey <- InputEvent.Key [game, client]",
                "PocketCompassClient.<lambda> <- ClientChatReceivedEvent [game, client]",
                "ServerEvents.onServerTick <- ServerTickEvent.Post [game, common]",
                "ServerEvents.onLogin <- PlayerEvent.PlayerLoggedInEvent [game, common]"), shown(a));

        Listener chat = listener(a, "onChat");
        c.same("@SubscribeEvent's priority and receiveCanceled", List.of("LOW", true), List.of(chat.priority(), chat.receiveCanceled()));
        c.same("a listener of an @EventBusSubscriber class", "@EventBusSubscriber on ClientEvents", chat.registeredBy());
        c.same("... on the client, since the annotation says so", "its class's @EventBusSubscriber names that side", chat.sideFrom());
        Listener key = listener(a, "onKey");
        c.same("addListener's priority and receiveCanceled", List.of("HIGH", true), List.of(key.priority(), key.receiveCanceled()));
        c.same("its event is the class handed to addListener", CLIENT_EVENTS + "InputEvent$Key", key.event());
        c.same("the game bus, by where it is added", "it is added to NeoForge.EVENT_BUS", key.busFrom());
        c.same("the client, by its @Mod class", "its class is a @Mod class for that side only", key.sideFrom());
        c.same("the mod bus, by the constructor's bus", "it is added to the bus NeoForge hands the @Mod class's constructor", listener(a, "onCommonSetup").busFrom());
        c.same("the mod bus, by the documentation", BusFacts.FROM_DOCS, listener(a, "onClientSetup").busFrom());
        c.same("a default priority", "NORMAL", listener(a, "onBreak").priority());
        c.same("a marked method that addListener is given is one listener", 1L, a.listeners().stream().filter(l -> "onBreak".equals(l.handlerMethod())).count());
        c.same("and it says the mark registers nothing", List.of("it is also marked @SubscribeEvent, which registers nothing here: nothing registers PocketCompass itself"),
                listener(a, "onBreak").notes());
        c.same("it is still registered by addListener", "addListener in PocketCompass's constructor", listener(a, "onBreak").registeredBy());
        c.same("an object's listeners", "register(new ServerEvents(...)) in PocketCompass's constructor", listener(a, "onServerTick").registeredBy());
        Listener notStatic = listener(a, "notStatic");
        c.yes("a method that isn't static in an @EventBusSubscriber class isn't registered",
                !notStatic.registered() && notStatic.notes().equals(List.of("@EventBusSubscriber registers a class's static methods, and this one isn't static")));
        Listener orphan = listener(a, "onTick");
        c.same("a listener nothing registers", List.of(false, "nothing found that registers Orphan"), List.of(orphan.registered(), orphan.registeredBy()));
        Listener written = a.listeners().stream().filter(Listener::lambda).findFirst().orElseThrow();
        c.same("a lambda written at the call", List.of(P + "PocketCompassClient", CLIENT_EVENTS + "ClientChatReceivedEvent"), List.of(written.handlerClass(), written.event()));
        c.same("no listener has anything to note but the one that isn't static and the one marked twice", List.of("onBreak", "notStatic"),
                a.listeners().stream().filter(l -> !l.notes().isEmpty()).map(Listener::handlerMethod).sorted(java.util.Comparator.reverseOrder()).toList());

        c.same("its registers", List.of(
                "BLOCK pocketcompass ModRegistry.BLOCKS, on the mod bus",
                "ITEM pocketcompass ModRegistry.ITEMS, on the mod bus",
                "SOUND_EVENT pocketcompass ModRegistry.SOUNDS, on the mod bus"),
                a.registries().stream().map(r -> r.kind() + " " + r.namespace() + " " + ClassData.simple(r.declaredIn()) + "." + r.field()
                        + (r.onModBus() ? ", on the mod bus" : "")).toList());
        List<String> registrations = new ArrayList<>(), told = new ArrayList<>();
        for (Analysis.Registry r : a.registries()) {
            for (Analysis.Registration e : r.entries()) {
                registrations.add(e.name() + " " + e.how() + " " + ClassData.simple(e.objectClass()) + " -> " + e.holderField());
                told.add(e.classFrom());
            }
        }
        c.same("what it registers", List.of(
                "compass_block registerSimpleBlock Block -> ModRegistry.COMPASS_BLOCK",
                "spinning_block register SpinningBlock -> ModRegistry.SPINNING_BLOCK",
                "plain_block registerBlock Block -> ModRegistry.PLAIN_BLOCK",
                "pocket_compass registerSimpleItem Item -> ModRegistry.POCKET_COMPASS",
                "compass_block registerSimpleBlockItem BlockItem -> ModRegistry.COMPASS_BLOCK_ITEM",
                "ding register SoundEvent -> ModRegistry.DING"), registrations);
        c.same("how each registered class was told", List.of(
                "what registerSimpleBlock always makes", "what the lambda at the call makes", "the constructor named at the call",
                "what registerSimpleItem always makes", "what registerSimpleBlockItem always makes",
                "the type its holder field declares, which may be a supertype"), told);

        c.same("its config", List.of(new Analysis.ConfigUse("CLIENT", "Config.SPEC", "PocketCompass's constructor")), a.configs());
        c.same("its Mixin classes", List.of(
                new Analysis.MixinClass(P + "mixin/MinecraftMixin", "pocketcompass.mixins.json", "client", List.of("net/minecraft/client/Minecraft"), List.of(
                        new Analysis.Injector("Inject", "pocketcompass$onTick", List.of("tick"), "HEAD", null, true),
                        new Analysis.Injector("Redirect", "pocketcompass$noScreen", List.of("tick"), "INVOKE",
                                "Lnet/minecraft/client/Minecraft;setScreen(Ljava/lang/Object;)V", false))),
                new Analysis.MixinClass(P + "mixin/UnlistedMixin", null, null, List.of("net/minecraft/client/gui/Gui"), List.of())), a.mixins());
        c.same("its access transformer", List.of(new Analysis.AccessFile("META-INF/accesstransformer.cfg", true,
                List.of("public net.minecraft.client.Minecraft someField", "public-f net.minecraft.client.Minecraft otherField"))), a.accessTransformers());
        c.same("its classes that extend the game's", List.of(
                new Analysis.GameClass(P + "CompassScreen", "net/minecraft/client/gui/screens/Screen", List.of("init")),
                new Analysis.GameClass(P + "SpinningBlock", "net/minecraft/world/level/block/Block", List.of("stepOn"))), a.gameClasses());
        c.same("the other parts of NeoForge's API it uses", List.of(new Analysis.ApiArea("custom network payloads", List.of(P + "Net", P + "PocketCompass"))), a.apiAreas());
        c.same("nothing to warn about", List.of(), a.warnings());
        c.yes("NeoForge's jars weren't given", !a.neoforgeJars());

        // ---- the same, with NeoForge's own classes to read each event's bus from ----
        BusFacts withJars = BusFacts.fromJars(List.of(stubs));
        Analysis b = Analyzer.analyze(jar, frontend, withJars);
        c.same("with NeoForge's jars, every registered listener's bus is known", List.of(
                "ClientEvents.onClientTick <- ClientTickEvent.Post [game, client]",
                "ClientEvents.onHud <- RenderGuiEvent.Post [game, client]",
                "ClientEvents.onClientSetup <- FMLClientSetupEvent [mod, client]",
                "ClientEvents.onChat <- ClientChatReceivedEvent [game, client]",
                "ClientEvents.notStatic <- ClientTickEvent.Pre [game, client, not registered]",
                "Net.register <- RegisterPayloadHandlersEvent [mod, common]",
                "Orphan.onTick <- ServerTickEvent.Post [game, common, not registered]",
                "PocketCompass.onCommonSetup <- FMLCommonSetupEvent [mod, common]",
                "PocketCompass.onBreak <- BlockEvent.BreakEvent [game, common]",
                "PocketCompassClient.onRegisterKeys <- RegisterKeyMappingsEvent [mod, client]",
                "PocketCompassClient.onKey <- InputEvent.Key [game, client]",
                "PocketCompassClient.<lambda> <- ClientChatReceivedEvent [game, client]",
                "ServerEvents.onServerTick <- ServerTickEvent.Post [game, common]",
                "ServerEvents.onLogin <- PlayerEvent.PlayerLoggedInEvent [game, common]"), shown(b));
        c.same("... read from the event class's ancestry", BusFacts.FROM_ANCESTRY, listener(b, "onClientTick").busFrom());
        c.yes("... and the analysis says the jars were given", b.neoforgeJars());

        // ---- which bus an event goes to ----
        BusFacts documented = BusFacts.documented();
        c.same("an FML lifecycle event: the mod bus, by the documentation", new BusFacts.Answer(Bus.MOD, BusFacts.FROM_DOCS),
                documented.of("net/neoforged/fml/event/lifecycle/FMLLoadCompleteEvent", Map.of()));
        c.same("an event the documentation puts on the mod bus", Bus.MOD, documented.of("net/neoforged/neoforge/registries/RegisterEvent", Map.of()).bus());
        c.same("one it puts on the game bus", Bus.GAME, documented.of("net/neoforged/neoforge/event/entity/living/LivingEvent$LivingJumpEvent", Map.of()).bus());
        c.same("a name that looks like a mod bus event isn't an answer", new BusFacts.Answer(Bus.UNKNOWN, null),
                documented.of(CLIENT_EVENTS + "RegisterKeyMappingsEvent", Map.of()));
        c.same("nor is another library's class of the same name", Bus.UNKNOWN, documented.of("com/other/RegisterEvent", Map.of()).bus());
        c.same("with the jars: an event that implements the marker", new BusFacts.Answer(Bus.MOD, BusFacts.FROM_ANCESTRY),
                withJars.of(CLIENT_EVENTS + "RegisterKeyMappingsEvent", Map.of()));
        c.same("with the jars: an event that doesn't, through its superclass", Bus.GAME, withJars.of(CLIENT_EVENTS + "ClientTickEvent$Post", Map.of()).bus());
        c.same("with the jars: a class they don't hold", Bus.UNKNOWN, withJars.of("net/neoforged/neoforge/event/NotThere", Map.of()).bus());
        c.yes("whether jars were given", withJars.hasJars() && !documented.hasJars());
        c.refuses("a NeoForge jar that isn't there", "--neoforge-jar: ", () -> BusFacts.fromJars(List.of(Path.of("no-such.jar"))));
        Path text = Files.createTempFile("not-a-jar-", ".jar");
        try {
            Files.writeString(text, "not a zip");
            c.refuses("a NeoForge jar that isn't a jar", "isn't a jar", () -> BusFacts.fromJars(List.of(text)));
        } finally {
            Files.delete(text);
        }

        // ---- the second mod: what can't be read is said, not guessed ----
        ModJar oddJar = ModJar.open(Path.of(args[3]));
        NeoForgeFrontend oddFrontend = NeoForgeFrontend.read(oddJar);
        Analysis odd = Analyzer.analyze(oddJar, oddFrontend, documented);
        c.same("an entry point NeoForge would refuse", List.of(new Analysis.EntryPoint("com/example/oddities/Oddities", "somethingelse", "both",
                List.of("IEventBus", "String"), List.of("String"))), odd.entryPoints());
        c.same("its listeners", List.of(
                "HandedHandlers.onHanded <- ServerTickEvent.Post [game, common]",
                "KeptHandlers.onKept <- ServerTickEvent.Post [game, common]",
                "Oddities.wrongBus <- FMLCommonSetupEvent [unknown, common]",
                "Oddities.? <- ? [game, common]",
                "Oddities.late <- ServerTickEvent.Post [unknown, common]",
                "Oddities.onLogin <- PlayerEvent.PlayerLoggedInEvent [game, common]",
                "Oddities.misplaced <- ServerTickEvent.Post [unknown, common, not registered]",
                "OwnEventHandlers.onOwnSetup <- OwnEvents.Setup [mod, common]",
                "OwnEventHandlers.onOwnLate <- OwnEvents.Late [unknown, common]",
                "StaticHandlers.onTick <- ServerTickEvent.Post [game, common]",
                "StaticHandlers.onClientOnly <- ServerTickEvent.Post [game, client]"), shown(odd));
        c.same("a mod bus event added to the game bus: said, and its bus left unknown",
                List.of("it is added to the game bus, but by NeoForge's documentation its event is posted on the mod bus"), listener(odd, "wrongBus").notes());
        Listener kept = odd.listeners().stream().filter(l -> l.handlerMethod() == null).findFirst().orElseThrow();
        c.same("a handler kept in a variable: said, not guessed", List.of("the event couldn't be read at the call",
                "the handler isn't a lambda or a method reference written at the call, so it wasn't read"), kept.notes());
        c.same("a bus handed on to another method: its bus isn't told there", null, listener(odd, "late").busFrom());
        c.same("an object's own listener", "register(this) in Oddities's constructor", listener(odd, "onLogin").registeredBy());
        c.same("a static method of a class registered as an object", List.of("the class is registered as an object, which registers its non-static methods only"),
                listener(odd, "misplaced").notes());
        c.same("a class's static listeners", "register(StaticHandlers.class) in Oddities's constructor", listener(odd, "onTick").registeredBy());
        c.same("an object kept in a field", "register(Oddities.KEPT, declared as a KeptHandlers) in Oddities's constructor", listener(odd, "onKept").registeredBy());
        c.same("an object a method hands out", "register(HandedHandlers.get(), declared to return a HandedHandlers) in Oddities's constructor", listener(odd, "onHanded").registeredBy());
        c.same("@OnlyIn on a method", "@OnlyIn on the method", listener(odd, "onClientOnly").sideFrom());
        c.same("the mod's own mod bus event, by its ancestry", BusFacts.FROM_ANCESTRY, listener(odd, "onOwnSetup").busFrom());
        c.same("no Mixin class when the config's class is missing", List.of(), odd.mixins());
        c.same("an access transformer the metadata doesn't list", List.of(new Analysis.AccessFile("META-INF/accesstransformer.cfg", false,
                List.of("public net.minecraft.client.Minecraft someField"))), odd.accessTransformers());
        c.same("what it warns about", List.of(
                "the jar holds 1 nested jar (META-INF/jarjar/library-1.0.jar): it isn't opened, so what it does isn't in this report",
                "the Mixin config missing.mixins.json is listed in META-INF/neoforge.mods.toml but isn't in the jar",
                "oddities.mixins.json lists the Mixin class com.example.oddities.mixin.GoneMixin, which isn't in the jar",
                "@Mod(\"somethingelse\") on Oddities names no mod of META-INF/neoforge.mods.toml",
                "Oddities has 2 public constructors: NeoForge wants exactly one on a @Mod class",
                "the access transformer META-INF/missing_at.cfg is listed in META-INF/neoforge.mods.toml but isn't in the jar"), odd.warnings());
        Analysis oddWithJars = Analyzer.analyze(oddJar, oddFrontend, withJars);
        c.same("with NeoForge's jars: the mod's own event that extends one of NeoForge's", "game", Analyzer.word(listener(oddWithJars, "onOwnLate").bus()));
        c.same("... the handed-on bus's listener, by its event", "game", Analyzer.word(listener(oddWithJars, "late").bus()));
        c.same("... and the wrong bus is still a contradiction", "unknown", Analyzer.word(listener(oddWithJars, "wrongBus").bus()));

        // ---- a holder field's declared type ----
        c.same("one type argument", "com/x/MyBlock", Analyzer.lastTypeArgument("Lnet/neoforged/neoforge/registries/DeferredBlock<Lcom/x/MyBlock;>;"));
        c.same("the last of two", "com/x/MySound", Analyzer.lastTypeArgument("Lfoo/Holder<Lnet/minecraft/sounds/SoundEvent;Lcom/x/MySound;>;"));
        c.same("a wildcard's bound", "com/x/Y", Analyzer.lastTypeArgument("Lfoo/Holder<+Lcom/x/Y;>;"));
        c.same("a type variable", null, Analyzer.lastTypeArgument("Lfoo/Holder<TT;>;"));
        c.same("a generic type", null, Analyzer.lastTypeArgument("Lfoo/Holder<Ljava/util/List<Lcom/x/Y;>;>;"));
        c.same("no type argument", null, Analyzer.lastTypeArgument("Lfoo/Plain;"));
        c.same("no signature", null, Analyzer.lastTypeArgument(null));

        System.exit(c.done());
    }

    private static List<String> shown(Analysis a) {
        return a.listeners().stream().map(l -> ClassData.simple(l.handlerClass()) + "."
                + (l.handlerMethod() == null ? "?" : l.lambda() ? "<lambda>" : l.handlerMethod()) + " <- " + (l.event() == null ? "?" : ClassData.simple(l.event()))
                + " [" + Analyzer.word(l.bus()) + ", " + l.side() + (l.registered() ? "" : ", not registered") + "]").toList();
    }

    private static Listener listener(Analysis a, String handlerMethod) {
        return a.listeners().stream().filter(l -> handlerMethod.equals(l.handlerMethod())).findFirst().orElseThrow();
    }
}
