package horizonneo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * The class reader, on the made-up mod's own classes as javac compiled them (their folder is the
 * program's argument): names, annotations, and the steps of a method's code in order.
 */
final class ClassDataTest {
    private ClassDataTest() {}

    private static final String PACKAGE = "com/example/pocketcompass/";

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("class files");
        Path classes = Path.of(args[0]);

        // The entry point: its annotation and what its constructor does, step by step.
        ClassData mod = read(classes, "PocketCompass");
        c.same("the class's name", PACKAGE + "PocketCompass", mod.name);
        c.same("its superclass", "java/lang/Object", mod.superName);
        ClassData.Annotation modAnnotation = mod.annotation("Lnet/neoforged/fml/common/Mod;");
        c.same("@Mod's value", "pocketcompass", modAnnotation.text("value"));
        c.same("@Mod without a dist", List.of(), modAnnotation.list("dist"));
        c.same("an annotation it doesn't have", null, mod.annotation("Lnet/neoforged/fml/common/EventBusSubscriber;"));
        ClassData.Method constructor = method(mod, "<init>");
        c.same("the constructor's parameters",
                List.of("Lnet/neoforged/bus/api/IEventBus;", "Lnet/neoforged/fml/ModContainer;", "Lnet/neoforged/api/distmarker/Dist;"),
                ClassData.parameters(constructor.descriptor()));
        c.same("the constructor's steps, in order", List.of(
                "local 0", "call Object.<init>",
                "get ModRegistry.BLOCKS", "local 1", "call DeferredRegister.Blocks.register",
                "get ModRegistry.ITEMS", "local 1", "call DeferredRegister.Items.register",
                "get ModRegistry.SOUNDS", "local 1", "call DeferredRegister.register",
                "local 1", "local 0", "lambda PocketCompass.onCommonSetup", "call IEventBus.addListener",
                "local 1", "lambda Net.register", "call IEventBus.addListener",
                "get NeoForge.EVENT_BUS", "local 0", "lambda PocketCompass.onBreak", "call IEventBus.addListener",
                "get NeoForge.EVENT_BUS", "new ServerEvents", "call ServerEvents.<init>", "call IEventBus.register",
                "local 2", "get ModConfig.Type.CLIENT", "get Config.SPEC", "call ModContainer.registerConfig"),
                steps(constructor));
        ClassData.Op setup = constructor.code().stream().filter(op -> op.kind() == ClassData.Kind.LAMBDA).findFirst().orElseThrow();
        ClassData.Lambda how = (ClassData.Lambda) setup.value();
        c.same("a method reference: what it runs", List.of(PACKAGE + "PocketCompass", "onCommonSetup", "(Lnet/neoforged/fml/event/lifecycle/FMLCommonSetupEvent;)V"),
                List.of(setup.owner(), setup.name(), setup.descriptor()));
        c.same("... what it is passed as", "java/util/function/Consumer", how.functionalInterface());
        c.same("... with which event", "(Lnet/neoforged/fml/event/lifecycle/FMLCommonSetupEvent;)V", how.instantiatedType());
        c.same("... carrying `this`", List.of("L" + PACKAGE + "PocketCompass;"), how.captured());
        c.yes("... and it isn't a constructor reference", !how.constructor());
        c.yes("the classes it names", mod.classRefs.contains("net/neoforged/neoforge/common/NeoForge") && mod.classRefs.contains(PACKAGE + "ServerEvents"));
        c.yes("the call to registerConfig is a virtual call", constructor.code().get(constructor.code().size() - 1).value().equals("virtual"));

        // An enum in an annotation, a class constant, a number and a lambda written in place.
        ClassData client = read(classes, "PocketCompassClient");
        c.same("@Mod's dist", List.of(new ClassData.EnumConstant("Lnet/neoforged/api/distmarker/Dist;", "CLIENT")),
                client.annotation("Lnet/neoforged/fml/common/Mod;").list("dist"));
        c.same("its constructor's steps", List.of(
                "local 0", "call Object.<init>",
                "local 1", "lambda PocketCompassClient.onRegisterKeys", "call IEventBus.addListener",
                "get NeoForge.EVENT_BUS", "get EventPriority.HIGH", "number 1", "class InputEvent.Key", "lambda PocketCompassClient.onKey", "call IEventBus.addListener",
                "get NeoForge.EVENT_BUS", "class ClientChatReceivedEvent", "lambda PocketCompassClient.lambda$new", "call IEventBus.addListener"),
                steps(method(client, "<init>")));
        ClassData.Op written = method(client, "<init>").code().stream()
                .filter(op -> op.kind() == ClassData.Kind.LAMBDA && op.name().startsWith("lambda$")).findFirst().orElseThrow();
        c.same("a lambda written in place: its event", "(Lnet/neoforged/neoforge/client/event/ClientChatReceivedEvent;)V",
                ((ClassData.Lambda) written.value()).instantiatedType());

        // Annotations on a class and on its methods.
        ClassData events = read(classes, "ClientEvents");
        ClassData.Annotation subscriber = events.annotation("Lnet/neoforged/fml/common/EventBusSubscriber;");
        c.same("@EventBusSubscriber's modid", "pocketcompass", subscriber.text("modid"));
        c.same("@EventBusSubscriber's value", List.of(new ClassData.EnumConstant("Lnet/neoforged/api/distmarker/Dist;", "CLIENT")), subscriber.list("value"));
        ClassData.Annotation chat = method(events, "onChat").annotation("Lnet/neoforged/bus/api/SubscribeEvent;");
        c.same("@SubscribeEvent's priority", new ClassData.EnumConstant("Lnet/neoforged/bus/api/EventPriority;", "LOW"), chat.values().get("priority"));
        c.same("@SubscribeEvent's receiveCanceled", true, chat.values().get("receiveCanceled"));
        c.yes("@SubscribeEvent with nothing set", method(events, "onClientTick").annotation("Lnet/neoforged/bus/api/SubscribeEvent;").values().isEmpty());
        c.yes("a static method", method(events, "onClientTick").isStatic());
        c.yes("a method that isn't static", !method(events, "notStatic").isStatic());
        c.same("a method without any annotation", List.of(), method(events, "<init>").annotations());

        // Static initializers: text, numbers, fields, a constructor reference; and a field's generic type.
        ClassData registry = read(classes, "ModRegistry");
        c.same("the registry class's static initializer", List.of(
                "\"pocketcompass\"", "call DeferredRegister.createBlocks", "put ModRegistry.BLOCKS",
                "\"pocketcompass\"", "call DeferredRegister.createItems", "put ModRegistry.ITEMS",
                "get Registries.SOUND_EVENT", "\"pocketcompass\"", "call DeferredRegister.create", "put ModRegistry.SOUNDS",
                "get ModRegistry.BLOCKS", "\"compass_block\"", "call BlockBehaviour.Properties.of", "number 1.5", "call BlockBehaviour.Properties.strength",
                "call DeferredRegister.Blocks.registerSimpleBlock", "put ModRegistry.COMPASS_BLOCK",
                "get ModRegistry.BLOCKS", "\"spinning_block\"", "lambda ModRegistry.lambda$static", "call DeferredRegister.Blocks.register", "put ModRegistry.SPINNING_BLOCK",
                "get ModRegistry.BLOCKS", "\"plain_block\"", "lambda Block.<init>", "call BlockBehaviour.Properties.of",
                "call DeferredRegister.Blocks.registerBlock", "put ModRegistry.PLAIN_BLOCK",
                "get ModRegistry.ITEMS", "\"pocket_compass\"", "call DeferredRegister.Items.registerSimpleItem", "put ModRegistry.POCKET_COMPASS",
                "get ModRegistry.ITEMS", "\"compass_block\"", "get ModRegistry.COMPASS_BLOCK", "call DeferredRegister.Items.registerSimpleBlockItem", "put ModRegistry.COMPASS_BLOCK_ITEM",
                "get ModRegistry.SOUNDS", "\"ding\"", "lambda ModRegistry.lambda$static", "call DeferredRegister.register", "put ModRegistry.DING"),
                steps(method(registry, "<clinit>")));
        ClassData.Op blockNew = method(registry, "<clinit>").code().stream()
                .filter(op -> op.kind() == ClassData.Kind.LAMBDA && op.name().equals("<init>")).findFirst().orElseThrow();
        c.yes("`Block::new` is a constructor reference", ((ClassData.Lambda) blockNew.value()).constructor());
        ClassData.Field spinning = registry.fields.stream().filter(f -> f.name().equals("SPINNING_BLOCK")).findFirst().orElseThrow();
        c.same("a field's type", "Lnet/neoforged/neoforge/registries/DeferredBlock;", spinning.descriptor());
        c.same("... and its generic type", "Lnet/neoforged/neoforge/registries/DeferredBlock<L" + PACKAGE + "SpinningBlock;>;", spinning.signature());
        c.yes("a static field", (spinning.access() & ClassData.STATIC) != 0);
        c.same("the config class's static initializer", List.of(
                "new ModConfigSpec.Builder", "call ModConfigSpec.Builder.<init>", "put Config.BUILDER",
                "get Config.BUILDER", "\"Show the compass\"", "call ModConfigSpec.Builder.comment", "\"show\"", "number 1", "call ModConfigSpec.Builder.define", "put Config.SHOW",
                "get Config.BUILDER", "\"size\"", "number 2", "number 1", "number 5", "call ModConfigSpec.Builder.defineInRange", "put Config.SIZE",
                "get Config.BUILDER", "call ModConfigSpec.Builder.build", "put Config.SPEC"),
                steps(method(read(classes, "Config"), "<clinit>")));

        // Mixin annotations: a class value, a nested annotation, an array of them.
        ClassData mixin = read(classes, "mixin/MinecraftMixin");
        c.same("@Mixin's target", List.of(new ClassData.TypeName("Lnet/minecraft/client/Minecraft;")),
                mixin.annotation("Lorg/spongepowered/asm/mixin/Mixin;").list("value"));
        ClassData.Annotation inject = method(mixin, "pocketcompass$onTick").annotation("Lorg/spongepowered/asm/mixin/injection/Inject;");
        c.same("@Inject's method", List.of("tick"), inject.list("method"));
        c.same("@Inject's cancellable", true, inject.values().get("cancellable"));
        c.same("@Inject's @At", "HEAD", ((ClassData.Annotation) inject.list("at").get(0)).text("value"));
        ClassData.Annotation redirectAt = (ClassData.Annotation) method(mixin, "pocketcompass$noScreen")
                .annotation("Lorg/spongepowered/asm/mixin/injection/Redirect;").values().get("at");
        c.same("@Redirect's @At", List.of("INVOKE", "Lnet/minecraft/client/Minecraft;setScreen(Ljava/lang/Object;)V"),
                List.of(redirectAt.text("value"), redirectAt.text("target")));
        c.same("@Mixin with targets as text", List.of("net.minecraft.client.gui.Gui"),
                read(classes, "mixin/UnlistedMixin").annotation("Lorg/spongepowered/asm/mixin/Mixin;").list("targets"));
        c.same("a class's superclass in the game", "net/minecraft/client/gui/screens/Screen", read(classes, "CompassScreen").superName);

        // Code the reader has to step over without losing its place: switches, wide numbers, arrays.
        ClassData.Method shapes = method(read(classes, "Shapes"), "name");
        List<Object> texts = shapes.code().stream().filter(op -> op.kind() == ClassData.Kind.STRING).map(ClassData.Op::value).toList();
        c.same("text constants around three switches", List.of("zero", "one", "two", "many", "a", "b", "after-the-switches"), texts);
        List<Object> numbers = shapes.code().stream().filter(op -> op.kind() == ClassData.Kind.NUMBER).map(ClassData.Op::value).toList();
        c.yes("a long, a double and small numbers", numbers.contains(123456789012L) && numbers.contains(2.5) && numbers.contains(300) && numbers.contains(4));

        // Descriptors.
        c.same("a method's parameters", List.of("I", "Ljava/lang/String;", "[J", "[[Ljava/lang/Object;", "D"),
                ClassData.parameters("(ILjava/lang/String;[J[[Ljava/lang/Object;D)V"));
        c.same("no parameters", List.of(), ClassData.parameters("()Ljava/lang/String;"));
        c.same("what a method returns", "Ljava/lang/String;", ClassData.returns("(I)Ljava/lang/String;"));
        c.same("a class descriptor's name", "foo/Bar", ClassData.internal("Lfoo/Bar;"));
        c.same("a primitive has none", null, ClassData.internal("I"));
        c.same("an array has none", null, ClassData.internal("[Lfoo/Bar;"));
        c.same("a nested class's name as source writes it", "Bar.Baz", ClassData.simple("foo/Bar$Baz"));

        // Not class files.
        byte[] real = Files.readAllBytes(classes.resolve(PACKAGE + "PocketCompass.class"));
        c.refuses("three bytes", "not a class file this tool can read", () -> ClassData.parse(new byte[] {1, 2, 3}));
        c.refuses("a class file cut in half", "not a class file this tool can read", () -> ClassData.parse(Arrays.copyOf(real, real.length / 2)));
        byte[] wrongStart = real.clone();
        wrongStart[0] = 0;
        c.refuses("a wrong first byte", "it doesn't start as a class file does", () -> ClassData.parse(wrongStart));
        c.refuses("a descriptor that isn't one", "`nonsense` isn't a method descriptor", () -> ClassData.parameters("nonsense"));

        System.exit(c.done());
    }

    private static ClassData read(Path classes, String name) throws IOException {
        return ClassData.parse(Files.readAllBytes(classes.resolve(PACKAGE + name + ".class")));
    }

    private static ClassData.Method method(ClassData data, String name) {
        return data.methods.stream().filter(m -> m.name().equals(name)).findFirst().orElseThrow();
    }

    /** A method's steps as short texts, with javac's numbering of lambdas left out. */
    private static List<String> steps(ClassData.Method method) {
        return method.code().stream().map(op -> switch (op.kind()) {
            case LOCAL -> "local " + op.value();
            case STRING -> "\"" + op.value() + "\"";
            case NUMBER -> "number " + op.value();
            case TYPE -> "class " + ClassData.simple(op.owner());
            case GET_STATIC, GET_FIELD -> "get " + ClassData.simple(op.owner()) + "." + op.name();
            case PUT_STATIC, PUT_FIELD -> "put " + ClassData.simple(op.owner()) + "." + op.name();
            case CALL -> "call " + ClassData.simple(op.owner()) + "." + op.name();
            case NEW -> "new " + ClassData.simple(op.owner());
            case LAMBDA -> "lambda " + ClassData.simple(op.owner()) + "." + op.name().replaceAll("\\$\\d+$", "");
        }).toList();
    }
}
