package horizonneo.probe;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Config files as NeoForge writes them (a rich spec's new file) and reads them (a file written before it loads, with
 * values to correct). In a server the files are NeoForge's; the stand-ins write and read theirs (given()).
 */
final class ConfigFiles {
    private ConfigFiles() {}

    static final ModConfigSpec RICH;
    static final ModConfigSpec FIXED;
    static final ModConfigSpec.IntValue F_COUNT;
    static final ModConfigSpec.BooleanValue F_FLAG;
    static final ModConfigSpec.ConfigValue<String> F_NAME;
    static final ModConfigSpec.EnumValue<Direction> F_SIDE;
    static final ModConfigSpec.DoubleValue F_RATIO;
    static final ModConfigSpec.ConfigValue<List<? extends String>> F_LIST;
    static final ModConfigSpec.IntValue F_MISSING;
    static final ModConfigSpec.LongValue F_BIG;
    static final ModConfigSpec.IntValue F_LOW;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.define("root", 1);
        b.comment("Enabled?").define("enabled", false);
        b.comment("Line one", "Line two").push("machines");
        b.comment("How many").defineInRange("count", 3, 1, 64);
        b.defineInRange("low", 0, Integer.MIN_VALUE, 10);
        b.defineInRange("high", 5, 0, Integer.MAX_VALUE);
        b.defineInRange("energy", 1000000000000L, 0L, Long.MAX_VALUE);
        b.defineInRange("ratio", 1.0E10, 0.0, Double.MAX_VALUE);
        b.defineInRange("small", 0.001, 0.0, 1.0);
        b.comment("Which side").defineEnum("side", Direction.UP);
        b.define("text", "a \"q\" \\ b");
        b.defineListAllowEmpty("ids", () -> List.of(1, 2), () -> 0, o -> o instanceof Integer);
        b.defineListAllowEmpty("none", () -> List.<String>of(), () -> "", o -> o instanceof String);
        b.push("sub");
        b.define("deep", true);
        b.pop();
        b.define("after", "x");
        b.pop();
        b.push("second");
        b.define("one", 1.5);
        b.pop();
        RICH = b.build();

        ModConfigSpec.Builder f = new ModConfigSpec.Builder();
        F_COUNT = f.defineInRange("count", 5, 0, 10);
        F_FLAG = f.define("flag", true);
        F_NAME = f.define("name", "x");
        F_SIDE = f.defineEnum("side", Direction.NORTH);
        F_RATIO = f.defineInRange("ratio", 0.5, 0.0, 1.0);
        F_LIST = f.defineListAllowEmpty("list", () -> List.of("a"), () -> "", o -> o instanceof String);
        F_MISSING = f.defineInRange("missing", 9, 0, 10);
        F_BIG = f.defineInRange("big", 3L, 0L, 10L);
        F_LOW = f.defineInRange("low", 3, 0, 10);
        FIXED = f.build();
    }

    /** The file the probe writes before NeoForge loads its configs: values to read, to correct, and some missing. */
    static final String FIXED_TEXT = "# a comment\ncount = 50\nflag = \"yes\"\nname = 3\nside = \"south\"\nratio = 0.25\nlist = [\"z\", 4]\nextra = 1\nbig = 7\nlow = -4\n";

    static void register(net.neoforged.fml.ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, RICH, "horizonprobe-rich.toml");
        container.registerConfig(ModConfig.Type.COMMON, FIXED, "horizonprobe-fixed.toml");
    }

    /** A file's text, one answer: tabs and line ends written out. */
    static String shown(String text) {
        return text.replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n");
    }

    static void run(java.util.function.Function<String, String> file) {
        p("config file common", () -> shown(file.apply("horizonprobe-common.toml")));
        p("config file rich", () -> shown(file.apply("horizonprobe-rich.toml")));
        p("config fixed values", () -> F_COUNT.get() + " " + F_FLAG.get() + " " + F_NAME.get() + " " + F_SIDE.get() + " " + F_RATIO.get() + " " + F_LIST.get() + " " + F_MISSING.get()
                + " " + F_BIG.get() + " " + F_LOW.get());
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
