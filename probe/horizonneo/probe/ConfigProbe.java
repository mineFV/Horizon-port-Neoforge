package horizonneo.probe;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * A mod's config: built, registered with the probe's container and loaded (in a server by NeoForge; for the stand-ins
 * by the bridge's own loading, given()), then read. A new config file each run: the defaults.
 */
final class ConfigProbe {
    private ConfigProbe() {}

    static final ModConfigSpec.BooleanValue ON;
    static final ModConfigSpec.IntValue COUNT;
    static final ModConfigSpec.LongValue BIG;
    static final ModConfigSpec.DoubleValue RATE;
    static final ModConfigSpec.EnumValue<Direction> SIDE;
    static final ModConfigSpec.ConfigValue<String> NAME;
    static final ModConfigSpec.ConfigValue<List<? extends String>> NAMES;
    static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("General").push("general");
        ON = b.comment("on").define("on", true);
        COUNT = b.translation("x").worldRestart().defineInRange("count", 5, 0, 10);
        BIG = b.gameRestart().defineInRange("big", 7L, 0L, 100L);
        b.push("inner.deep");
        RATE = b.defineInRange("rate", 0.5, 0.0, 1.0);
        SIDE = b.defineEnum("side", Direction.NORTH);
        b.pop(2);
        NAME = b.define("name", "probe");
        NAMES = b.defineListAllowEmpty("names", () -> List.of("a", "b"), () -> "", o -> o instanceof String);
        b.pop();
        SPEC = b.build();
    }

    static final StringBuilder LOADED = new StringBuilder();

    static void loaded(ModConfigEvent.Loading event) {
        ModConfig c = event.getConfig();
        LOADED.append(c.getFileName()).append(' ').append(c.getModId()).append(' ').append(c.getType()).append(' ').append(c.getSpec() == SPEC).append(' ')
                .append(SPEC.isLoaded()).append(' ').append(COUNT.get()).append(" | ");
    }

    /** The stand-ins' container and loading (the bridge's, not the game's API). */
    private static void given() {
        try {
            Class<?> busClass = Class.forName("net.neoforged.bus.api.HorizonEventBus");
            Object bus = busClass.getConstructor().newInstance();
            ModContainer container = (ModContainer) Class.forName("net.neoforged.fml.HorizonModContainer")
                    .getConstructor(String.class, String.class, String.class, net.neoforged.bus.api.IEventBus.class).newInstance("horizonprobe", "1.0", "Probe", bus);
            register(container, container.getEventBus());
            java.lang.reflect.Method text = container.getClass().getMethod("horizonDefaultText", String.class);
            java.lang.reflect.Method load = container.getClass().getMethod("horizonLoadConfig", String.class, String.class, boolean.class);
            for (String name : List.of("horizonprobe-common.toml", "horizonprobe-rich.toml")) load.invoke(container, name, text.invoke(container, name), false);
            load.invoke(container, "horizonprobe-fixed.toml", ConfigFiles.FIXED_TEXT, false);
            FILES = name -> {
                try {
                    return (String) text.invoke(container, name);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            };
        } catch (ClassNotFoundException e) {
            // The game's own: NeoForge registered and loaded it.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** What the probe's mod does with its container and bus (InServer, and given()). */
    /** A config file's text: the server's on disk, the stand-ins' as they write a new one (given()). */
    static java.util.function.Function<String, String> FILES = name -> {
        try {
            return java.nio.file.Files.readString(java.nio.file.Path.of("config", name));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    };

    static void register(ModContainer container, net.neoforged.bus.api.IEventBus bus) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
        ConfigFiles.register(container);
        bus.addListener(ModConfigEvent.Loading.class, ConfigProbe::loaded);
    }

    static void run() {
        given();
        p("config loaded", () -> LOADED);
        p("config values", () -> ON.get() + " " + ON.getAsBoolean() + " " + ON.isTrue() + " " + ON.isFalse() + " " + COUNT.get() + " " + COUNT.getAsInt() + " " + BIG.get() + " " + BIG.getAsLong()
                + " " + RATE.get() + " " + RATE.getAsDouble() + " " + SIDE.get() + " " + NAME.get() + " " + NAMES.get() + " " + SPEC.isLoaded() + " " + SPEC.isEmpty());
        p("config paths", () -> ON.getPath() + " " + COUNT.getPath() + " " + RATE.getPath() + " " + SIDE.getPath() + " " + NAME.getPath() + " " + NAMES.getPath()
                + " " + COUNT.getDefault() + " " + SIDE.getDefault() + " " + NAMES.getDefault());
        p("config set", () -> {
            COUNT.set(8);
            int now = COUNT.get();
            COUNT.set(5);
            return now + " " + COUNT.get();
        });
        p("config not loaded", () -> {
            ModConfigSpec.Builder b = new ModConfigSpec.Builder();
            ModConfigSpec.IntValue v = b.defineInRange("x", 1, 0, 2);
            ModConfigSpec s = b.build();
            return s.isLoaded() + " " + v.getDefault() + " " + v.get();
        });
        p("config bad default", () -> new ModConfigSpec.Builder().defineInRange("x", 50, 0, 10).getClass().getName());
        p("config bad pop", () -> new ModConfigSpec.Builder().push("a").pop(2));
        p("config bad predicate", () -> new ModConfigSpec.Builder().define("x", "v", o -> false).getClass().getName());
        p("config empty", () -> new ModConfigSpec.Builder().build().isEmpty());
        ConfigFiles.run(FILES);
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
