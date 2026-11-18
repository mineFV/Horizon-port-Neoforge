package horizonneo.probe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Supplier;

/** DataFixerUpper's codecs as built (their texts), its ranges, results and lifecycles: no server needed. */
final class CodecProbe {
    private CodecProbe() {}

    record Pair(int a, String b) {}

    record Three(int a, String b, boolean c) {}

    static void run() {
        Codec<?>[] primitives = {Codec.BOOL, Codec.BYTE, Codec.SHORT, Codec.INT, Codec.LONG, Codec.FLOAT, Codec.DOUBLE, Codec.STRING};
        p("codec primitives", () -> java.util.Arrays.toString(primitives));
        p("codec fields", () -> Codec.INT.fieldOf("n") + " | " + Codec.INT.optionalFieldOf("n") + " | " + Codec.INT.optionalFieldOf("n", 3) + " | " + Codec.STRING.fieldOf("s").codec()
                + " | " + Codec.INT.fieldOf("n").fieldOf("m") + " | " + Codec.INT.lenientOptionalFieldOf("n"));
        p("codec derived", () -> Codec.INT.listOf() + " | " + Codec.INT.xmap(i -> i, i -> i) + " | " + Codec.INT.validate(DataResult::success) + " | " + Codec.intRange(0, 5)
                + " | " + Codec.unboundedMap(Codec.STRING, Codec.INT) + " | " + Codec.INT.orElse(4) + " | " + Codec.INT.listOf(1, 3) + " | " + Codec.list(Codec.BOOL)
                + " | " + Codec.withAlternative(Codec.INT, Codec.LONG.xmap(Long::intValue, Integer::longValue)) + " | " + Codec.INT.comapFlatMap(DataResult::success, i -> i)
                + " | " + Codec.INT.flatXmap(DataResult::success, DataResult::success) + " | " + Codec.doubleRange(0, 1) + " | " + Codec.floatRange(0, 1)
                + " | " + Codec.lazyInitialized(() -> Codec.INT) + " | " + Codec.INT.promotePartial(s -> {}) + " | " + Codec.stringResolver(Object::toString, s -> s));
        p("codec record", () -> {
            Codec<Pair> c = RecordCodecBuilder.create(i -> i.group(Codec.INT.fieldOf("a").forGetter(Pair::a), Codec.STRING.optionalFieldOf("b", "x").forGetter(Pair::b)).apply(i, Pair::new));
            MapCodec<Three> m = RecordCodecBuilder.mapCodec(i -> i.group(Codec.INT.fieldOf("a").forGetter(Three::a), Codec.STRING.fieldOf("b").forGetter(Three::b),
                    Codec.BOOL.fieldOf("c").forGetter(Three::c)).apply(i, Three::new));
            Codec<Pair> one = RecordCodecBuilder.create(i -> i.group(Codec.INT.fieldOf("a").forGetter(Pair::a)).apply(i, a -> new Pair(a, "z")));
            return c + " | " + m + " | " + m.codec() + " | " + one + " | " + MapCodec.unit(5) + " | " + MapCodec.unit(5).codec() + " | " + Codec.unit("u");
        });
        p("codec dispatch", () -> Codec.STRING.dispatch(Object::toString, s -> Codec.INT.fieldOf("n")) + " | " + Codec.STRING.dispatch("kind", Object::toString, s -> Codec.INT.fieldOf("n"))
                + " | " + Codec.INT.fieldOf("n").xmap(i -> i, i -> i) + " | " + Codec.INT.fieldOf("n").validate(DataResult::success) + " | " + Codec.INT.fieldOf("n").orElse(3));
        Object[][] ranges = {{0, 5, 3}, {0, 5, 0}, {0, 5, 5}, {0, 5, -1}, {0, 5, 6}, {1.5, 2.5, 2.0}, {1.5, 2.5, 3.0}};
        for (Object[] r : ranges) {
            p("codec range " + java.util.Arrays.toString(r), () -> range(r));
        }
        DataResult<Integer> ok = DataResult.success(3);
        DataResult<Integer> bad = DataResult.error(() -> "bad");
        DataResult<Integer> partial = DataResult.error(() -> "half", 7);
        p("result texts", () -> ok + " | " + bad + " | " + partial + " | " + DataResult.success(null) + " | " + ok.isSuccess() + " " + bad.isSuccess() + " " + bad.isError()
                + " | " + ok.result() + " " + bad.result() + " " + partial.result() + " | " + ok.error().isPresent() + " " + bad.error().isPresent() + " | " + ok.lifecycle() + " " + bad.lifecycle()
                + " | " + ok.hasResultOrPartial() + " " + bad.hasResultOrPartial() + " " + partial.hasResultOrPartial());
        p("result map", () -> ok.map(i -> i + 1) + " | " + bad.map(i -> i + 1) + " | " + partial.map(i -> i + 1) + " | " + ok.flatMap(i -> DataResult.success(i * 2)) + " | "
                + ok.flatMap(i -> DataResult.error(() -> "no")) + " | " + bad.flatMap(i -> DataResult.success(i)) + " | " + partial.flatMap(i -> DataResult.success(i * 2))
                + " | " + partial.flatMap(i -> DataResult.error(() -> "again", i * 3)) + " | " + partial.flatMap(i -> DataResult.error(() -> "again")));
        {
            java.util.List<DataResult<Integer>> rs = java.util.List.of(ok, bad, partial, DataResult.error(() -> "other", 5, Lifecycle.stable()), DataResult.success(4, Lifecycle.stable()),
                    DataResult.success(6, Lifecycle.deprecated(2)));
            for (int i = 0; i < rs.size(); i++) {
                for (int j = 0; j < rs.size(); j++) {
                    DataResult<Integer> x = rs.get(i), y = rs.get(j);
                    p("result apply2 " + i + " " + j, () -> {
                        DataResult<String> s1 = x.apply2stable((u, v) -> u + "+" + v, y), s2 = x.apply2((u, v) -> u + "*" + v, y);
                        return s1 + " " + s1.lifecycle() + " " + s1.hasResultOrPartial() + " | " + s2 + " " + s2.lifecycle();
                    });
                }
            }
        }
        p("products and", () -> {
            Codec<Pair> two = RecordCodecBuilder.create(i -> i.group(Codec.INT.fieldOf("a").forGetter(Pair::a)).and(Codec.STRING.fieldOf("b").forGetter(Pair::b)).apply(i, Pair::new));
            MapCodec<Three> three = RecordCodecBuilder.mapCodec(i -> i.group(Codec.INT.fieldOf("a").forGetter(Three::a)).and(i.group(Codec.STRING.fieldOf("b").forGetter(Three::b),
                    Codec.BOOL.fieldOf("c").forGetter(Three::c))).apply(i, Three::new));
            MapCodec<Three> three2 = RecordCodecBuilder.mapCodec(i -> i.group(Codec.INT.fieldOf("a").forGetter(Three::a), Codec.STRING.fieldOf("b").forGetter(Three::b))
                    .and(Codec.BOOL.fieldOf("c").forGetter(Three::c)).apply(i, Three::new));
            return two + " | " + three + " | " + three2;
        });
        p("result partial", () -> {
            StringBuilder b = new StringBuilder();
            java.util.function.Consumer<String> angle = m -> b.append("<").append(m).append(">");
            java.util.function.Consumer<String> curly = m -> b.append("{").append(m).append("}");
            Object r1 = partial.resultOrPartial(angle), r2 = bad.resultOrPartial(angle), r3 = ok.resultOrPartial(m -> b.append("!"));
            b.append(r1).append(' ').append(r2).append(' ').append(r3);
            Object p1 = partial.promotePartial(curly), p2 = bad.promotePartial(curly);
            b.append(" | ").append(p1).append(' ').append(p2);
            Object s1 = bad.setPartial(9), s2 = ok.setPartial(9), s3 = bad.setPartial(() -> 8), s4 = bad.mapError(m -> m + "!"), s5 = ok.mapError(m -> m + "!");
            b.append(" | ").append(s1).append(' ').append(s2).append(' ').append(s3).append(' ').append(s4).append(' ').append(s5);
            Object o1 = partial.resultOrPartial(), o2 = bad.resultOrPartial();
            b.append(" | ").append(o1).append(' ').append(o2);
            ok.ifSuccess(v -> b.append(" s").append(v)).ifError(e -> b.append(" e").append(e.message()));
            bad.ifSuccess(v -> b.append(" s").append(v)).ifError(e -> b.append(" e").append(e.message()));
            Object m1 = partial.mapOrElse(v -> "v" + v, e -> "e" + e.message()), m2 = ok.mapOrElse(v -> "v" + v, e -> "e");
            return b.append(" | ").append(m1).append(' ').append(m2);
        });
        p("result throw", () -> ok.getOrThrow() + " " + partial.getPartialOrThrow());
        p("result throw bad", () -> bad.getOrThrow());
        p("result throw partial", () -> partial.getOrThrow());
        p("result throw mapped", () -> bad.getOrThrow(m -> new IllegalArgumentException("x" + m)));
        p("lifecycles", () -> Lifecycle.stable() + " " + Lifecycle.experimental() + " " + Lifecycle.deprecated(3) + " " + (Lifecycle.stable() == Lifecycle.stable())
                + " " + Lifecycle.stable().add(Lifecycle.experimental()) + " " + Lifecycle.deprecated(3).add(Lifecycle.deprecated(2)) + " " + Lifecycle.stable().add(Lifecycle.deprecated(4))
                + " " + ((Lifecycle.Deprecated) Lifecycle.deprecated(3).add(Lifecycle.deprecated(2))).since() + " " + ((Lifecycle.Deprecated) Lifecycle.deprecated(3).add(Lifecycle.deprecated(5))).since()
                + " " + ok.setLifecycle(Lifecycle.stable()).lifecycle() + " " + DataResult.success(1, Lifecycle.stable()).addLifecycle(Lifecycle.deprecated(1)).lifecycle()
                + " " + Lifecycle.deprecated(2).equals(Lifecycle.deprecated(2)));
        p("results equal", () -> ok.equals(DataResult.success(3)) + " " + DataResult.success(3, Lifecycle.stable()).equals(DataResult.success(3)) + " " + Optional.of(1).equals(ok.result()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object range(Object[] r) {
        java.util.function.Function f = r[0] instanceof Integer ? Codec.checkRange((Integer) r[0], (Integer) r[1]) : Codec.checkRange((Double) r[0], (Double) r[1]);
        return f.apply(r[2]);
    }

    /** A lambda's text and an identity hash change from run to run: they are written as "<lambda>" and "@?". */
    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, () -> {
            Object o = call.get();
            return o == null ? null : String.valueOf(o).replaceAll("\\S+\\$\\$Lambda[^ \\]]*", "<lambda>").replaceAll("@[0-9a-f]+", "@?");
        });
    }
}
