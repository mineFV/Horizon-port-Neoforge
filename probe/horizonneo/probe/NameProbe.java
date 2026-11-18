package horizonneo.probe;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.util.StringRepresentable;

/** The game's lookups of values by name (StringRepresentable): a small enum and one past the map's threshold. */
final class NameProbe {
    private NameProbe() {}

    enum Small implements StringRepresentable {
        A("alpha"), B("beta"), C("alpha");

        private final String name;

        Small(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    enum Big implements StringRepresentable {
        V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15, V16, V17;

        @Override
        public String getSerializedName() {
            return "v" + ordinal();
        }
    }

    enum BigTwice implements StringRepresentable {
        V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15, V16, V17, DUP;

        @Override
        public String getSerializedName() {
            return this == DUP ? "v3" : "v" + ordinal();
        }
    }

    static void run() {
        StringRepresentable.EnumCodec<Small> small = StringRepresentable.fromEnum(Small::values);
        StringRepresentable.EnumCodec<Big> big = StringRepresentable.fromEnum(Big::values);
        StringRepresentable.EnumCodec<Small> mapped = StringRepresentable.fromEnumWithMapping(Small::values, s -> "x_" + s);
        String[] names = {"alpha", "beta", "v3", "v17", "V3", "nope", "", "x_alpha"};
        for (String n : names) {
            p("name " + n, () -> small.byName(n) + " " + small.byName(n, Small.B) + " " + big.byName(n) + " " + big.byName(n, Big.V9) + " " + mapped.byName(n));
        }
        p("name null", () -> small.byName(null) + " " + big.byName(null, Big.V1));
        p("name twice", () -> StringRepresentable.fromEnum(BigTwice::values));
        p("name threshold", () -> StringRepresentable.PRE_BUILT_MAP_THRESHOLD);
        Function<String, Small> lookup = StringRepresentable.createNameLookup(Small.values(), s -> s + "!");
        p("name lookup", () -> lookup.apply("alpha!") + " " + lookup.apply("alpha") + " " + StringRepresentable.createNameLookup(Big.values(), s -> s).apply("v3"));
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
