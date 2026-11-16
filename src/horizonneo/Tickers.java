package horizonneo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Which of the mod's methods a block entity's ticker runs (the maintainer's choice (a), 2026-11-17). NeoForge gives
 * the ticker as an object, EntityBlock.getTicker's, a lambda whose method isn't readable at run time. `dump` has the
 * JVM write each lambda's class, and its exporter takes the ticker's along (`ticker_class`): its code is one call, to
 * the method it runs. Without it (an older machines file), the class's name (`ticker`, `Owner$$Lambda/0x...`, named
 * after the class whose code made it) leads to that class's lambdas that make a BlockEntityTicker: certain when they
 * all run one method. Mekanism makes all of its in one class, for four methods: only the class's code tells them
 * apart. Either way, a lambda that captures values isn't certain (they aren't known), and says why.
 */
final class Tickers {
    private Tickers() {}

    static final String TICKER = "net/minecraft/world/level/block/entity/BlockEntityTicker";
    static final String LAMBDA = "$$Lambda";

    /** The method a ticker runs (owner, name, descriptor, as the bytecode names them), or why it isn't certain. */
    record Tick(String owner, String name, String descriptor, String why) {
        String shown() {
            return why != null ? "not certain: " + why : owner.replace('/', '.') + "." + name + descriptor;
        }
    }

    /**
     * What a ticker runs: from its class's code (`classFile`, the JVM's own, which `dump` has it write), else from
     * its class's name (`ticker`) and the jar's classes.
     */
    static Tick of(String ticker, byte[] classFile, Map<String, ClassData> jar) {
        if (classFile == null) return of(ticker, jar);
        ClassData proxy;
        try {
            proxy = ClassData.parse(classFile);
        } catch (RuntimeException | java.io.IOException e) {
            return new Tick(null, null, null, "the ticker's class, as the JVM wrote it, doesn't read: " + e.getMessage());
        }
        for (ClassData.Field f : proxy.fields) {
            if (!f.isStatic()) return new Tick(null, null, null, "the ticker's lambda captures values (its class holds " + f.name() + "), which aren't known");
        }
        if (!proxy.interfaces.contains(TICKER)) return new Tick(null, null, null, "the ticker's class isn't a BlockEntityTicker of the game's");
        for (ClassData.Method m : proxy.methods) {
            if (!m.name().equals("tick") || m.isStatic() || (m.access() & 0x0040) != 0 || m.code() == null) continue;
            List<ClassData.Op> calls = m.code().stream().filter(op -> op.kind() == ClassData.Kind.CALL).toList();
            if (calls.size() != 1) return new Tick(null, null, null, "the ticker's code makes " + calls.size() + " calls, not the one a lambda makes");
            ClassData.Op call = calls.get(0);
            if (!jar.containsKey(call.owner())) return new Tick(null, null, null, "the ticker runs " + call.owner().replace('/', '.') + "." + call.name() + ", which isn't the mod's");
            return new Tick(call.owner(), call.name(), call.descriptor(), null);
        }
        return new Tick(null, null, null, "the ticker's class has no tick method this reader sees");
    }

    /** What `ticker` (the exporter's name of the ticker's class) runs, from the jar's classes alone. */
    static Tick of(String ticker, Map<String, ClassData> jar) {
        if (ticker == null || ticker.isEmpty()) return new Tick(null, null, null, "dump didn't say what the ticker is (a machines file from before 2026-11-17)");
        int lambda = ticker.indexOf(LAMBDA);
        if (lambda < 0) {
            return new Tick(null, null, null, "the ticker is an object of " + ticker + ", not a lambda: what it holds isn't known");
        }
        String owner = ticker.substring(0, lambda).replace('.', '/');
        ClassData c = jar.get(owner);
        if (c == null) return new Tick(null, null, null, "the ticker is a lambda made in " + owner.replace('/', '.') + ", which isn't the mod's");
        Set<String> targets = new LinkedHashSet<>();
        List<String> capturing = new ArrayList<>();
        for (ClassData.Method m : c.methods) {
            for (ClassData.Op op : m.code()) {
                if (op.kind() != ClassData.Kind.LAMBDA || !(op.value() instanceof ClassData.Lambda l) || !l.functionalInterface().equals(TICKER)) continue;
                targets.add(op.owner() + "." + op.name() + op.descriptor());
                if (!l.captured().isEmpty()) capturing.add(op.owner().replace('/', '.') + "." + op.name());
            }
        }
        if (targets.isEmpty()) return new Tick(null, null, null, "the ticker is a lambda made in " + owner.replace('/', '.') + ", whose code makes no BlockEntityTicker that this reader sees");
        if (targets.size() > 1) {
            return new Tick(null, null, null, owner.replace('/', '.') + " makes " + targets.size() + " different BlockEntityTicker lambdas, and which one this is can't be told");
        }
        if (!capturing.isEmpty()) return new Tick(null, null, null, "the ticker's lambda captures values (" + capturing.get(0) + "), which aren't known");
        String target = targets.iterator().next();
        int dot = target.lastIndexOf('.', target.indexOf('('));
        int paren = target.indexOf('(');
        return new Tick(target.substring(0, dot), target.substring(dot + 1, paren), target.substring(paren), null);
    }

    /** Each block's tick in the machines file (`tick`, as `shown`), for the ones NeoForge ticks on the server. */
    static Map<String, String> all(Map<String, Object> machines, Map<String, ClassData> jar) {
        Map<String, String> out = new TreeMap<>();
        Json.object(machines.get("blocks")).forEach((id, v) -> {
            Map<String, Object> o = Json.object(v);
            if (!Boolean.TRUE.equals(o.get("ticks_on_server"))) return;
            String file = Json.text(o, "ticker_class");
            out.put(id, of(Json.text(o, "ticker"), file == null ? null : java.util.Base64.getDecoder().decode(file), jar).shown());
        });
        return out;
    }
}
