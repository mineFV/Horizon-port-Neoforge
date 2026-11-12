package horizonneo;

import java.io.PrintStream;

/** horizon-port-neoforge: turns a NeoForge mod into a Horizon native-mod bundle, through Horizon-Translator's pipeline. */
public final class Main {
    /** Exit codes. REFUSED is the code horizon-port gives when a licence doesn't allow porting. */
    static final int DONE = 0, FAILED = 1, USAGE = 2, REFUSED = 3;

    private Main() {}

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** Runs one command and returns its exit code. It never exits the JVM itself, so tests can call it. */
    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0) {
            err.print(HELP);
            return USAGE;
        }
        if (args[0].equals("-h") || args[0].equals("--help") || args[0].equals("help")) {
            out.print(HELP);
            return DONE;
        }
        err.println("horizon-port-neoforge: unknown command `" + args[0] + "` (see --help)");
        return USAGE;
    }

    static final String HELP = """
            horizon-port-neoforge: NeoForge mods to Horizon native-mod bundles

            No command yet: this is the project's skeleton.
            """;
}
