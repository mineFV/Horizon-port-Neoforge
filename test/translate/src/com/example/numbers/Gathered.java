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
}
