package com.example.numbers;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

    static <T> String collect(Collection<T> from, Collection<T> into) {
        into.addAll(from);
        into.forEach(x -> {});
        return into.toString() + into.containsAll(from);
    }
}
