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
}
