package horizonneo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which missing pieces would let the most of a mod's methods be translated (`translate --blockers`): a method that
 * isn't translated is kept out by every reason of its own instructions (not only the first, which translation.md gives)
 * and by those of the methods it calls and the class initializers it runs, the mod's and the model's, followed through.
 * Each reason is a piece: a class of the game or of another library (what of it a method uses), or the reason itself.
 * A piece that is all a method lacks would bring it in alone.
 *
 * <p>An estimate, said so in what it writes: virtual calls aren't followed (what a call may reach is left out), nor the
 * objects a method makes dropping its makers (poisonable) or the fixed point's other drops, which are pieces of their
 * own ("translator: ...").
 */
final class Blockers {
    private Blockers() {}

    /** A qualified Java name after a colon: the thing a reason names. */
    private static final Pattern NAMED = Pattern.compile(": ([a-zA-Z_$][\\w$]*(?:\\.[\\w$<>]+)+)");

    /** The piece a reason names: a class (the member's owner), else the reason's kind. */
    static String piece(String why) {
        Matcher m = NAMED.matcher(why);
        if (!m.find()) return why.length() > 90 ? why.substring(0, 90) + "..." : why;
        String name = m.group(1);
        boolean isClass = why.contains("an object of") || why.contains("superclass") || why.contains("supertypes") || why.contains("catches")
                || why.contains("initializes") || why.contains("a lambda of");
        String owner = isClass ? name : name.substring(0, Math.max(name.lastIndexOf('.'), 0));
        String where = why.contains("the game") ? "game" : why.contains("another library or mod") ? "other library" : why.contains("Java's library")
                || why.contains("library model") ? "Java" : "mod";
        return where + ": " + owner;
    }

    /** Every untranslated method's pieces, its own and its dependencies', followed through (cycles included). */
    record Pieces(JavaWorld world, Map<JarRust.Id, Set<String>> of) {}

    static Pieces pieces(Map<String, ClassData> jar, Map<String, ClassData> model, Map<String, ClassData> outside, Map<JarRust.Id, String> translated,
            Map<JarRust.Id, String> dropped) {
        JavaWorld world = new JavaWorld(jar, model, outside);
        Map<JarRust.Id, Set<String>> direct = new HashMap<>();
        Map<JarRust.Id, List<JarRust.Id>> deps = new HashMap<>();
        for (ClassData c : world.classes.values()) {
            for (ClassData.Method m : c.methods) {
                if (m.body() == null) continue;
                JarRust.Id id = new JarRust.Id(c.name, m.name(), m.descriptor());
                if (translated.containsKey(id)) continue;
                List<String> all = new ArrayList<>();
                MethodRust.Check check;
                try {
                    check = MethodRust.check(c.name, m, c.pool, world, all);
                } catch (ClassData.Malformed | Bytecode.Invalid e) {
                    check = new MethodRust.Check("its bytecode doesn't decode: " + e.getMessage(), List.of());
                }
                if (all.isEmpty() && check.why() != null) all.add(check.why());
                // What the translation's own pass refused it for (an object of a class outside, a type test...), when it
                // isn't a method it needs: a piece of its own too.
                String own = dropped.get(id);
                if (all.isEmpty() && own != null && !own.contains(", which isn't translated") && !own.contains(", which may run ")) all.add(own);
                Set<String> pieces = new TreeSet<>();
                for (String w : all) pieces.add(piece(w));
                direct.put(id, pieces);
                List<JarRust.Id> d = new ArrayList<>();
                for (MethodRust.Need n : check.needs()) {
                    switch (n.kind()) {
                        case "call", "lambda" -> {
                            if (n.id() != null) d.add(n.id());
                        }
                        case "init" -> {
                            for (String k : world.initOrder(n.type())) d.add(new JarRust.Id(k, "<clinit>", "()V"));
                        }
                        default -> {
                        }
                    }
                }
                deps.put(id, d);
            }
        }
        // Each method's pieces, its own and its dependencies', to a fixed point (cycles included), as sets of piece numbers.
        List<String> pieceNames = new ArrayList<>();
        Map<String, Integer> pieceIndex = new HashMap<>();
        List<JarRust.Id> ids = new ArrayList<>(direct.keySet());
        Map<JarRust.Id, Integer> at = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) at.put(ids.get(i), i);
        java.util.BitSet[] sets = new java.util.BitSet[ids.size()];
        int[][] edges = new int[ids.size()][];
        for (int i = 0; i < ids.size(); i++) {
            sets[i] = new java.util.BitSet();
            for (String p : direct.get(ids.get(i))) sets[i].set(pieceIndex.computeIfAbsent(p, k -> {
                pieceNames.add(k);
                return pieceNames.size() - 1;
            }));
            edges[i] = deps.get(ids.get(i)).stream().filter(at::containsKey).mapToInt(at::get).toArray();
        }
        for (boolean changed = true; changed; ) {
            changed = false;
            for (int i = 0; i < ids.size(); i++) {
                int before = sets[i].cardinality();
                for (int e : edges[i]) sets[i].or(sets[e]);
                if (sets[i].cardinality() != before) changed = true;
            }
        }
        Map<JarRust.Id, Set<String>> of = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            Set<String> b = new TreeSet<>();
            for (int k = sets[i].nextSetBit(0); k >= 0; k = sets[i].nextSetBit(k + 1)) b.add(pieceNames.get(k));
            if (b.isEmpty()) b.add("translator: " + JarRust.kind(dropped.getOrDefault(ids.get(i), "dropped with what it needs")));
            of.put(ids.get(i), b);
        }
        return new Pieces(world, of);
    }

    /** What it writes: the pieces, by how many of the mod's methods each keeps out alone, and among others. */
    static String report(Map<String, ClassData> jar, Map<String, ClassData> model, Map<String, ClassData> outside, Map<JarRust.Id, String> translated,
            Map<JarRust.Id, String> dropped) {
        Pieces all = pieces(jar, model, outside, translated, dropped);
        JavaWorld world = all.world();
        List<JarRust.Id> ids = new ArrayList<>(all.of().keySet());
        Map<String, Integer> alone = new HashMap<>(), among = new HashMap<>(), pairs = new HashMap<>();
        Map<String, List<String>> examples = new HashMap<>();
        int[] sizes = new int[6];
        int counted = 0;
        for (int i = 0; i < ids.size(); i++) {
            JarRust.Id id = ids.get(i);
            if (!world.jar.contains(id.owner())) continue;
            Set<String> b = all.of().get(id);
            counted++;
            sizes[Math.min(b.size(), 5)]++;
            for (String p : b) among.merge(p, 1, Integer::sum);
            if (b.size() == 1) {
                alone.merge(b.iterator().next(), 1, Integer::sum);
                List<String> e = examples.computeIfAbsent(b.iterator().next(), k -> new ArrayList<>());
                if (e.size() < 3) e.add(id.owner().replace('/', '.') + "." + id.name());
            }
            if (b.size() == 2) pairs.merge(String.join(" + ", b), 1, Integer::sum);
        }
        StringBuilder out = new StringBuilder("# What keeps the mod's methods out, piece by piece\n\n");
        out.append("An estimate (`translate --blockers`, Blockers): every reason a method isn't translated, its own instructions' and those of the ")
                .append("methods it calls and the class initializers it runs, followed through. Virtual calls aren't followed, nor the drops of the ")
                .append("translation's fixed point (objects' makers, what a method needs that dropped later): those are \"translator: ...\" pieces.\n\n");
        out.append(counted).append(" of the mod's methods aren't translated. By how many pieces each lacks: ");
        for (int i = 1; i < 6; i++) out.append(i == 5 ? "5 or more: " : i + ": ").append(sizes[i]).append(i == 5 ? ".\n\n" : ", ");
        table(out, "Pieces that alone keep methods out (each would bring that many in)", "Alone", alone, 60, examples);
        table(out, "Pairs of pieces that together keep methods out", "Methods", pairs, 30, Map.of());
        table(out, "Pieces among what keeps methods out", "Among", among, 60, Map.of());
        return out.toString();
    }

    /**
     * `translate --blockers-for`: for the methods named (each `class.method`, dotted, every overload), whether each is
     * translated, and every piece that keeps it out, followed through what it calls; then the pieces by how many of
     * them each keeps out. What a machine needs (its constructor, tick, load and save) is asked this way.
     */
    static String forMethods(Map<String, ClassData> jar, Map<String, ClassData> model, Map<String, ClassData> outside, Map<JarRust.Id, String> translated,
            Map<JarRust.Id, String> dropped, List<String> names) {
        Pieces p = pieces(jar, model, outside, translated, dropped);
        StringBuilder out = new StringBuilder("# What keeps these methods out\n\n");
        out.append("Each method named (`translate --blockers-for`), and every piece that keeps it out: its own reasons and those of the methods it calls ")
                .append("and the class initializers it runs, followed through (an estimate: virtual calls aren't followed, see blockers.md).\n\n");
        Map<String, Integer> among = new HashMap<>();
        for (String name : names) {
            int dot = name.lastIndexOf('.');
            String owner = dot < 0 ? "" : name.substring(0, dot).replace('.', '/'), method = name.substring(dot + 1);
            List<JarRust.Id> found = new ArrayList<>();
            for (JarRust.Id id : translated.keySet()) if (id.owner().equals(owner) && id.name().equals(method)) found.add(id);
            for (JarRust.Id id : p.of().keySet()) if (id.owner().equals(owner) && id.name().equals(method)) found.add(id);
            if (found.isEmpty()) {
                out.append("## ").append(name).append("\n\nNo method of that name with a body.\n\n");
                continue;
            }
            found.sort(java.util.Comparator.comparing(JarRust.Id::descriptor));
            for (JarRust.Id id : found) {
                out.append("## ").append(name).append(id.descriptor()).append("\n\n");
                if (translated.containsKey(id)) {
                    out.append("Translated, as `").append(translated.get(id)).append("`.\n\n");
                    continue;
                }
                Set<String> b = p.of().get(id);
                for (String piece : b) {
                    out.append("- ").append(piece).append('\n');
                    among.merge(piece, 1, Integer::sum);
                }
                out.append('\n');
            }
        }
        table(out, "Pieces among what keeps them out", "Methods", among, 200, Map.of());
        return out.toString();
    }

    private static void table(StringBuilder out, String title, String column, Map<String, Integer> counts, int limit, Map<String, List<String>> examples) {
        out.append("## ").append(title).append("\n\n| Piece | ").append(column).append(examples.isEmpty() ? " |\n|---|---|\n" : " | For example |\n|---|---|---|\n");
        counts.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue() != 0 ? b.getValue() - a.getValue() : a.getKey().compareTo(b.getKey())).limit(limit)
                .forEach(e -> out.append("| ").append(e.getKey().replace("|", "\\|")).append(" | ").append(e.getValue())
                        .append(examples.isEmpty() ? "" : " | " + String.join(", ", examples.getOrDefault(e.getKey(), List.of()))).append(" |\n"));
        out.append('\n');
    }

}
