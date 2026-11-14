package horizonneo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** `dump`'s reading of a NeoForge server folder and of the oracle's, on made-up folders: nothing here is NeoForge's or the game's. */
final class DumpTest {
    public static void main(String[] args) throws Exception {
        Checks c = new Checks("Dump");
        Path dir = Files.createTempDirectory("dump-test-");
        try {
            String name = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).startsWith("windows") ? "win_args.txt" : "unix_args.txt";

            // ---- the installer's arguments ----
            Path server = dir.resolve("server");
            c.same("a folder with no NeoForge has no arguments", null, Dump.argsFile(server));
            for (String v : List.of("21.1.9", "21.1.266", "21.1.30")) {
                Files.createDirectories(server.resolve("libraries/net/neoforged/neoforge/" + v));
                Files.writeString(server.resolve("libraries/net/neoforged/neoforge/" + v + "/" + name), "-p libraries/a.jar");
            }
            Files.createDirectories(server.resolve("libraries/net/neoforged/neoforge/21.2.1"));
            c.same("the newest NeoForge that has arguments, by number, not by text", server.resolve("libraries/net/neoforged/neoforge/21.1.266/" + name),
                    Dump.argsFile(server));

            // ---- the EULA ----
            c.yes("no eula.txt: not accepted", !Dump.eulaAccepted(server));
            Files.writeString(server.resolve("eula.txt"), "#By changing the setting below to TRUE you are indicating your agreement\neula=false\n");
            c.yes("eula=false: not accepted", !Dump.eulaAccepted(server));
            Files.writeString(server.resolve("eula.txt"), "# eula=true is what the comment says\neula=FALSE\n");
            c.yes("a comment that names it isn't accepting it", !Dump.eulaAccepted(server));
            Files.writeString(server.resolve("eula.txt"), "#comment\neula=true\n");
            c.yes("eula=true: accepted", Dump.eulaAccepted(server));

            // ---- the oracle's folder for that version ----
            Path home = dir.resolve("horizon/.local/neoforge-oracle/21.1.266");
            c.same("nothing there: a link to the server is made", null, Dump.link(home, server));
            c.yes("and it is a link to it", Files.isSymbolicLink(home) && Files.isSameFile(home, server));
            c.same("a link to the same server is used again", null, Dump.link(home, server));
            Path other = dir.resolve("other");
            Files.createDirectories(other);
            c.yes("a link to another folder is left alone, and said", String.valueOf(Dump.link(home, other)).contains("already links to another folder"));
            c.yes("and still points where it did", Files.isSameFile(home, server));
            Path plain = dir.resolve("horizon/.local/neoforge-oracle/21.1.1");
            Files.createDirectories(plain);
            c.yes("a folder there with no server in it is said, not replaced", String.valueOf(Dump.link(plain, server)).contains("holds no NeoForge server"));
            Files.createDirectories(plain.resolve("libraries/net/neoforged/neoforge/21.1.1"));
            Files.writeString(plain.resolve("libraries/net/neoforged/neoforge/21.1.1/" + name), "");
            c.same("an install of the oracle's own there is used", null, Dump.link(plain, server));
        } finally {
            Checks.delete(dir);
        }
        System.exit(c.done());
    }
}
