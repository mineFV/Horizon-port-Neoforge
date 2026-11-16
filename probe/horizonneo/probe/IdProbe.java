package horizonneo.probe;

import java.util.function.IntFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;

/** Ids (ResourceLocation) and ByIdMap, as GameProbe observes the rest (see there). */
final class IdProbe {
    private IdProbe() {}

    static final String[] PARTS = {"", "a", "minecraft", "mekanism", "my_mod", "Upper", "a.b-c_d", "a/b", "path/to/x.png",
        "with space", "a:b", ":", "a:", ":b", "a:b:c", "é", "-", ".", "..", "x/../y", "UPPER:case", "ns:Path", "q\"", "back\\", "new\nline", "tab\t", "\u0001\u001f\u007f", "😀", "日"};

    static void run() {
        for (String ns : PARTS) {
            for (String path : PARTS) {
                String k = "id [" + ns + "] [" + path + "]";
                p(k + " from", () -> shown(ResourceLocation.fromNamespaceAndPath(ns, path)));
                p(k + " tryBuild", () -> shown(ResourceLocation.tryBuild(ns, path)));
            }
            String k = "id [" + ns + "]";
            p(k + " parse", () -> shown(ResourceLocation.parse(ns)));
            p(k + " tryParse", () -> shown(ResourceLocation.tryParse(ns)));
            p(k + " default", () -> shown(ResourceLocation.withDefaultNamespace(ns)));
            p(k + " valid", () -> ResourceLocation.isValidNamespace(ns) + " " + ResourceLocation.isValidPath(ns));
        }
        for (char c = 0; c < 256; c++) {
            char ch = c;
            p("id char " + (int) c, () -> ResourceLocation.validPathChar(ch) + " " + ResourceLocation.validNamespaceChar(ch) + " " + ResourceLocation.isAllowedInResourceLocation(ch));
        }
        ResourceLocation[] ids = {ResourceLocation.fromNamespaceAndPath("mekanism", "steel_casing"), ResourceLocation.fromNamespaceAndPath("minecraft", "stone"),
            ResourceLocation.withDefaultNamespace("stone"), ResourceLocation.fromNamespaceAndPath("a", "z"), ResourceLocation.fromNamespaceAndPath("z", "a"),
            ResourceLocation.fromNamespaceAndPath("mekanism", "block/steel_casing"), ResourceLocation.parse("a:b/c.d")};
        for (int i = 0; i < ids.length; i++) {
            ResourceLocation a = ids[i];
            String k = "id " + i;
            p(k + " parts", () -> a.getNamespace() + " " + a.getPath() + " " + a + " " + a.hashCode());
            p(k + " keys", () -> a.toLanguageKey() + " " + a.toLanguageKey("block") + " " + a.toLanguageKey("item", "tip") + " " + a.toShortLanguageKey() + " " + a.toDebugFileName());
            p(k + " with", () -> shown(a.withPath("new/path")) + " " + shown(a.withPrefix("pre_")) + " " + shown(a.withSuffix("_post")) + " " + (a.withPath(a.getPath()) == a));
            p(k + " with bad", () -> shown(a.withPath("Bad Path")));
            p(k + " prefix bad", () -> shown(a.withPrefix("A:")));
            for (int j = 0; j < ids.length; j++) {
                ResourceLocation b = ids[j];
                p(k + " vs " + j, () -> a.equals(b) + " " + a.compareTo(b) + " " + a.compareNamespaced(b));
            }
            p(k + " equals other", () -> a.equals(null) + " " + a.equals(a.toString()));
        }
        byId();
    }

    static String shown(Object o) {
        return o == null ? "null" : o.getClass().getName() + ":" + o;
    }

    static void byId() {
        String[] values = {"a", "bb", "ccc", "dddd"};
        for (ByIdMap.OutOfBoundsStrategy s : ByIdMap.OutOfBoundsStrategy.values()) {
            p("byId strategy " + s.ordinal(), () -> s.name() + " " + s);
            IntFunction<String> f = ByIdMap.continuous(v -> v.length() - 1, values, s);
            for (int i = -9; i <= 9; i++) {
                int n = i;
                p("byId " + s + " " + n, () -> f.apply(n));
            }
            p("byId " + s + " extremes", () -> f.apply(Integer.MIN_VALUE) + " " + f.apply(Integer.MAX_VALUE));
            p("byId " + s + " gap", () -> ByIdMap.continuous(v -> v.length(), values, s).apply(1));
            p("byId " + s + " twice", () -> ByIdMap.continuous(v -> 0, values, s).apply(0));
            p("byId " + s + " none", () -> ByIdMap.continuous(v -> 0, new String[0], s).apply(0));
            p("byId " + s + " single", () -> ByIdMap.continuous(v -> 0, new String[] {"only"}, s).apply(5));
        }
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
