package horizonneo;

import horizonneo.MethodRust.Call;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The translator over a whole jar: every method of the mod's own classes is checked (`MethodRust.check`), a
 * method that calls one of the mod's that isn't translated isn't either (until nothing changes), and the rest are
 * written into one Rust module, with the helpers they use. What isn't translated is counted by why.
 *
 * The bytecode is read from the jar; nothing is decompiled.
 */
final class JarRust {
    private JarRust() {}

    /** A method of the jar, by class, name and descriptor. */
    record Id(String owner, String name, String descriptor) implements Comparable<Id> {
        @Override
        public int compareTo(Id o) {
            return (owner + "." + name + descriptor).compareTo(o.owner + "." + o.name + o.descriptor);
        }

        String display() {
            return owner.replace('/', '.') + "." + name + descriptor;
        }
    }

    /**
     * What came out. methods: how many methods have a body. translated: the Rust name of each one translated.
     * why: why each other one isn't. rust: the module's text.
     */
    record Result(int methods, Map<Id, String> translated, Map<Id, String> why, String rust) {
        /** How many methods each reason keeps out, the most first; a call of an untranslated method counts as one reason. */
        Map<String, Integer> reasons() {
            Map<String, Integer> counts = new HashMap<>();
            for (String w : why.values()) counts.merge(kind(w), 1, Integer::sum);
            Map<String, Integer> out = new LinkedHashMap<>();
            counts.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue() != 0 ? b.getValue() - a.getValue() : a.getKey().compareTo(b.getKey()))
                    .forEach(e -> out.put(e.getKey(), e.getValue()));
            return out;
        }
    }

    /** A reason without what is particular to the method: "it takes an object (Lfoo/Bar;)..." as "it takes an object". */
    static String kind(String why) {
        if (why.startsWith("it calls the game")) return "it calls the game";
        if (why.startsWith("it calls ") && why.contains("which this translator doesn't know")) return "it calls Java's library beyond Math";
        if (why.startsWith("it calls ") && why.contains("isn't translated")) return "it calls a method of the mod that isn't translated";
        if (why.startsWith("it takes an object")) return "it takes an object";
        if (why.startsWith("it returns an object")) return "it returns an object";
        if (why.startsWith("its bytecode doesn't decode")) return "its bytecode doesn't decode";
        return why;
    }

    static Result translate(ModJar jar) throws IOException {
        Map<String, ClassData> classes = new TreeMap<>();
        try (ZipFile zip = new ZipFile(jar.path().toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (e.isDirectory() || !name.endsWith(".class") || name.startsWith("META-INF/") || name.endsWith("module-info.class")) continue;
                ClassData c = ClassData.parse(ModJar.read(zip, e, Analyzer.MAX_CLASS));
                classes.put(c.name, c);
            }
        }
        return translate(classes);
    }

    static Result translate(Map<String, ClassData> classes) {
        int methods = 0;
        Map<Id, String> why = new TreeMap<>();
        Map<Id, List<Call>> calls = new TreeMap<>();
        Map<Id, ClassData> owners = new HashMap<>();
        Map<Id, ClassData.Method> bodies = new HashMap<>();
        for (ClassData c : classes.values()) {
            for (ClassData.Method m : c.methods) {
                if (m.body() == null) continue;
                methods++;
                Id id = new Id(c.name, m.name(), m.descriptor());
                MethodRust.Check check;
                try {
                    check = MethodRust.check(m, c.pool);
                } catch (ClassData.Malformed e) {
                    check = new MethodRust.Check("its bytecode doesn't decode: " + e.getMessage(), List.of());
                }
                if (check.why() != null) {
                    why.put(id, check.why());
                } else {
                    calls.put(id, check.calls());
                    owners.put(id, c);
                    bodies.put(id, m);
                }
            }
        }
        Map<Id, String> translated = new TreeMap<>();
        Set<String> helpers = new TreeSet<>();
        StringBuilder functions = new StringBuilder();
        // Written until every method written has its callees written too: one whose flow can't be followed drops
        // out, and so do its callers.
        for (boolean again = true; again; ) {
            dropUncallable(calls, why);
            Map<Id, String> names = names(calls.keySet());
            translated.clear();
            helpers.clear();
            functions.setLength(0);
            again = false;
            for (Id id : List.copyOf(calls.keySet())) {
                try {
                    MethodRust.Done done = MethodRust.write(id.owner(), bodies.get(id), owners.get(id).pool, names.get(id),
                            call -> names.get(new Id(call.owner(), call.name(), call.descriptor())));
                    functions.append('\n').append(done.rust());
                    helpers.addAll(done.helpers());
                    translated.put(id, names.get(id));
                } catch (RuntimeException e) {
                    why.put(id, "its flow couldn't be followed: " + e.getMessage());
                    calls.remove(id);
                    again = true;
                }
            }
        }
        StringBuilder rust = new StringBuilder();
        rust.append("// The mod's methods that need nothing but numbers, translated from their bytecode by horizon-port-neoforge's own\n");
        rust.append("// translator, with Java's semantics: wrapping int arithmetic, shifts modulo the width, saturating float casts,\n");
        rust.append("// NaN as each comparison treats it, Math's exact results. Where Java throws (an int divided by zero), this panics.\n");
        rust.append("// ").append(translated.size()).append(" of ").append(methods).append(" methods; translation.md says why the others aren't.\n");
        for (String h : helpers) rust.append('\n').append("#[allow(dead_code)]\n").append(MethodRust.HELPERS.get(h)).append('\n');
        rust.append(functions);
        return new Result(methods, translated, why, rust.toString());
    }

    /** A method that calls one of the mod's that isn't translated isn't either, until nothing changes. */
    private static void dropUncallable(Map<Id, List<Call>> calls, Map<Id, String> why) {
        for (boolean changed = true; changed; ) {
            changed = false;
            for (Id id : List.copyOf(calls.keySet())) {
                for (Call call : calls.get(id)) {
                    Id callee = new Id(call.owner(), call.name(), call.descriptor());
                    if (calls.containsKey(callee)) continue;
                    why.put(id, why.containsKey(callee) ? "it calls " + callee.display() + ", which isn't translated (" + why.get(callee) + ")"
                            : "it calls " + callee.display() + ", which isn't translated (not in the jar: another mod's, or inherited)");
                    calls.remove(id);
                    changed = true;
                    break;
                }
            }
        }
    }

    /** Each method's Rust name: `class_name__method_name`, made unique by its descriptor, then by a number. */
    static Map<Id, String> names(Set<Id> ids) {
        Map<String, List<Id>> byBase = new TreeMap<>();
        for (Id id : new TreeSet<>(ids)) byBase.computeIfAbsent(snake(simple(id.owner())) + "__" + snake(id.name()), k -> new ArrayList<>()).add(id);
        Map<Id, String> out = new HashMap<>();
        Set<String> taken = new TreeSet<>();
        byBase.forEach((base, list) -> {
            for (Id id : list) {
                String name = list.size() == 1 ? base : base + "__" + descriptorWord(id.descriptor());
                String unique = name;
                for (int n = 2; !taken.add(unique); n++) unique = name + "_" + n;
                out.put(id, unique);
            }
        });
        return out;
    }

    private static String simple(String internal) {
        return internal.substring(internal.lastIndexOf('/') + 1);
    }

    private static String descriptorWord(String descriptor) {
        String params = descriptor.substring(1, descriptor.indexOf(')'));
        return params.isEmpty() ? "v" : params.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "_");
    }

    /** `MathUtil$Inner` as `math_util_inner`, `lambda$x$0` as `lambda_x_0`. */
    static String snake(String name) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                boolean afterSmall = i > 0 && (Character.isLowerCase(name.charAt(i - 1)) || Character.isDigit(name.charAt(i - 1)));
                boolean beforeSmall = i > 0 && Character.isUpperCase(name.charAt(i - 1)) && i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1));
                if ((afterSmall || beforeSmall) && b.length() > 0 && b.charAt(b.length() - 1) != '_') b.append('_');
                b.append(Character.toLowerCase(c));
            } else if (c < 128 && Character.isLetterOrDigit(c)) {
                b.append(c);
            } else if (b.length() > 0 && b.charAt(b.length() - 1) != '_') {
                b.append('_');
            }
        }
        String s = b.toString().replaceAll("_+$", "");
        if (s.isEmpty() || Character.isDigit(s.charAt(0))) s = "m_" + s;
        return s;
    }

    /** translation.md: the count, the reasons, and each method left out with why. */
    static String report(Result r, int horizonPort) {
        StringBuilder b = new StringBuilder("# What horizon-port-neoforge's own translator translated\n\n");
        b.append(r.translated().size()).append(" of ").append(r.methods()).append(" methods with a body, from their bytecode");
        if (horizonPort >= 0) b.append(" (horizon-port, from decompiled Java: ").append(horizonPort).append(")");
        b.append(". This step translates static methods of numbers only; objects, arrays, fields, lambdas and exceptions come next.\n\n");
        b.append("| Why the others aren't | Methods |\n|---|---|\n");
        r.reasons().forEach((w, n) -> b.append("| ").append(w.replace("|", "\\|")).append(" | ").append(n).append(" |\n"));
        b.append("\n## Translated\n\n");
        r.translated().forEach((id, name) -> b.append("- `").append(id.display()).append("` as `").append(name).append("`\n"));
        b.append("\n## Not translated\n\n");
        r.why().forEach((id, w) -> b.append("- `").append(id.display()).append("`: ").append(w).append('\n'));
        return b.toString();
    }
}
