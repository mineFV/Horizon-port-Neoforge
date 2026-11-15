package com.example.numbers;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Made up for the translator's tests: collections and Optional, the library model's third part. Each public static
 * method answers with a text, so that `NumbersCheck` and `numbers_check.rs` can compare them. The JDK's HashMap order
 * is part of what is compared; the order of Set.of and Map.of isn't (the JDK salts it at random, run to run).
 */
public final class Gathered {
    private Gathered() {}

    /** Constants of the class file (ConstantValue), negative ones among them. */
    static final int LOW = -3;
    static final float LOWER = -2.5f;
    static int counted = LOW;

    interface Act {
        Object run();
    }

    /** What the act gives, or the exception it throws (a NullPointerException's message is the JDK's own: left out). */
    static String attempt(Act act) {
        try {
            return String.valueOf(act.run());
        } catch (NullPointerException e) {
            return "NPE";
        } catch (ClassCastException e) {
            return "CCE";
        } catch (RuntimeException e) {
            return e.toString();
        }
    }

    record Item(String name, int weight) {}

    /** A cache that keeps its last three: removeEldestEntry, overridden. */
    static final class Recent extends LinkedHashMap<Integer, String> {
        Recent() {
            super(16, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
            return size() > 3;
        }
    }

    // ---- ArrayList ----

    public static String lists(int n) {
        List<String> l = new ArrayList<>();
        counted += LOW;
        for (int i = 0; i < n + 3; i++) l.add("e" + (i * 7 % 5));
        l.add(1, "first");
        String s = l + " " + l.size() + " " + l.indexOf("e2") + " " + l.lastIndexOf("e2") + " " + l.contains("e4") + " " + l.get(0) + " " + l.getLast();
        l.remove(0);
        l.remove("e2");
        l.set(0, "set");
        s += " | " + l + " " + l.hashCode() + " " + l.equals(new ArrayList<>(l)) + " " + l.subList(1, Math.min(3, l.size()));
        l.removeIf(e -> e.endsWith("1"));
        l.replaceAll(e -> e.startsWith("s") ? e : e + "!");
        s += " | " + l;
        l.sort(null);
        s += " " + l;
        Iterator<String> it = l.iterator();
        while (it.hasNext()) if (it.next().startsWith("e3")) it.remove();
        s += " " + l + " " + l.toArray(new String[0]).length + " " + Arrays.toString(l.toArray(new String[] {"x", "y", "z", "w", "v", "u", "t", "s", "r", "q"}));
        s += " | " + attempt(() -> l.get(n + 10)) + " " + attempt(() -> {
            l.add(n + 20, "far");
            return "added";
        }) + " " + attempt(() -> {
            for (String e : l) if (e.length() > 1) l.remove(e);
            return "removed";
        }) + " " + attempt(() -> new ArrayList<String>().getFirst()) + " " + attempt(() -> l.subList(2, 1));
        List<Integer> numbers = new ArrayList<>(List.of(5, 3, 9, 1));
        numbers.addAll(1, List.of(n, -n));
        numbers.removeAll(List.of(9));
        numbers.retainAll(List.of(5, 3, 1, n, -n));
        s += " | " + numbers + " " + numbers.equals(List.of(5, n, -n, 3, 1)) + " " + List.of(5, n, -n, 3, 1).equals(numbers);
        List<Integer> view = numbers.subList(1, 3);
        view.set(0, 100);
        view.add(200);
        s += " " + numbers + " " + view;
        return s;
    }

    // ---- HashMap, LinkedHashMap ----

    public static String maps(int n) {
        // Keys that share bins: up to 12 in a bin of a table under 64 (the JDK grows the table), never 9 in a table
        // of 64 or more (the JDK makes such a bin a tree, iterated in another order: lib/README.md).
        Map<Object, Object> m = new HashMap<>();
        for (int i = 0; i < 4 + n * 2; i++) m.put(i * 16 + n, "v" + i);
        m.put("Aa", 1);
        m.put("BB", 2);
        m.put(null, "nothing");
        m.put(17, "seventeen");
        String s = m.toString() + " " + m.size() + " " + m.get(16 + n) + " " + m.get("BB") + " " + m.get(null) + " " + m.containsKey(1000) + " "
                + m.containsValue("v2") + " " + m.hashCode();
        m.remove(n);
        m.remove("Aa");
        s += " | " + m.keySet() + " " + m.values();
        Map<Integer, Integer> c = new HashMap<>();
        for (int i = 0; i < 6; i++) c.put(i * 32, i);
        c.computeIfAbsent(64 + 16, k -> 7);
        c.computeIfAbsent(n * 32 + 16, k -> 8);
        c.computeIfAbsent(5, k -> null);
        c.compute(0, (k, v) -> v == null ? 1 : v + 100);
        c.compute(n * 64, (k, v) -> null);
        c.merge(32, 5, Integer::sum);
        c.merge(48, 5, Integer::sum);
        c.putIfAbsent(32, 0);
        c.putIfAbsent(96 + n, 9);
        s += " | " + c + " " + c.getOrDefault(1, -1) + " " + c.computeIfPresent(48, (k, v) -> v * 2) + " " + c.replace(48, 1) + " " + c;
        for (Map.Entry<Integer, Integer> e : c.entrySet()) e.setValue(e.getValue() + e.getKey());
        c.entrySet().removeIf(e -> e.getKey() % 64 == 0);
        s += " " + c + " " + c.equals(new HashMap<>(c)) + " " + new HashMap<>(c).toString().equals(c.toString());
        s += " " + attempt(() -> c.computeIfAbsent(1000, k -> c.put(2000, 1))) + " " + attempt(() -> {
            for (Integer k : c.keySet()) c.remove(k);
            return "removed";
        });
        Map<String, Integer> counts = new HashMap<>();
        for (String w : List.of("the", "cat", "and", "the", "dog", "and", "the", "bird", "w" + n)) counts.merge(w, 1, Integer::sum);
        s += " | " + counts;
        Map<String, Integer> linked = new LinkedHashMap<>();
        for (int i = 0; i < 5 + n; i++) linked.put("k" + (i * 3 % 7), i);
        linked.put("k0", 99);
        linked.remove("k3");
        s += " | " + linked + " " + linked.keySet() + " " + linked.equals(new HashMap<>(linked));
        Recent recent = new Recent();
        for (int i = 0; i < 5; i++) {
            recent.put(i % (n + 2), "r" + i);
            recent.get(0);
        }
        s += " " + recent;
        SequencedMap<String, Integer> seq = new LinkedHashMap<>(linked);
        s += " " + seq.size() + " " + seq.get("k0") + " " + seq.entrySet().iterator().next();
        return s;
    }

    // ---- HashSet, LinkedHashSet ----

    public static String sets(int n) {
        Set<Integer> h = new HashSet<>();
        for (int i = 0; i < 10 + n * 2; i++) h.add(i * 17 % 40);
        Set<String> words = new HashSet<>(List.of("pear", "apple", "fig", "kiwi", "plum", "lime", "date", "word" + n));
        LinkedHashSet<String> kept = new LinkedHashSet<>(List.of("z", "a", "m", "b"));
        kept.add("a");
        kept.add("c" + n);
        String s = h + " " + h.size() + " " + h.contains(17) + " " + words + " " + kept + " " + kept.getFirst();
        h.removeAll(Set.of(0, 17, 34));
        h.retainAll(new HashSet<>(List.of(1, 2, 3, 6, 7, 11, 14, 22, 23, 24, 31)));
        s += " | " + h + " " + h.equals(new HashSet<>(h)) + " " + h.hashCode() + " " + new HashSet<>(h).equals(h);
        Set<Integer> of = Set.of(3, 1, 4, n + 10);
        s += " | " + of.size() + " " + of.contains(4) + " " + of.contains(2) + " " + of.equals(new HashSet<>(List.of(1, 3, 4, n + 10))) + " " + of.hashCode() + " "
                + attempt(() -> Set.of(1, 2, 1)) + " " + attempt(() -> of.contains(null)) + " " + attempt(() -> of.add(5)) + " " + Set.copyOf(List.of(2, 2, 2));
        return s;
    }

    // ---- List.of, Map.of, Collections ----

    public static String immutables(int n) {
        List<Integer> one = List.of(n), two = List.of(n, n + 1), many = List.of(1, 2, 3, n), none = List.of();
        String s = one + " " + two + " " + many + " " + none + " " + (none == List.<Integer>of()) + " " + (List.copyOf(many) == many) + " " + many.equals(Arrays.asList(1, 2, 3, n))
                + " " + many.hashCode() + " " + many.indexOf(n) + " " + many.subList(1, 3) + " " + two.getLast();
        s += " | " + attempt(() -> one.get(n + 1)) + " " + attempt(() -> two.get(-1)) + " " + attempt(() -> many.get(4)) + " " + attempt(() -> none.get(0)) + " "
                + attempt(() -> many.add(1)) + " " + attempt(() -> many.contains(null)) + " " + attempt(() -> List.of(1, null)) + " "
                + attempt(() -> none.getFirst()) + " " + attempt(() -> many.iterator().next());
        Map<String, Integer> m = Map.of("a", 1, "b", n);
        Map<String, Integer> big = Map.ofEntries(Map.entry("x", 1), Map.entry("y", 2), Map.entry("z", n));
        s += " | " + m.get("b") + " " + m.size() + " " + m.containsKey("c") + " " + m.getOrDefault("c", -1) + " " + big.get("z") + " " + big.hashCode() + " "
                + m.equals(new HashMap<>(Map.of("b", n, "a", 1))) + " " + Map.entry("k", n) + " " + Map.entry("k", n).hashCode() + " " + Map.of("only", n) + " "
                + attempt(() -> Map.of("a", 1, "a", 2)) + " " + attempt(() -> m.put("c", 3)) + " " + attempt(() -> m.get(null)) + " "
                + attempt(() -> Map.entry("k", 1).setValue(2)) + " " + attempt(() -> m.computeIfAbsent("a", k -> 5)) + " " + Map.copyOf(new HashMap<>(Map.of("q", n)));
        List<String> empty = Collections.emptyList();
        List<String> single = Collections.singletonList("only" + n);
        List<String> backing = new ArrayList<>(List.of("p", "q"));
        List<String> view = Collections.unmodifiableList(backing);
        backing.add("r" + n);
        s += " | " + empty + " " + empty.size() + " " + single + " " + single.hashCode() + " " + view + " " + (Collections.unmodifiableList(view) == view) + " "
                + Collections.singleton(n) + " " + Collections.singletonMap("k", n) + " " + Collections.emptyMap() + " " + Collections.emptySet().contains(null) + " "
                + attempt(() -> empty.get(n)) + " " + attempt(() -> single.get(1)) + " " + attempt(() -> single.add("x")) + " " + attempt(() -> view.add("x")) + " "
                + attempt(() -> view.get(9)) + " " + attempt(() -> Collections.emptyIterator().next());
        Map<String, Integer> um = Collections.unmodifiableMap(new HashMap<>(Map.of("a", n)));
        s += " " + um + " " + attempt(() -> um.entrySet().iterator().next().setValue(5)) + " " + attempt(() -> um.remove("a"));
        List<Integer> sorted = new ArrayList<>(List.of(4, 1, n, 9, -n));
        Collections.sort(sorted);
        s += " | " + sorted;
        Collections.reverse(sorted);
        Collections.swap(sorted, 0, 1);
        Collections.addAll(sorted, 7, 7);
        s += " " + sorted + " " + Collections.max(sorted) + " " + Collections.min(sorted) + " " + Collections.frequency(sorted, 7) + " " + Collections.nCopies(3, "c" + n);
        return s;
    }

    // ---- Optional ----

    public static String optionals(int n) {
        Optional<String> some = Optional.of("v" + n);
        Optional<String> none = n % 2 == 0 ? Optional.empty() : Optional.ofNullable(null);
        StringBuilderLike seen = new StringBuilderLike();
        some.ifPresent(v -> seen.add("saw " + v));
        none.ifPresentOrElse(v -> seen.add("odd"), () -> seen.add("nothing"));
        return some + " " + none + " " + some.isPresent() + " " + none.isEmpty() + " " + some.get() + " " + none.orElse("else") + " " + none.orElseGet(() -> "got") + " "
                + some.map(String::length) + " " + some.filter(v -> v.length() > 2) + " " + some.flatMap(v -> Optional.of(v + v)) + " " + none.or(() -> Optional.of("or")) + " "
                + some.equals(Optional.of("v" + n)) + " " + some.hashCode() + " " + none.hashCode() + " " + (none == Optional.<String>empty()) + " " + seen.text + " "
                + attempt(() -> none.get()) + " " + attempt(() -> none.orElseThrow()) + " " + attempt(() -> Optional.of(null)) + " "
                + attempt(() -> none.orElseThrow(() -> new IllegalStateException("missing " + n)));
    }

    /** A text that lambdas add to. */
    static final class StringBuilderLike {
        String text = "";

        void add(String s) {
            text = text.isEmpty() ? s : text + "," + s;
        }
    }

    // ---- Arrays, Comparator ----

    public static String arrays(int n) {
        int[] a = {5, n, -3, 9, n * 2, 0};
        int[] copy = Arrays.copyOf(a, 8);
        Arrays.sort(a);
        double[] d = {3, -0.0, Double.NaN, 0.0, -n};
        Arrays.sort(d);
        String[] words = {"pear", "fig", "apple", "kiwi" + n};
        String[] longer = Arrays.copyOf(words, 6);
        Arrays.sort(words);
        long[] filled = new long[4];
        Arrays.fill(filled, 1, 3, n);
        List<String> asList = Arrays.asList("x", "y", "z");
        asList.set(1, "Y" + n);
        String s = Arrays.toString(a) + " " + Arrays.toString(copy) + " " + Arrays.toString(d) + " " + Arrays.toString(words) + " " + Arrays.toString(longer) + " "
                + Arrays.toString(filled) + " " + Arrays.hashCode(a) + " " + Arrays.hashCode(d) + " " + Arrays.hashCode(words) + " " + Arrays.equals(a, Arrays.copyOf(a, 6))
                + " " + Arrays.equals(new double[] {Double.NaN}, new double[] {Double.NaN}) + " " + Arrays.toString(Arrays.copyOfRange(copy, 2, 10)) + " "
                + Arrays.toString(Arrays.copyOfRange(words, 1, 3)) + " " + asList + " " + Arrays.toString((int[]) null) + " " + Arrays.hashCode(new boolean[] {true, n > 2});
        s += " | " + attempt(() -> Arrays.copyOf(a, -n - 1)) + " " + attempt(() -> Arrays.copyOfRange(a, 3, 2)) + " " + attempt(() -> Arrays.copyOfRange(a, 9, 10)) + " "
                + attempt(() -> Arrays.copyOfRange(a, -1, 2)) + " " + attempt(() -> {
                    Arrays.fill(a, 0, 10, 1);
                    return "filled";
                }) + " " + attempt(() -> asList.add("w")) + " " + attempt(() -> asList.get(5)) + " " + attempt(() -> {
                    Arrays.sort(new Object[] {1, "a"});
                    return "sorted";
                });
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 7; i++) items.add(new Item("i" + (i * 5 % 3), (i * n + 3) % 4));
        items.sort(Comparator.comparingInt(Item::weight));
        s += " | " + items;
        items.sort(Comparator.comparing(Item::name).thenComparing(Item::weight, Comparator.reverseOrder()));
        s += " " + items;
        items.sort(Comparator.comparing(Item::name).reversed().thenComparingInt(Item::weight));
        s += " " + items;
        List<Integer> ints = new ArrayList<>(List.of(3, n, 8, -1));
        ints.sort(Comparator.reverseOrder());
        s += " " + ints;
        ints.sort(Comparator.<Integer>naturalOrder().reversed().reversed());
        s += " " + ints + " " + Collections.max(ints, Comparator.reverseOrder());
        Map<String, Integer> m = new HashMap<>(Map.of("b", 2, "a", n, "c", 1));
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(m.entrySet());
        entries.sort(Map.Entry.comparingByValue());
        s += " " + entries;
        entries.sort(Map.Entry.comparingByKey());
        s += " " + entries + " " + new AbstractMap.SimpleEntry<>("s", n) + " " + collect(List.of(1, 2, n), new ArrayList<>());
        return s;
    }

    // ---- StringBuilder ----

    public static String builders(int n) {
        StringBuilder b = new StringBuilder();
        b.append("n=").append(n).append(' ').append(n * 3L).append(' ').append(n / 4f).append(' ').append(n / 8.0).append(' ').append(n > 3).append(' ')
                .append((Object) null).append(' ').append((String) null).append(new char[] {'x', 'y'}).append("abcdef", 1, 3 + n % 3);
        StringBuilder c = new StringBuilder("start").insert(0, n).insert(2, '!').insert(b.length() > 100 ? 0 : 3, "ins");
        c.setCharAt(0, 'Z');
        c.append(c);
        String s = b + " | " + c + " " + c.length() + " " + c.charAt(n % c.length()) + " " + c.indexOf("ins") + " " + c.lastIndexOf("t") + " " + c.substring(2, 5);
        c.delete(1, 4).deleteCharAt(0).replace(0, 2, "<" + n + ">").reverse();
        s += " | " + c + " " + new StringBuilder("a\uD83D\uDE00b").reverse().toString().codePointAt(1) + " " + new StringBuilder("abc").compareTo(new StringBuilder("abd"));
        c.setLength(3);
        s += " " + c + " " + c.isEmpty();
        c.setLength(5);
        s += " " + c.toString().replace('\0', '_') + " | " + attempt(() -> new StringBuilder("abc").charAt(n + 3)) + " " + attempt(() -> new StringBuilder("abc").delete(2, 1)) + " "
                + attempt(() -> new StringBuilder("abc").insert(n + 4, "x")) + " " + attempt(() -> new StringBuilder("abc").substring(n + 4)) + " "
                + attempt(() -> new StringBuilder().append("abc", 2, 1)) + " " + attempt(() -> new StringBuilder(-1 - n)) + " " + attempt(() -> {
                    new StringBuilder("ab").setLength(-n - 1);
                    return "set";
                }) + " " + new StringBuilder("abc").delete(1, 10 + n) + " " + new StringBuilder("abc").replace(1, 10, "x" + n);
        return s;
    }

    // ---- Class objects ----

    public static String classes(int n) {
        class Local {}
        Runnable lambda = () -> {};
        Object[] things = {"s", n, new ArrayList<>(), List.of(1), Boxes.Color.GREEN, Boxes.Color.RED, new int[1], new String[0], new Item("a", 1), new Object(),
            Optional.empty(), new Local(), new Object() {}, new Item[1][], Map.entry(1, 2)};
        Object o = things[n % things.length];
        Class<?> c = o.getClass();
        String s = c.getName() + " " + c.getSimpleName() + " " + c.getCanonicalName() + " " + c.getTypeName() + " " + c + " " + c.isArray() + " " + c.isEnum() + " "
                + c.isInterface() + " " + c.getSuperclass() + " " + (c == o.getClass()) + " " + c.isInstance(o) + " " + Comparable.class.isInstance(o) + " "
                + Object[].class.isInstance(o) + " " + Cloneable.class.isInstance(o) + " " + java.io.Serializable.class.isInstance(o) + " " + Runnable.class.isInstance(o) + " "
                + Number.class.isAssignableFrom(c) + " " + c.isAssignableFrom(Integer.class) + " " + attempt(() -> String.class.cast(o)) + " "
                + (o instanceof Enum<?> e ? e.getDeclaringClass().getName() : "-");
        s += " | " + (String.class == "x".getClass()) + " " + (int[].class == new int[0].getClass()) + " " + Integer.class.isInstance(n) + " " + Runnable.class.isInstance(lambda)
                + " " + Runnable.class.isInterface() + " " + Runnable.class + " " + Comparable.class.getSuperclass() + " " + Object.class.getSuperclass() + " "
                + String[].class.getSuperclass() + " " + Object[].class.isAssignableFrom(String[][].class) + " " + int[].class.getSimpleName() + " "
                + Map.Entry.class.getCanonicalName() + " " + Arrays.toString(Boxes.Color.class.getEnumConstants()) + " " + (Boxes.Color.class.getEnumConstants() != Boxes.Color.class.getEnumConstants())
                + " " + String.class.getEnumConstants() + " " + Boxes.Color.GREEN.getClass().getEnumConstants() + " " + Enum.valueOf(Boxes.Color.class, "BLUE") + " "
                + Boxes.Color.valueOf(n % 2 == 0 ? "RED" : "GREEN") + " " + attempt(() -> Enum.valueOf(Boxes.Color.class, "PURPLE" + n)) + " "
                + attempt(() -> Boxes.Color.valueOf(null)) + " " + Gathered.class.desiredAssertionStatus() + " " + checked(n);
        Class<?>[] primitives = {int.class, Boolean.TYPE, void.class, long.class, char.class, double.class, Void.TYPE, byte.class, short.class, float.class};
        Class<?> p = primitives[n % primitives.length];
        s += " | " + p + " " + p.getName() + " " + p.getSimpleName() + " " + p.getCanonicalName() + " " + p.getTypeName() + " " + p.isPrimitive() + " " + p.isArray() + " "
                + p.isInterface() + " " + p.isEnum() + " " + p.getSuperclass() + " " + p.isAssignableFrom(p) + " " + Object.class.isAssignableFrom(p) + " "
                + p.isAssignableFrom(Integer.class) + " " + p.isInstance(n) + " " + (p == int.class) + " " + (p == Integer.TYPE) + " " + Integer.class.isPrimitive() + " "
                + p.getEnumConstants() + " " + attempt(() -> p.cast(n)) + " " + p.cast(null) + " " + (p.hashCode() == p.hashCode()) + " " + p.equals(long.class);
        return s;
    }

    /** An assert: off, as a JVM's are unless told otherwise. */
    static String checked(int n) {
        assert n < 0 : "never checked";
        return "unchecked";
    }

    /** More than 64 constants: a JumboEnumSet's. */
    enum Many {
        M0, M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18, M19, M20, M21, M22, M23, M24, M25, M26, M27, M28, M29, M30, M31, M32, M33, M34, M35, M36, M37, M38, M39, M40, M41, M42, M43, M44, M45, M46, M47, M48, M49, M50, M51, M52, M53, M54, M55, M56, M57, M58, M59, M60, M61, M62, M63, M64, M65, M66, M67, M68, M69
    }

    // ---- EnumMap, EnumSet ----

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static String enumCollections(int n) {
        Boxes.Color[] colors = Boxes.Color.values();
        EnumMap<Boxes.Color, Integer> m = new EnumMap<>(Boxes.Color.class);
        m.put(colors[n % 3], n);
        m.put(Boxes.Color.GREEN, null);
        m.put(colors[(n + 2) % 3], n * 2);
        String s = m + " " + m.size() + " " + m.get(Boxes.Color.GREEN) + " " + m.containsKey(Boxes.Color.GREEN) + " " + m.containsValue(null) + " " + m.get("RED") + " "
                + m.keySet() + " " + m.values() + " " + m.equals(new HashMap<>(m)) + " " + new HashMap<>(m).equals(m) + " " + new EnumMap<>(m) + " "
                + new EnumMap<>(new HashMap<>(Map.of(Boxes.Color.BLUE, 1))) + " " + m.remove(Boxes.Color.GREEN) + " " + m;
        for (Map.Entry<Boxes.Color, Integer> e : m.entrySet()) e.setValue(e.getValue() == null ? -1 : e.getValue() + 100);
        m.entrySet().removeIf(e -> e.getKey() == Boxes.Color.RED);
        s += " " + m + " " + attempt(() -> ((Map) m).put("x", 1)) + " " + attempt(() -> m.put(null, 1)) + " " + attempt(() -> new EnumMap<Boxes.Color, Integer>(new HashMap<>()));
        EnumSet<Boxes.Color> set = EnumSet.noneOf(Boxes.Color.class);
        set.add(colors[n % 3]);
        set.add(Boxes.Color.BLUE);
        s += " | " + set + " " + set.size() + " " + set.contains(Boxes.Color.GREEN) + " " + EnumSet.allOf(Boxes.Color.class) + " " + EnumSet.complementOf(set) + " "
                + EnumSet.of(Boxes.Color.BLUE, Boxes.Color.RED) + " " + EnumSet.range(Boxes.Color.GREEN, Boxes.Color.BLUE) + " " + EnumSet.copyOf(List.of(Boxes.Color.GREEN)) + " "
                + set.getClass().getName() + " " + set.equals(Set.of(Boxes.Color.BLUE, colors[n % 3])) + " " + attempt(() -> EnumSet.range(Boxes.Color.BLUE, Boxes.Color.RED)) + " "
                + attempt(() -> EnumSet.copyOf(new ArrayList<Boxes.Color>())) + " " + attempt(() -> ((Set) set).add("x")) + " " + attempt(() -> EnumSet.noneOf((Class) String.class));
        Iterator<Boxes.Color> it = set.iterator();
        it.next();
        it.remove();
        s += " " + set + " " + attempt(() -> {
            it.remove();
            return "removed";
        });
        EnumSet<Many> many = EnumSet.range(Many.values()[n], Many.M69);
        many.remove(Many.M68);
        many.add(Many.M1);
        s += " | " + many.size() + " " + many.contains(Many.M69) + " " + many.getClass().getName() + " " + EnumSet.complementOf(many) + " " + EnumSet.allOf(Many.class).size();
        return s;
    }

    static <T> String collect(Collection<T> from, Collection<T> into) {
        into.addAll(from);
        into.forEach(x -> {});
        return into.toString() + into.containsAll(from);
    }

    // ---- streams ----

    /** A key that counts how often it is hashed: distinct over a Set doesn't hash again. */
    static final class Counted {
        static int hashes;
        final int k;

        Counted(int k) {
            this.k = k;
        }

        @Override
        public int hashCode() {
            hashes++;
            return k;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Counted c && c.k == k;
        }
    }

    public static String streams(int n) {
        StringBuilder b = new StringBuilder();
        // One element after another through the stages; a short-circuit stops; sorted takes all in first.
        Stream.of(1, 2, 3, n).map(x -> {
            b.append("m").append(x);
            return x + 1;
        }).filter(x -> {
            b.append("f").append(x);
            return x % 2 == 0;
        }).forEach(x -> b.append("e").append(x));
        boolean any = Stream.of(3, 1, n, 2).peek(x -> b.append("p").append(x)).sorted().peek(x -> b.append("q").append(x)).anyMatch(x -> x == 2);
        b.append(" ").append(any).append(" ");
        // A size known without running the stages: count doesn't run them, toArray asks the generator first.
        long c1 = List.of(1, 2, n).stream().peek(x -> b.append("c").append(x)).count();
        long c2 = List.of(1, 2, n).stream().filter(x -> x > 1).peek(x -> b.append("d").append(x)).count();
        String[] a1 = Stream.of("a", "b").map(x -> {
            b.append("g").append(x);
            return x;
        }).toArray(k -> {
            b.append("<").append(k).append(">");
            return new String[k];
        });
        String[] a2 = Stream.of("a", "b", "c").filter(x -> !x.equals("b")).toArray(k -> {
            b.append("[").append(k).append("]");
            return new String[k];
        });
        b.append(" ").append(c1).append(c2).append(Arrays.toString(a1)).append(Arrays.toString(a2)).append(" ");
        b.append(attempt(() -> Arrays.toString(Stream.of("a", "b").toArray(Integer[]::new)))).append(" ");
        b.append(attempt(() -> Arrays.toString(Stream.of("a", "b").filter(x -> true).toArray(Integer[]::new)))).append(" ");
        b.append(attempt(() -> Arrays.toString(Stream.of("a", "b").filter(x -> true).toArray(k -> new String[1])))).append(" ");
        b.append(attempt(() -> Arrays.toString(Stream.of("a", "b").toArray(k -> new String[k + 1])))).append(" ");
        // The source is read when the terminal operation runs; a change while it runs is caught.
        List<Integer> late = new ArrayList<>(List.of(1));
        Stream<Integer> lateStream = late.stream();
        late.add(n);
        b.append(lateStream.toList()).append(" ");
        List<Integer> changing = new ArrayList<>(List.of(1, 2, 3));
        b.append(attempt(() -> {
            changing.stream().forEach(x -> {
                b.append(x);
                if (x == 1) changing.add(9);
            });
            return "ran";
        })).append(" ");
        List<Integer> changing2 = new ArrayList<>(List.of(1, 2, 3));
        b.append(attempt(() -> changing2.stream().anyMatch(x -> {
            b.append(x);
            if (x == 1) changing2.add(9);
            return false;
        }))).append(" ");
        Map<Integer, Integer> hm = new HashMap<>(Map.of(1, 1, 2, 2));
        b.append(attempt(() -> {
            hm.keySet().stream().forEach(k -> {
                b.append(k);
                if (k == 1) hm.put(3 + n, 3);
            });
            return "ran";
        })).append(" ");
        // Used twice, closed: the JDK's messages.
        Stream<Integer> once = Stream.of(1);
        once.count();
        b.append(attempt(() -> once.count())).append(" ");
        Stream<Integer> linked = Stream.of(1);
        linked.map(x -> x);
        b.append(attempt(() -> linked.map(x -> x))).append(" ");
        Stream<Integer> closed = Stream.of(1);
        Stream<Integer> after = closed.map(x -> x + n);
        closed.close();
        b.append(attempt(() -> after.toList())).append(" ").append(attempt(() -> Stream.of(1).map(null))).append(" ");
        // toList: nulls allowed, can't change.
        List<Integer> withNull = Stream.of(null, n).toList();
        b.append(withNull).append(withNull.contains(null)).append(withNull.indexOf(null)).append(withNull.hashCode()).append(withNull.equals(Arrays.asList(null, n)))
                .append(withNull.subList(0, 1).contains(null)).append(attempt(() -> List.copyOf(withNull))).append(attempt(() -> withNull.add(1))).append(" ");
        // Collectors.
        b.append(Stream.of(n, 3, 1, n).collect(Collectors.toList())).append(Stream.of(17, 5, n).collect(Collectors.toSet()))
                .append(Stream.of("a", "bb", "cc", "d").collect(Collectors.groupingBy(String::length))).append(Stream.of("a", "b", "c").collect(Collectors.joining(", ", "[", "]")))
                .append(Stream.of("x", "y").collect(Collectors.joining())).append(Stream.<String>empty().collect(Collectors.joining("-"))).append(" ");
        b.append(attempt(() -> Stream.of(1, n, 1).collect(Collectors.toMap(x -> x, x -> "v" + x)))).append(" ");
        b.append(attempt(() -> Stream.of(1).collect(Collectors.toMap(x -> x, x -> null)))).append(" ");
        Map<Integer, Integer> merged = Stream.of(3, 1, 3, n).collect(Collectors.toMap(x -> x, x -> 1, Integer::sum, LinkedHashMap::new));
        Map<Object, Integer> nullKey = Stream.of(n).collect(Collectors.toMap(x -> null, x -> 1));
        b.append(merged).append(nullKey)
                .append(Stream.of("a", "bb", "c").collect(Collectors.groupingBy(String::length, Collectors.counting())))
                .append(Stream.of("a", "bb", "c").collect(Collectors.groupingBy(String::length, Collectors.mapping(x -> x + x, Collectors.toList()))))
                .append(attempt(() -> Stream.of("a").collect(Collectors.groupingBy(x -> null)))).append(attempt(() -> Stream.of(1, null).collect(Collectors.toUnmodifiableList())))
                .append((Object) Stream.of(4, n).collect(Collectors.toCollection(ArrayList<Integer>::new))).append(" ");
        // The other operations.
        b.append(Stream.of(List.of(1, 2), List.of(3, n)).flatMap(List::stream).anyMatch(x -> {
            b.append("[").append(x).append("]");
            return x == 2;
        })).append(Stream.of(1, null, 1, null, n).distinct().toList()).append(Stream.of(3, 1, 3, n, 1).sorted().distinct().toList())
                .append(Stream.of(3, 1, n).sorted(Comparator.reverseOrder()).toList()).append(attempt(() -> Stream.of(new Object(), new Object()).sorted().toList()))
                .append(Stream.of(1, 2, 3).reduce(Integer::sum)).append(Stream.<Integer>empty().reduce(Integer::sum)).append(Stream.of(1, 2, n).reduce(10, (x, y) -> x + y, Integer::sum))
                .append(Stream.of(1, 2).reduce(0, Integer::sum)).append(attempt(() -> Stream.of(1, 2).reduce((x, y) -> null))).append(attempt(() -> Stream.of((Object) null).findFirst()))
                .append(Stream.of(4, n, 2).min(Comparator.naturalOrder())).append(Stream.of(4, n, 2).max(Comparator.naturalOrder())).append(Stream.of(n).findAny())
                .append(Stream.of().allMatch(x -> false)).append(Stream.of().anyMatch(x -> true)).append(Stream.of(1, 2).noneMatch(x -> x == n)).append(" ");
        b.append(Stream.concat(Stream.of(1), Stream.of(2, n)).toList()).append(Optional.of(n).stream().toList()).append(Optional.empty().stream().count())
                .append(Arrays.asList(5, n).stream().map(x -> x * 2).toList()).append(Collections.unmodifiableList(new ArrayList<>(List.of(n, 1))).stream().toList())
                .append(new LinkedHashSet<>(List.of(9, 1, n)).stream().toList()).append(new HashSet<>(List.of(17, 3, 5, n)).stream().toList())
                .append(new LinkedHashMap<>(Map.of(n, "z")).entrySet().stream().map(Map.Entry::getValue).toList()).append(new HashMap<>(Map.of("k", n)).values().stream().toList())
                .append(Arrays.stream(new Integer[] {3, n, 1}, 1, 3).toList()).append(" ");
        Stream.Builder<Integer> builder = Stream.builder();
        builder.add(1).add(n);
        Stream<Integer> built = builder.build();
        b.append(attempt(() -> builder.add(3))).append(built.toList()).append(" ");
        Iterator<Integer> it = Stream.of(1, 2, n + 3).filter(x -> x != 2).map(x -> {
            b.append("i").append(x);
            return x * 10;
        }).iterator();
        b.append(it.next()).append(it.hasNext()).append(it.next()).append(it.hasNext()).append(" ");
        Stream<Integer> withClose = Stream.of(1).onClose(() -> b.append("close1")).onClose(() -> b.append("close2"));
        withClose.close();
        withClose.close();
        b.append(" ").append(attempt(() -> {
            try (Stream<Integer> failing = Stream.of(1).onClose(() -> {
                throw new IllegalStateException("first");
            }).onClose(() -> {
                throw new IllegalArgumentException("second");
            })) {
                return failing.count();
            } catch (IllegalStateException e) {
                return e.getMessage() + e.getSuppressed().length + e.getSuppressed()[0].getMessage();
            }
        })).append(" ");
        Counted.hashes = 0;
        Set<Counted> counted = new HashSet<>(List.of(new Counted(1), new Counted(2), new Counted(n + 3)));
        int before = Counted.hashes;
        long distinctCount = counted.stream().distinct().count();
        long listDistinct = new ArrayList<>(counted).stream().distinct().count();
        b.append(distinctCount).append(Counted.hashes - before).append(listDistinct).append(" ");
        // Streams of ints and longs.
        b.append(IntStream.range(0, 4).mapToObj(i -> "i" + i).toList()).append(IntStream.range(3, 1).sum()).append(IntStream.range(0, n + 5).filter(i -> i % 2 == 0).max())
                .append(IntStream.range(0, 0).max()).append(IntStream.rangeClosed(1, 4).reduce(1, (x, y) -> x * y)).append(IntStream.of(3, n, 1).sorted().boxed().toList())
                .append(Arrays.toString(IntStream.of(n, 2).map(x -> x * 3).toArray())).append(IntStream.of(5).average()).append(IntStream.empty().average())
                .append(Stream.of("ab", "c").mapToInt(String::length).sum()).append(Stream.of("ab").mapToLong(String::length).sum()).append(" ");
        b.append(LongStream.of(1, 5, n).sum()).append(LongStream.of(1, 5, n).max()).append(Arrays.toString(LongStream.of(4, 2, n).filter(x -> x > 2).toArray()))
                .append(LongStream.of(7).boxed().toList()).append(LongStream.range(0, 3).count()).append(IntStream.range(0, 3).asLongStream().sum())
                .append(IntStream.of(1, 2, n).anyMatch(x -> x == n)).append(IntStream.of(2, 4).allMatch(x -> x % 2 == 0)).append(IntStream.of(n).findFirst())
                .append(Arrays.stream(new int[] {n, 1}).min()).append(Arrays.stream(new long[] {n, 1}).count());
        return b.toString();
    }

    // ---- UUID, atomics, references, System, Math's exact operations, exceptions ----

    public static String misc(int n) {
        StringBuilder b = new StringBuilder();
        java.util.UUID u = new java.util.UUID(0x123456789abcdef0L * (n + 1), -0x0fedcba987654321L * n);
        java.util.UUID v = new java.util.UUID(u.getMostSignificantBits(), u.getLeastSignificantBits());
        java.util.UUID r = java.util.UUID.randomUUID();
        b.append(u).append(" ").append(u.hashCode()).append(u.equals(v)).append(u.equals(r)).append(u.compareTo(new java.util.UUID(0, n))).append(u.version()).append(u.variant())
                .append(r.version()).append(r.variant()).append(r.toString().length()).append(" ");
        java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(n);
        b.append(ai.incrementAndGet()).append(ai.getAndAdd(5)).append(ai.addAndGet(Integer.MAX_VALUE)).append(ai.compareAndSet(3, 4)).append(ai.compareAndSet(ai.get(), 7))
                .append(ai.getAndSet(-1)).append(ai.updateAndGet(x -> x * 10)).append(ai.accumulateAndGet(n, (x, y) -> Math.max(x, y))).append(ai).append(ai.longValue()).append(" ");
        java.util.concurrent.atomic.AtomicLong al = new java.util.concurrent.atomic.AtomicLong(Long.MAX_VALUE - n);
        b.append(al.incrementAndGet()).append(al.getAndDecrement()).append(al.getAndUpdate(x -> x / 3)).append(al.compareAndSet(1, 2)).append(al).append(" ");
        java.util.concurrent.atomic.AtomicBoolean ab = new java.util.concurrent.atomic.AtomicBoolean(n % 2 == 0);
        b.append(ab.getAndSet(true)).append(ab.compareAndSet(false, true)).append(ab.compareAndSet(true, false)).append(ab).append(" ");
        String first = "first" + n;
        java.util.concurrent.atomic.AtomicReference<String> ar = new java.util.concurrent.atomic.AtomicReference<>(first);
        b.append(ar.compareAndSet(new String(first), "no")).append(ar.compareAndSet(first, "yes")).append(ar.getAndSet(null)).append(ar.updateAndGet(x -> x + "!")).append(ar).append(" ");
        Object held = new Object();
        java.lang.ref.WeakReference<Object> weak = new java.lang.ref.WeakReference<>(held);
        b.append(weak.get() == held).append(weak.refersTo(held)).append(weak.refersTo(null));
        weak.clear();
        b.append(weak.get()).append(weak.refersTo(null)).append(new java.lang.ref.SoftReference<>("s").get()).append(" ");
        long t1 = System.nanoTime();
        long t2 = System.nanoTime();
        b.append(System.identityHashCode(null)).append(System.identityHashCode(held) == held.hashCode()).append(System.currentTimeMillis() > 1_700_000_000_000L).append(t2 >= t1).append(" ");
        b.append(attempt(() -> Math.addExact(Integer.MAX_VALUE - 1, n))).append(attempt(() -> Math.multiplyExact(Long.MAX_VALUE / 2, (long) n))).append(attempt(() -> Math.toIntExact(1L << (n + 25))))
                .append(attempt(() -> Math.negateExact(Integer.MIN_VALUE + n))).append(attempt(() -> Math.multiplyExact(3_000_000_000L, n))).append(Math.toRadians(30.0 * n)).append(Math.toDegrees(n / 7.0))
                .append(attempt(() -> Math.subtractExact(-n, Long.MAX_VALUE))).append(attempt(() -> Math.incrementExact(Integer.MAX_VALUE - n % 2))).append(" ");
        try {
            if (n % 3 == 0) throw new java.io.IOException("io" + n);
            if (n % 3 == 1) throw new java.io.UncheckedIOException("unchecked", new java.io.IOException("inner" + n));
            throw new TypeNotPresentException("some.Type" + n, null);
        } catch (java.io.IOException e) {
            b.append("IOE ").append(e.getMessage());
        } catch (java.io.UncheckedIOException e) {
            b.append("UIOE ").append(e.getMessage()).append(e.getCause().getMessage());
        } catch (RuntimeException e) {
            b.append(e);
        }
        try {
            Object nothing = n > 100 ? held : null;
            b.append(nothing.hashCode());
        } catch (NullPointerException e) {
            e.addSuppressed(new IllegalStateException("later" + n));
            b.append(" ").append(e.getSuppressed().length).append(e.getSuppressed()[0].getMessage()).append(attempt(() -> {
                e.addSuppressed(e);
                return "self";
            }));
        }
        return b.toString();
    }

    // ---- pattern switches ----

    sealed interface Shape permits Circle, Square, Tri {}

    record Circle(int r) implements Shape {}

    record Square(int side) implements Shape {}

    record Tri(int a) implements Shape {}

    public static String patterns(int n) {
        Object[] things = {new Circle(n), new Square(n), "text" + n, n, null, new Tri(n), List.of(n), new int[n], new Circle(n + 5)};
        StringBuilder b = new StringBuilder();
        for (Object o : things) {
            String r = switch (o) {
                case null -> "null";
                case Circle c when c.r() > 5 -> "big circle";
                case Circle c -> "circle " + c.r();
                case Square(int side) -> "square " + side;
                case String s when s.length() > 5 -> "long " + s;
                case String s -> "short " + s;
                case Integer k -> "int " + (k + 1);
                case int[] arr -> "ints " + arr.length;
                case List<?> l -> "list " + l.size();
                default -> "other " + o.getClass().getSimpleName();
            };
            b.append(r).append("; ");
        }
        Shape shape = n % 3 == 0 ? new Circle(n) : n % 3 == 1 ? new Square(n) : new Tri(n);
        b.append(switch (shape) {
            case Circle c -> "C" + c.r();
            case Square q -> "S" + q.side();
            case Tri t -> "T" + t.a();
        });
        Object maybe = n % 2 == 0 ? null : "x";
        b.append(" ").append(attempt(() -> switch (maybe) {
            case String s -> s;
            default -> "d";
        }));
        return b.toString();
    }

    // ---- monitors ----

    static final class Counter {
        private int count;

        synchronized int bump(int by) {
            count += by;
            return by > 0 ? bump(by - 1) : count;
        }

        static synchronized String name(int n) {
            return "counter" + n;
        }
    }

    public static String monitors(int n) {
        Counter c = new Counter();
        StringBuilder b = new StringBuilder();
        b.append(c.bump(n)).append(Counter.name(n));
        synchronized (c) {
            synchronized (c) {
                b.append(c.bump(1));
            }
        }
        b.append(attempt(() -> {
            synchronized (c) {
                if (n % 2 == 0) throw new IllegalStateException("inside" + n);
                return "out";
            }
        }));
        synchronized (c) {
            b.append(c.bump(0));
        }
        return b.toString();
    }
}
