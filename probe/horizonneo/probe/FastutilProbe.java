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
}
