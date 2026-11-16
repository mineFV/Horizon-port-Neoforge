package horizonneo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/** Tickers, on made-up classes (test/tickers/, compiled by run-test.sh; their folder is the first argument). */
public final class TickersTest {
    private TickersTest() {}

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("tickers");
        Map<String, ClassData> jar = new TreeMap<>();
        for (String name : new String[] {"OneTicker", "TwoTickers", "Capturing"}) {
            ClassData d = ClassData.parse(Files.readAllBytes(Path.of(args[0], "com/example/tickers", name + ".class")));
            jar.put(d.name, d);
        }
        Tickers.Tick one = Tickers.of("com.example.tickers.OneTicker$$Lambda", jar);
        c.same("one method, made in two places: certain", "com.example.tickers.OneTicker.serverTick(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Lcom/example/tickers/OneTicker;)V", one.shown());
        c.same("its owner, as the bytecode names it", "com/example/tickers/OneTicker", one.owner());
        c.same("two methods: not certain", "not certain: com.example.tickers.TwoTickers makes 2 different BlockEntityTicker lambdas, and which one this is can't be told",
                Tickers.of("com.example.tickers.TwoTickers$$Lambda", jar).shown());
        c.same("captured values: not certain", "not certain: the ticker's lambda captures values (com.example.tickers.Capturing.lambda$ticker$0), which aren't known",
                Tickers.of("com.example.tickers.Capturing$$Lambda", jar).shown());
        c.same("an object of a class of its own", "not certain: the ticker is an object of com.example.tickers.SpinTicker, not a lambda: what it holds isn't known",
                Tickers.of("com.example.tickers.SpinTicker", jar).shown());
        c.same("made outside the mod", "not certain: the ticker is a lambda made in net.minecraft.world.level.block.BaseEntityBlock, which isn't the mod's",
                Tickers.of("net.minecraft.world.level.block.BaseEntityBlock$$Lambda", jar).shown());
        c.same("no ticker said", "not certain: dump didn't say what the ticker is (a machines file from before 2026-11-17)", Tickers.of(null, jar).shown());

        // The classes the JVM wrote for those lambdas (args[1]), by the names Dumper printed (args[2]): each one's code
        // names its method, so the class with two of them is told apart.
        Map<String, String> names = new TreeMap<>();
        for (String line : Files.readAllLines(Path.of(args[2]))) names.put(line.substring(0, line.indexOf(' ')), line.substring(line.indexOf(' ') + 1));
        String desc = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Lcom/example/tickers/";
        c.same("the server's of two, from its class", "com.example.tickers.TwoTickers.serverTick" + desc + "TwoTickers;)V", fromJvm(args[1], names.get("two-server"), jar).shown());
        c.same("the client's of two, from its class", "com.example.tickers.TwoTickers.clientTick" + desc + "TwoTickers;)V", fromJvm(args[1], names.get("two-client"), jar).shown());
        c.same("one, from its class", "com.example.tickers.OneTicker.serverTick" + desc + "OneTicker;)V", fromJvm(args[1], names.get("one"), jar).shown());
        c.same("captured values, from its class", "not certain: the ticker's lambda captures values (its class holds arg$1), which aren't known",
                fromJvm(args[1], names.get("capturing"), jar).shown());
        c.same("not the mod's", "not certain: the ticker runs com.example.tickers.TwoTickers.serverTick, which isn't the mod's",
                fromJvm(args[1], names.get("two-server"), Map.of()).shown());
        c.same("a class that doesn't read", true, Tickers.of("x", new byte[] {1, 2, 3}, jar).shown().startsWith("not certain: the ticker's class, as the JVM wrote it, doesn't read"));
        c.done();
    }

    private static Tickers.Tick fromJvm(String dir, String name, Map<String, ClassData> jar) throws java.io.IOException {
        // The JVM writes "a.b.Owner$$Lambda/0x12" as a/b/Owner$$Lambda.0x12.class, as the exporter reads it.
        int slash = name.indexOf('/');
        String file = name.substring(0, slash).replace('.', '/') + "." + name.substring(slash + 1) + ".class";
        return Tickers.of(name, Files.readAllBytes(Path.of(dir, file)), jar);
    }
}
