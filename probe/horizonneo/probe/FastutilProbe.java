package horizonneo.probe;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.function.Supplier;

/** fastutil's open hash set: its order after adds and removals, growing and shrinking, null, removal while iterating. */
final class FastutilProbe {
    private FastutilProbe() {}

    /** A key whose hash is chosen: collisions on purpose. */
    record Keyed(int hash, String name) {
        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static void run() {
        chars();
        longs();
        int[] counts = {0, 1, 5, 12, 13, 24, 25, 40, 100, 300};
        for (int n : counts) {
            p("set ints " + n, () -> {
                ObjectOpenHashSet<Object> s = new ObjectOpenHashSet<>();
                for (int i = 0; i < n; i++) s.add(i * 7 - 50);
                return s + " " + s.size() + " " + s.hashCode();
            });
            p("set strings " + n, () -> {
                ObjectOpenHashSet<String> s = new ObjectOpenHashSet<>();
                for (int i = 0; i < n; i++) s.add("k" + i);
                for (int i = 0; i < n; i += 3) s.remove("k" + i);
                return s + " " + s.size() + " " + s.contains("k1") + " " + s.contains("k0");
            });
            p("set shrink " + n, () -> {
                ObjectOpenHashSet<Integer> s = new ObjectOpenHashSet<>(4);
                for (int i = 0; i < n; i++) s.add(i);
                for (int i = 0; i < n - 2; i++) s.remove(i);
                s.add(1000);
                return s.toString();
            });
        }
        p("set collisions", () -> {
            ObjectOpenHashSet<Keyed> s = new ObjectOpenHashSet<>();
            for (int i = 0; i < 20; i++) s.add(new Keyed(i % 3 == 0 ? 42 : i * 31, "e" + i));
            String before = s.toString();
            s.remove(new Keyed(42, "e3"));
            s.remove(new Keyed(42, "e0"));
            return before + " | " + s;
        });
        p("set null", () -> {
            ObjectOpenHashSet<String> s = new ObjectOpenHashSet<>();
            s.add("a");
            boolean first = s.add(null), again = s.add(null);
            s.add("b");
            String with = s.toString();
            boolean removed = s.remove(null);
            return first + " " + again + " " + with + " " + removed + " " + s + " " + s.contains(null) + " " + s.size();
        });
        for (int every : new int[] {2, 3, 5}) {
            p("set iterator remove " + every, () -> {
                ObjectOpenHashSet<Keyed> s = new ObjectOpenHashSet<>();
                for (int i = 0; i < 40; i++) s.add(new Keyed(i % 4 == 0 ? 7 : i * 13, "e" + i));
                StringBuilder seen = new StringBuilder();
                int k = 0;
                for (ObjectIterator<Keyed> it = s.iterator(); it.hasNext();) {
                    Keyed e = it.next();
                    seen.append(e).append(' ');
                    if (k++ % every == 0) it.remove();
                }
                return seen + "| " + s + " " + s.size();
            });
        }
        p("set iterator errors", () -> {
            ObjectOpenHashSet<String> s = new ObjectOpenHashSet<>();
            s.add("x");
            ObjectIterator<String> it = s.iterator();
            try {
                it.remove();
                return "no";
            } catch (IllegalStateException e) {
                it.next();
                try {
                    it.next();
                    return "no2";
                } catch (java.util.NoSuchElementException f) {
                    return "both";
                }
            }
        });
        p("set misc", () -> {
            ObjectOpenHashSet<String> s = new ObjectOpenHashSet<>(java.util.List.of("q", "w", "e", "r"));
            String added = s.addOrGet("w") + " " + s.addOrGet("t") + " " + s.get("e") + " " + s.get("z");
            ObjectOpenHashSet<String> c = s.clone();
            c.add("y");
            s.trim();
            ObjectOpenHashSet<String> a = new ObjectOpenHashSet<>(new String[] {"m", "n", "m"});
            ObjectIterator<String> it = a.iterator();
            int skipped = it.skip(5);
            return added + " " + s + " " + c + " " + a + " " + skipped + " " + s.equals(new java.util.HashSet<>(s)) + " " + s.isEmpty();
        });
        maps();
        references();
        arrayMaps();
        p("set bad", () -> new ObjectOpenHashSet<String>(4, 1.5f));
        p("set bad size", () -> new ObjectOpenHashSet<String>(-1));
        p("set big", () -> {
            ObjectOpenHashSet<Long> s = new ObjectOpenHashSet<>();
            for (long i = 0; i < 2000; i++) s.add(i * i);
            for (long i = 0; i < 1990; i++) s.remove(i * i);
            StringBuilder b = new StringBuilder();
            s.forEach(x -> b.append(x).append(','));
            return b + " " + s;
        });
    }

    static void maps() {
        int[] counts = {0, 1, 13, 25, 100};
        for (int n : counts) {
            p("map ints " + n, () -> {
                it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<Object, String> m = new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>();
                for (int i = 0; i < n; i++) m.put(i * 11 - 30, "v" + i);
                for (int i = 0; i < n; i += 4) m.remove(i * 11 - 30);
                return m + " " + m.size() + " " + m.hashCode() + " " + m.keySet() + " " + m.values() + " " + m.get(-30) + " " + m.get(-19) + " " + m.containsValue("v3");
            });
        }
        p("map null and defaults", () -> {
            it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<String, String> m = new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>();
            m.defaultReturnValue("none");
            StringBuilder b = new StringBuilder();
            b.append(m.put("a", "1")).append(' ').append(m.put(null, "n")).append(' ').append(m.put("a", "2")).append(' ').append(m.get("z")).append(' ').append(m.get(null));
            b.append(' ').append(m.getOrDefault("z", "d")).append(' ').append(m.putIfAbsent("a", "3")).append(' ').append(m.putIfAbsent("b", "4")).append(' ').append(m);
            b.append(' ').append(m.remove("q")).append(' ').append(m.remove(null)).append(' ').append(m.remove("b", "x")).append(' ').append(m.replace("a", "2", "5")).append(' ').append(m.replace("c", "6"));
            b.append(' ').append(m.computeIfAbsent("c", k -> k + "!")).append(' ').append(m.computeIfPresent("a", (k, v) -> null)).append(' ').append(m.compute("d", (k, v) -> k + v));
            b.append(' ').append(m.merge("d", "+", String::concat)).append(' ').append(m.merge("e", "e", String::concat)).append(' ').append(m).append(' ').append(m.defaultReturnValue());
            return b;
        });
        p("map entries", () -> {
            it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<Keyed, Integer> m = new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>();
            for (int i = 0; i < 30; i++) m.put(new Keyed(i % 5 == 0 ? 3 : i * 17, "k" + i), i);
            StringBuilder b = new StringBuilder();
            for (var e : m.object2ObjectEntrySet()) b.append(e).append(' ');
            int k = 0;
            for (ObjectIterator<it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry<Keyed, Integer>> it = m.object2ObjectEntrySet().iterator(); it.hasNext();) {
                var e = it.next();
                if (k++ % 3 == 0) it.remove();
                else e.setValue(e.getValue() * 10);
            }
            m.keySet().remove(new Keyed(34, "k2"));
            return b + "| " + m + " " + m.size() + " " + m.equals(new java.util.HashMap<>(m)) + " " + new java.util.HashMap<>(m).equals(m);
        });
        p("map from", () -> {
            java.util.Map<String, Integer> src = new java.util.TreeMap<>(java.util.Map.of("x", 1, "y", 2, "z", 3));
            it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<String, Integer> m = new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>(src);
            it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<String, Integer> c = m.clone();
            c.put("w", 0);
            StringBuilder b = new StringBuilder();
            m.forEach((k, v) -> b.append(k).append(v));
            return m + " " + c + " " + b + " " + new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>(new String[] {"p", "q"}, new Integer[] {7, 8});
        });
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }

    /**
     * A key whose hash is another object's identity hash, equal only to a key of the same object: the reference map's
     * keys as the object map sees them. Identity hashes change from run to run, so the reference map is observed by
     * what it shares with the object map on these keys (orders, texts, hashes), and by what no hash decides.
     */
    record Same(Object o) {
        static Same of(Object o) {
            return new Same(o);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(o);
        }

        @Override
        public boolean equals(Object x) {
            return x instanceof Same s && s.o == o;
        }

        @Override
        public String toString() {
            return String.valueOf(o);
        }
    }

    /** A subclass that reads the arrays, as Mekanism's does (CustomObjectToObjectArrayMap): its capacity, its own forEach. */
    static final class Peeked extends it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap<Object, String> {
        Peeked() {}

        Peeked(java.util.Map<Object, String> m) {
            super(m);
        }

        int capacity() {
            return key.length;
        }

        String direct() {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < size; i++) b.append(key[i]).append('=').append(value[i]).append(';');
            return b.toString();
        }

        @Override
        public it.unimi.dsi.fastutil.objects.ObjectSet<Object> keySet() {
            return size == 0 ? it.unimi.dsi.fastutil.objects.ObjectSets.emptySet() : super.keySet();
        }
    }

    /** What running call gives, or the class and message of what it throws (a NullPointerException's message isn't compared). */
    static String t(Supplier<Object> call) {
        try {
            return String.valueOf(call.get());
        } catch (NullPointerException e) {
            return e.getClass().getName();
        } catch (RuntimeException e) {
            return e.getClass().getName() + ": " + e.getMessage();
        }
    }

    /** fastutil's array map: its order, removals through the map, its views and their iterators, its arrays; the empty set. */
    static void arrayMaps() {
        p("array map made", () -> {
            Peeked p = new Peeked();
            StringBuilder b = new StringBuilder().append(p.capacity()).append(' ');
            for (int i = 0; i < 9; i++) {
                p.put("k" + i, "v" + i);
                b.append(p.capacity()).append(',');
            }
            Peeked q = new Peeked(new java.util.TreeMap<>(java.util.Map.of("b", "2", "a", "1", "c", "3")));
            b.append(' ').append(q).append(q.capacity()).append(' ').append(new it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap<Object, String>(5)).append(' ')
                    .append(new it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap<Object, String>(new Object[] {"x", "y"}, new Object[] {"1", "2"})).append(' ')
                    .append(new it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap<Object, String>(new Object[] {"x", "y", "z"}, new Object[] {"1", "2", "3"}, 2)).append(' ')
                    .append(t(() -> new it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap<Object, String>(new Object[] {"x"}, new Object[] {"1", "2"}))).append(' ')
                    .append(t(() -> new it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap<Object, String>(new Object[] {"x"}, new Object[] {"1"}, 2))).append(' ')
                    .append(t(() -> new it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap<Object, String>(-1)));
            return b;
        });
        p("array map ops", () -> {
            Peeked m = new Peeked();
            m.defaultReturnValue("none");
            StringBuilder b = new StringBuilder();
            b.append(m.put("a", "1")).append(m.put(null, "n")).append(m.put("b", null)).append(m.put(new String("a"), "2")).append(' ').append(m).append(' ');
            b.append(m.get("a")).append(m.get(null)).append(m.get("b")).append(m.get("z")).append(m.containsKey("b")).append(m.containsKey("z")).append(m.containsValue(null)).append(m.containsValue("2"));
            b.append(' ').append(m.remove("a")).append(m.remove("a")).append(' ').append(m).append(m.size()).append(m.direct()).append(' ').append(m.getOrDefault("z", "d")).append(m.putIfAbsent("c", "3"));
            b.append(' ').append(m.hashCode()).append(' ').append(m.equals(new java.util.HashMap<>(m))).append(new java.util.HashMap<>(m).equals(m)).append(' ').append(m.keySet() == m.keySet()).append(m.values() == m.values())
                    .append(m.object2ObjectEntrySet() == m.object2ObjectEntrySet()).append(' ').append(m.keySet()).append(m.values()).append(m.object2ObjectEntrySet());
            m.clear();
            b.append(' ').append(m).append(m.size()).append(m.isEmpty()).append(m.capacity()).append(m.keySet()).append(m.keySet().getClass().getName());
            return b;
        });
        p("array map views", () -> {
            Peeked m = new Peeked();
            for (int i = 0; i < 12; i++) m.put("k" + i, "v" + i);
            StringBuilder b = new StringBuilder();
            var entries = m.object2ObjectEntrySet().iterator();
            int k = 0;
            while (entries.hasNext()) {
                var e = entries.next();
                if (k++ % 3 == 0) entries.remove();
                b.append(e.getKey());
            }
            b.append(' ').append(t(() -> {
                var it = m.object2ObjectEntrySet().iterator();
                it.next();
                it.remove();
                it.remove();
                return "no";
            })).append(' ').append(m).append(' ');
            var keys = m.keySet().iterator();
            keys.next();
            keys.next();
            keys.remove();
            keys.remove();
            b.append(m.keySet()).append(' ').append(t(() -> {
                m.keySet().iterator().remove();
                return "no";
            })).append(' ');
            var values = m.values().iterator();
            values.next();
            values.remove();
            b.append(m.values()).append(' ').append(m.keySet().remove("k5")).append(m.keySet().remove("k5")).append(m.object2ObjectEntrySet().remove(java.util.Map.entry("k7", "v7")))
                    .append(m.object2ObjectEntrySet().remove(java.util.Map.entry("k8", "x"))).append(m.object2ObjectEntrySet().contains(java.util.Map.entry("k8", "v8")))
                    .append(m.object2ObjectEntrySet().contains("k8")).append(m.keySet().contains("k8")).append(m.values().contains("v8")).append(' ').append(m).append(' ');
            var fast = m.object2ObjectEntrySet().fastIterator();
            var first = fast.next();
            var second = fast.next();
            b.append(first == second).append(first).append(' ');
            StringBuilder each = new StringBuilder();
            m.object2ObjectEntrySet().fastForEach(e -> each.append(e.getKey()).append(e.getValue()));
            m.object2ObjectEntrySet().forEach(e -> each.append(e));
            m.keySet().forEach(each::append);
            m.values().forEach(each::append);
            m.forEach((x, y) -> each.append(x).append(y));
            b.append(each).append(' ').append(m.keySet().hashCode()).append(m.keySet().equals(java.util.Set.copyOf(m.keySet()))).append(' ').append(m.direct());
            var plain = new it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap<Object, String>(m);
            var c = plain.clone();
            c.put("new", "n");
            b.append(' ').append(plain.size()).append(c.size()).append(c.getClass().getName()).append(c.keySet() != plain.keySet()).append(c.equals(plain));
            return b;
        });
        p("array map empty set", () -> {
            Peeked m = new Peeked();
            var s = m.keySet();
            var i = s.iterator();
            StringBuilder b = new StringBuilder();
            b.append(s).append(s.size()).append(s.isEmpty()).append(s.contains("a")).append(s.contains(null)).append(s.hashCode()).append(' ').append(s.equals(java.util.Set.of())).append(s.equals(java.util.List.of()))
                    .append(s.equals(new java.util.HashSet<>())).append(new java.util.HashSet<>().equals(s)).append(s.equals(java.util.Set.of(1))).append(' ').append(i.hasNext()).append(' ').append(t(() -> i.next()))
                    .append(' ').append(t(() -> {
                        i.remove();
                        return "no";
                    })).append(' ').append(s.toArray() == s.toArray()).append(s.toArray().length);
            Object[] arr = {"x", "y"};
            b.append(' ').append(s.toArray(arr) == arr).append(arr[0]).append(arr[1]).append(' ').append(s.containsAll(java.util.List.of())).append(s.containsAll(java.util.List.of(1)))
                    .append(' ').append(t(() -> s.add("q"))).append(' ').append(t(() -> s.remove("q"))).append(' ').append(t(() -> s.addAll(java.util.List.of("q")))).append(' ')
                    .append(t(() -> s.removeAll(java.util.List.of()))).append(' ').append(t(() -> s.removeIf(null))).append(' ').append(s.removeIf(x -> true)).append(' ').append(s.stream().count());
            s.clear();
            b.append(' ').append(s == it.unimi.dsi.fastutil.objects.ObjectSets.emptySet()).append(s.getClass().getName()).append(i.getClass().getName());
            var e = it.unimi.dsi.fastutil.objects.ObjectIterators.EMPTY_ITERATOR;
            b.append(' ').append(e.hasPrevious()).append(e.nextIndex()).append(e.previousIndex()).append(e.skip(3)).append(e.back(3)).append(' ').append(t(() -> e.previous())).append(' ')
                    .append(t(() -> {
                        e.set("x");
                        return "no";
                    })).append(' ').append(e.clone() == e).append(it.unimi.dsi.fastutil.objects.ObjectIterators.emptyIterator() == e);
            return b;
        });
    }

    /** The object map's key for a reference map's: null as it is. */
    static Object same(Object k) {
        return k == null ? null : Same.of(k);
    }

    /** Whether the two maps read the same: text, order of keys, values and entries, hash, size. */
    static String alike(it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap<Object, String> r, it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<Object, String> o) {
        StringBuilder rk = new StringBuilder(), ok = new StringBuilder(), re = new StringBuilder(), oe = new StringBuilder();
        r.forEach((k, v) -> rk.append(k).append('|'));
        o.forEach((k, v) -> ok.append(k).append('|'));
        for (var e : r.reference2ObjectEntrySet()) re.append(e).append(e.hashCode() == (System.identityHashCode(e.getKey()) ^ java.util.Objects.hashCode(e.getValue()))).append('|');
        for (var e : o.object2ObjectEntrySet()) oe.append(e).append(true).append('|');
        return r.toString().equals(o.toString()) + " " + r.keySet().toString().equals(o.keySet().toString()) + " " + r.values().toString().equals(o.values().toString()) + " "
                + rk.toString().equals(ok.toString()) + " " + re.toString().equals(oe.toString()) + " " + (r.hashCode() == o.hashCode()) + " " + (r.keySet().hashCode() == o.keySet().hashCode()) + " "
                + r.size() + " " + o.size();
    }

    /** fastutil's map by identity: orders, growing and shrinking, removals while iterating, null, defaults, views. */
    static void references() {
        for (int n : new int[] {0, 1, 5, 13, 25, 100, 300}) {
            p("reference map " + n, () -> {
                it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap<Object, String> r = new it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap<>();
                it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<Object, String> o = new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>();
                Object[] keys = new Object[n];
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < n; i++) {
                    keys[i] = new String("k" + i % 7);
                    b.append(r.put(keys[i], "v" + i)).append(o.put(same(keys[i]), "v" + i));
                }
                b.append(' ').append(alike(r, o));
                for (int i = 0; i < n; i += 3) b.append(r.remove(keys[i])).append(o.remove(same(keys[i])));
                b.append(' ').append(alike(r, o)).append(' ').append(r.get(new String("k1"))).append(r.containsKey(new String("k1"))).append(r.containsValue("v1"));
                if (n > 2) b.append(r.get(keys[2])).append(r.containsKey(keys[2])).append(r.keySet().contains(keys[2])).append(r.keySet().contains(new String("k2")));
                return b;
            });
        }
        p("reference map shrink", () -> {
            it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap<Object, String> r = new it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap<>(4);
            it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<Object, String> o = new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>(4);
            Object[] keys = new Object[200];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = new Object();
                r.put(keys[i], "v" + i);
                o.put(same(keys[i]), "v" + i);
            }
            String grown = alike(r, o);
            for (int i = 0; i < keys.length - 3; i++) {
                r.remove(keys[i]);
                o.remove(same(keys[i]));
            }
            String shrunk = alike(r, o);
            r.trim();
            o.trim();
            return grown + " | " + shrunk + " | " + alike(r, o);
        });
        p("reference map null and defaults", () -> {
            it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap<Object, String> m = new it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap<>();
            Object a = new String("a"), b2 = new String("b"), c = new String("c"), d = new String("d"), e = new String("e");
            m.defaultReturnValue("none");
            StringBuilder b = new StringBuilder();
            b.append(m.put(a, "1")).append(' ').append(m.put(null, "n")).append(' ').append(m.put(a, "2")).append(' ').append(m.get(new String("a"))).append(' ').append(m.get(null));
            b.append(' ').append(m.getOrDefault(new String("a"), "d")).append(' ').append(m.putIfAbsent(a, "3")).append(' ').append(m.putIfAbsent(b2, "4")).append(' ').append(m.size());
            b.append(' ').append(m.remove(new String("b"))).append(' ').append(m.remove(null)).append(' ').append(m.remove(b2, "x")).append(' ').append(m.replace(a, "2", "5")).append(' ').append(m.replace(c, "6"));
            b.append(' ').append(m.computeIfAbsent(c, k -> k + "!")).append(' ').append(m.computeIfPresent(a, (k, v) -> null)).append(' ').append(m.compute(d, (k, v) -> k + v));
            b.append(' ').append(m.merge(d, "+", String::concat)).append(' ').append(m.merge(e, "e", String::concat)).append(' ').append(m.size()).append(' ').append(m.defaultReturnValue());
            b.append(' ').append(m.get(c)).append(m.get(d)).append(m.get(e)).append(m.get(a)).append(' ').append(m.containsKey(null)).append(m.containsValue("b"));
            return b;
        });
        p("reference map views", () -> {
            it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap<Object, String> r = new it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap<>();
            it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<Object, String> o = new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>();
            Object[] keys = new Object[40];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = i % 10 == 0 ? null : new String("k" + i % 9);
                r.put(keys[i], "v" + i);
                o.put(same(keys[i]), "v" + i);
            }
            StringBuilder b = new StringBuilder(alike(r, o));
            StringBuilder rs = new StringBuilder(), os = new StringBuilder();
            int k = 0;
            for (var it = r.reference2ObjectEntrySet().iterator(); it.hasNext();) {
                var e = it.next();
                rs.append(e).append(',');
                if (k++ % 3 == 0) it.remove();
                else e.setValue(e.getValue() + "*");
            }
            k = 0;
            for (var it = o.object2ObjectEntrySet().iterator(); it.hasNext();) {
                var e = it.next();
                os.append(e).append(',');
                if (k++ % 3 == 0) it.remove();
                else e.setValue(e.getValue() + "*");
            }
            b.append(' ').append(rs.toString().equals(os.toString())).append(' ').append(alike(r, o));
            rs.setLength(0);
            os.setLength(0);
            k = 0;
            for (var it = r.keySet().iterator(); it.hasNext();) {
                rs.append(it.next()).append(',');
                if (k++ % 2 == 1) it.remove();
            }
            k = 0;
            for (var it = o.keySet().iterator(); it.hasNext();) {
                os.append(it.next()).append(',');
                if (k++ % 2 == 1) it.remove();
            }
            b.append(' ').append(rs.toString().equals(os.toString())).append(' ').append(alike(r, o));
            // Which entries are left depends on the order, so on the identity hashes: only what they share is asked.
            java.util.IdentityHashMap<Object, String> id = new java.util.IdentityHashMap<>(r);
            int sum = 0;
            for (var e : id.entrySet()) sum += System.identityHashCode(e.getKey()) ^ java.util.Objects.hashCode(e.getValue());
            b.append(' ').append(r.equals(id)).append(id.equals(r)).append(r.hashCode() == sum).append(r.equals(new java.util.HashMap<>(java.util.Map.of(new String("k1"), "x"))));
            b.append(' ').append(r.keySet().equals(id.keySet())).append(r.keySet().equals(java.util.Set.of(new String("k1")))).append(r.values().contains(r.values().iterator().next()))
                    .append(r.values().contains("absent")).append(r.isEmpty());
            var first = r.reference2ObjectEntrySet().iterator().next();
            var basic = new it.unimi.dsi.fastutil.objects.AbstractReference2ObjectMap.BasicEntry<Object, String>(first.getKey(), first.getValue());
            b.append(' ').append(basic.equals(first)).append(first.equals(basic)).append(basic.equals(java.util.Map.entry(new String(String.valueOf(first.getKey())), first.getValue())));
            b.append(basic.hashCode() == first.hashCode()).append(' ').append(basic.toString().equals(first.getKey() + "->" + first.getValue()));
            var c = r.clone();
            c.put(new Object(), "new");
            b.append(' ').append(r.size()).append(' ').append(c.size()).append(' ').append(c.containsKey(first.getKey())).append(' ').append(r.reference2ObjectEntrySet().contains(java.util.Map.entry(new String("x"), "y")));
            return b;
        });
    }

    /** fastutil's char sets and maps: orders, '\\0', growth, removals, texts, hashes, boxed methods. */
    static void chars() {
        char[][] inputs = {{}, {'a'}, {'\0', 'b', 'a', 'b'}, "the quick brown fox jumps over a lazy dog".toCharArray(), {'\uffff', '\u0100', '\0', 'z', '\u7fff'}};
        for (int n = 0; n < inputs.length; n++) {
            char[] in = inputs[n];
            p("char set " + n, () -> {
                it.unimi.dsi.fastutil.chars.CharOpenHashSet s = new it.unimi.dsi.fastutil.chars.CharOpenHashSet(in);
                StringBuilder b = new StringBuilder().append(s).append(' ').append(s.size()).append(' ').append(s.hashCode()).append(' ').append(s.contains('a'))
                        .append(s.contains('\0')).append(s.contains((Object) 'b')).append(s.contains((Object) null)).append(' ');
                s.remove('a');
                s.rem('o');
                s.add('Q');
                b.append(s).append(' ').append(s.add('Q')).append(' ').append(s.remove((Object) 'Q')).append(' ').append(s);
                java.util.Set<Character> boxed = new java.util.HashSet<>();
                for (char c : in) boxed.add(c);
                b.append(' ').append(new it.unimi.dsi.fastutil.chars.CharOpenHashSet(in).equals(boxed)).append(' ').append(boxed.equals(new it.unimi.dsi.fastutil.chars.CharOpenHashSet(in)));
                StringBuilder each = new StringBuilder();
                for (Character c : new it.unimi.dsi.fastutil.chars.CharOpenHashSet(in)) each.append(c);
                return b.append(' ').append(each);
            });
            p("char map " + n, () -> {
                it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap<String> m = new it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap<>(1);
                for (int i = 0; i < in.length; i++) m.put(in[i], "v" + i);
                StringBuilder b = new StringBuilder().append(m).append(' ').append(m.size()).append(' ').append(m.hashCode()).append(' ').append(m.get('a')).append(' ')
                        .append(m.get('#')).append(' ').append(m.containsKey('b')).append(' ').append(m.keySet()).append(' ').append(m.values()).append(' ').append(m.keySet().contains('\0'))
                        .append(' ').append(m.keySet().hashCode()).append(' ').append(m.get((Object) 'a')).append(' ').append(m.get((Object) '#')).append(' ').append(m.get((Object) null))
                        .append(' ').append(m.getOrDefault('#', "d")).append(' ').append(m.getOrDefault((Object) 'a', "d")).append(' ').append(m.put((Character) 'a', "x")).append(' ')
                        .append(m.put((Character) '%', "y")).append(' ').append(m.remove('b')).append(' ').append(m.remove('#')).append(' ').append(m).append(' ').append(m.containsValue("x"))
                        .append(' ').append(m.char2ObjectEntrySet()).append(' ').append(m.entrySet().iterator().hasNext() ? m.entrySet().iterator().next().getKey() : null);
                m.defaultReturnValue("none");
                b.append(' ').append(m.get('#')).append(' ').append(m.get((Object) '#')).append(' ').append(m.getOrDefault('#', "d")).append(' ').append(m.apply('%'));
                java.util.Map<Character, String> h = new java.util.HashMap<>(m);
                return b.append(' ').append(m.equals(h)).append(' ').append(h.equals(m)).append(' ').append(m.hashCode() == h.hashCode());
            });
        }
        p("char map apply", () -> {
            try {
                return new it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap<String>().apply(70000);
            } catch (IllegalArgumentException e) {
                return e.getMessage();
            }
        });
        p("char set all", () -> {
            it.unimi.dsi.fastutil.chars.CharOpenHashSet s = new it.unimi.dsi.fastutil.chars.CharOpenHashSet(new char[] {'x'});
            java.util.List<Character> more = new java.util.ArrayList<>();
            for (char c = 'a'; c < 'a' + 20; c++) more.add(c);
            boolean changed = s.addAll(more);
            it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap<String> m = new it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap<>();
            m.put('a', "1");
            m.put('b', "2");
            boolean removed = m.keySet().remove('a');
            it.unimi.dsi.fastutil.chars.CharOpenHashSet copy = s.clone();
            copy.removeAll(java.util.List.of('a', 'b', 'x'));
            copy.retainAll(java.util.List.of('c', 'd', 'e', 'q'));
            it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap<String> mc = new it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap<>();
            mc.put('k', "v");
            it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap<String> mcc = mc.clone();
            mcc.put('l', "w");
            boolean gone = mcc.char2ObjectEntrySet().remove(java.util.Map.entry('k', "v")), kept = mcc.char2ObjectEntrySet().remove(java.util.Map.entry('l', "x"));
            return changed + " " + s + " " + copy + " " + mc + " " + mcc + " " + gone + " " + kept + " " + s.containsAll(more) + " " + removed + " " + m + " " + m.char2ObjectEntrySet().contains(java.util.Map.entry('b', "2"))
                    + " " + m.char2ObjectEntrySet().contains(java.util.Map.entry('b', "3")) + " " + m.char2ObjectEntrySet().contains("b");
        });
        for (int size : new int[] {3, 11, 12, 40, 200}) {
            p("char set iterate remove " + size, () -> {
                it.unimi.dsi.fastutil.chars.CharOpenHashSet s = new it.unimi.dsi.fastutil.chars.CharOpenHashSet();
                for (int i = 0; i < size; i++) s.add((char) (i * 4099 + 1));
                if (size > 5) s.add('\0');
                StringBuilder seen = new StringBuilder();
                it.unimi.dsi.fastutil.chars.CharIterator i = s.iterator();
                while (i.hasNext()) {
                    char c = i.nextChar();
                    seen.append((int) c).append(',');
                    if (c % 3 != 1) i.remove();
                }
                StringBuilder left = new StringBuilder();
                for (it.unimi.dsi.fastutil.chars.CharIterator j = s.iterator(); j.hasNext();) left.append((int) j.nextChar()).append(',');
                return s.size() + " " + seen + " | " + left;
            });
        }
        p("char set grow", () -> {
            it.unimi.dsi.fastutil.chars.CharOpenHashSet s = new it.unimi.dsi.fastutil.chars.CharOpenHashSet();
            for (int i = 0; i < 300; i += 7) s.add((char) (i * 131));
            for (int i = 0; i < 300; i += 21) s.remove((char) (i * 131));
            return s + " " + s.size() + " " + s.hashCode();
        });
    }

    /** fastutil's long sets and maps: orders, 0, growth, removals while iterating, texts, hashes, boxed methods. */
    static void longs() {
        long[][] inputs = {{}, {7}, {0, 5, -5, 5}, {Long.MIN_VALUE, Long.MAX_VALUE, 0, 1, -1, 1L << 40, -(1L << 33)}, java.util.stream.LongStream.range(0, 40).map(i -> i * 0x9E3779B97F4A7C15L).toArray()};
        for (int n = 0; n < inputs.length; n++) {
            long[] in = inputs[n];
            p("long set " + n, () -> {
                it.unimi.dsi.fastutil.longs.LongOpenHashSet s = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
                for (long v : in) s.add(v);
                StringBuilder b = new StringBuilder().append(s).append(' ').append(s.size()).append(' ').append(s.hashCode()).append(' ').append(s.contains(5)).append(s.contains(0))
                        .append(s.contains((Object) 7L)).append(s.contains((Object) null)).append(' ').append(java.util.Arrays.toString(s.toLongArray())).append(' ');
                s.remove(5);
                s.add(99);
                b.append(s).append(' ').append(s.add(99)).append(' ').append(s.remove((Object) 99L)).append(' ').append(s);
                java.util.Set<Long> boxed = new java.util.HashSet<>();
                for (long v : in) boxed.add(v);
                it.unimi.dsi.fastutil.longs.LongOpenHashSet again = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(4);
                again.addAll(boxed);
                b.append(' ').append(again).append(' ').append(again.equals(boxed)).append(' ').append(boxed.equals(again));
                it.unimi.dsi.fastutil.longs.LongIterator i = again.iterator();
                StringBuilder seen = new StringBuilder();
                while (i.hasNext()) {
                    long v = i.nextLong();
                    seen.append(v).append(',');
                    if ((v & 3) != 0) i.remove();
                }
                return b.append(' ').append(seen).append(' ').append(again).append(' ').append(again.size());
            });
            p("long map " + n, () -> {
                it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<String> m = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
                for (int i = 0; i < in.length; i++) m.put(in[i], "v" + i);
                StringBuilder b = new StringBuilder().append(m).append(' ').append(m.size()).append(' ').append(m.hashCode()).append(' ').append(m.get(5)).append(' ').append(m.get(12345))
                        .append(' ').append(m.containsKey(0)).append(' ').append(m.keySet()).append(' ').append(m.values()).append(' ').append(m.keySet().hashCode()).append(' ')
                        .append(m.get((Object) 7L)).append(' ').append(m.get((Object) null)).append(' ').append(m.getOrDefault(12345, "d")).append(' ').append(m.put((Long) 7L, "x"))
                        .append(' ').append(m.putIfAbsent(8, "p")).append(' ').append(m.putIfAbsent(8, "q")).append(' ').append(m.remove(5)).append(' ').append(m.remove(12345))
                        .append(' ').append(m.computeIfAbsent(9, k -> "c" + k)).append(' ').append(m.computeIfAbsent(9L, (java.util.function.LongFunction<String>) k -> "z"))
                        .append(' ').append(m.merge(8, "m", (a, c) -> a + c)).append(' ').append(m.compute(10, (k, v) -> v == null ? "n" : null)).append(' ').append(m);
                java.util.Map<Long, String> h = new java.util.HashMap<>(m);
                b.append(' ').append(m.equals(h)).append(' ').append(h.equals(m)).append(' ').append(m.hashCode() == h.hashCode());
                it.unimi.dsi.fastutil.objects.ObjectIterator<it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<String>> fast = it.unimi.dsi.fastutil.longs.Long2ObjectMaps.fastIterator(m);
                StringBuilder walk = new StringBuilder();
                while (fast.hasNext()) {
                    it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<String> e = fast.next();
                    walk.append(e.getLongKey()).append('=').append(e.getValue()).append(',');
                }
                for (java.util.Iterator<it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<String>> i = m.long2ObjectEntrySet().iterator(); i.hasNext();) {
                    it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<String> e = i.next();
                    if ((e.getLongKey() & 1) == 0) i.remove();
                }
                java.util.Iterator<it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<String>> first = m.long2ObjectEntrySet().iterator();
                String entry = first.hasNext() ? first.next() + " " + first.hasNext() : "none";
                return b.append(' ').append(walk).append(' ').append(m).append(' ').append(m.size()).append(' ').append(entry).append(' ').append(m.keySet().contains(7));
            });
        }
    }
}
