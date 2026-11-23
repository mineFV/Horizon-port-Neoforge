package horizonneo.probe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * The game's NonNullList, a list that refuses null: made empty, with a capacity, of a size filled with a default, on
 * the elements given, or as a copy; what each lets change, and what clear does. And NeoForge's ModelProperty, a
 * predicate a block entity's model data is keyed by (Mekanism makes them in its transmitters' initializer).
 */
final class ListProbe {
    private ListProbe() {}

    static void run() {
        p("nonnull create", () -> {
            NonNullList<String> l = NonNullList.create();
            StringBuilder b = new StringBuilder().append(l.size()).append(l.isEmpty()).append(' ');
            l.add("a");
            l.add("b");
            l.add(0, "c");
            b.append(l).append(' ').append(l.size()).append(' ').append(l.get(1)).append(' ').append(l.set(0, "d")).append(' ').append(l.remove(1)).append(' ').append(l);
            b.append(' ').append(tried(() -> l.add(null))).append(' ').append(tried(() -> l.set(0, null))).append(' ').append(tried(() -> {
                l.add(0, null);
                return l;
            }));
            b.append(' ').append(l.equals(List.of("d", "b"))).append(l.hashCode() == List.of("d", "b").hashCode()).append(l.indexOf("b")).append(l.contains("d"));
            b.append(' ').append(String.join(",", l.toArray(String[]::new))).append(' ').append(l.stream().map(s -> s + s).toList());
            StringBuilder seen = new StringBuilder();
            for (String s : l) seen.append(s);
            b.append(' ').append(seen).append(' ').append(l.getClass().getName());
            l.clear();
            b.append(' ').append(l).append(l.size()).append(' ').append(tried(() -> l.get(0))).append(' ').append(tried(() -> l.remove(0)));
            return b;
        });
        p("nonnull capacity", () -> {
            NonNullList<String> l = NonNullList.createWithCapacity(4);
            l.add("a");
            l.add("b");
            l.add("c");
            l.add("d");
            l.add("e");
            return l + " " + l.size() + " " + tried(() -> NonNullList.createWithCapacity(-1)) + " " + tried(() -> l.add(null)) + " " + NonNullList.createWithCapacity(0);
        });
        p("nonnull with size", () -> {
            NonNullList<String> l = NonNullList.withSize(3, "x");
            StringBuilder b = new StringBuilder().append(l).append(' ').append(l.size()).append(l.isEmpty()).append(' ').append(l.set(1, "y")).append(' ').append(l);
            b.append(' ').append(tried(() -> l.add("z"))).append(' ').append(tried(() -> l.remove(0))).append(' ').append(tried(() -> {
                l.add(0, "z");
                return l;
            }));
            b.append(' ').append(tried(() -> l.set(0, null))).append(' ').append(tried(() -> l.add(null)));
            l.clear();
            b.append(' ').append(l).append(' ').append(tried(() -> NonNullList.withSize(2, null))).append(' ').append(tried(() -> NonNullList.withSize(-1, "x")))
                    .append(' ').append(NonNullList.withSize(0, "x")).append(NonNullList.withSize(0, "x").isEmpty());
            NonNullList<String> empty = NonNullList.withSize(0, "x");
            empty.clear();
            return b.append(' ').append(empty);
        });
        p("nonnull of", () -> {
            String[] given = {"a", "b"};
            NonNullList<String> l = NonNullList.of("d", given);
            StringBuilder b = new StringBuilder().append(l).append(' ').append(l.size()).append(' ').append(tried(() -> l.add("c"))).append(' ').append(l.set(0, "e"))
                    .append(' ').append(l).append(' ').append(given[0]).append(' ').append(tried(() -> l.set(1, null)));
            l.clear();
            b.append(' ').append(l).append(' ').append(given[1]);
            NonNullList<String> none = NonNullList.of(null, "a", "b");
            b.append(' ').append(tried(() -> {
                none.clear();
                return none;
            })).append(' ').append(none);
            NonNullList<String> holes = NonNullList.of("d", "a", null);
            b.append(' ').append(holes).append(' ').append(holes.contains(null)).append(' ').append(NonNullList.of("d")).append(NonNullList.of("d").size());
            holes.clear();
            return b.append(' ').append(holes);
        });
        p("nonnull copy", () -> {
            List<String> source = new ArrayList<>(List.of("a", "b"));
            NonNullList<String> c = NonNullList.copyOf(source);
            source.set(0, "z");
            StringBuilder b = new StringBuilder().append(c).append(' ').append(tried(() -> c.add("c"))).append(' ').append(c).append(' ').append(tried(() -> c.set(0, "y")))
                    .append(' ').append(c).append(' ').append(tried(() -> c.add(null))).append(' ').append(tried(() -> c.remove(0))).append(' ').append(c);
            b.append(' ').append(tried(() -> {
                c.clear();
                return c;
            })).append(c.size());
            b.append(' ').append(tried(() -> NonNullList.copyOf(Arrays.asList("a", null)))).append(' ').append(tried(() -> NonNullList.copyOf(null)));
            b.append(' ').append(NonNullList.copyOf(List.of())).append(' ').append(NonNullList.copyOf(new java.util.TreeSet<>(List.of("q", "p"))));
            return b;
        });
        p("model property", () -> {
            ModelProperty<String> any = new ModelProperty<>();
            ModelProperty<String> some = new ModelProperty<>(s -> s.startsWith("a"));
            return any.test("x") + " " + any.test(null) + " " + some.test("abc") + " " + some.test("x") + " " + tried(() -> some.test(null)) + " "
                    + any.equals(new ModelProperty<String>()) + " " + any.equals(any) + " " + any.getClass().getName() + " " + any.negate().test("x") + " "
                    + tried(() -> new ModelProperty<String>(null) != null) + " " + tried(() -> new ModelProperty<String>(null).test("x"))
                    + " " + some.and(s -> s.length() > 2).test("abc") + some.or(s -> true).test("x");
        });
    }

    /**
     * What call gives, or what it throws: its class and message (not the JVM's own message of a NullPointerException,
     * built from the bytecode, which GameProbe doesn't compare either).
     */
    static String tried(java.util.function.Supplier<Object> call) {
        try {
            return String.valueOf(call.get());
        } catch (RuntimeException e) {
            boolean jvm = e instanceof NullPointerException && e.getMessage() != null && e.getMessage().startsWith("Cannot ");
            return e.getClass().getSimpleName() + "(" + (jvm ? "" : e.getMessage()) + ")";
        }
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
