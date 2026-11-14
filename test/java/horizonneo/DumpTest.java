package horizonneo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** `dump`'s reading of a NeoForge server folder, on made-up folders: nothing here is NeoForge's or the game's. */
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

            Path root = Path.of("/srv/neo");
            String lib = root.resolve("libraries").toString().replace('\\', '/');
            c.same("each path into libraries/ made absolute, after a space, a colon, an equals sign, or at a line's start",
                    "-p " + lib + "/a.jar:" + lib + "/b.jar\n-DlibraryDirectory=" + lib + "\n" + lib + "/c.jar --launchTarget forgeserver",
                    Dump.absolute("-p libraries/a.jar:libraries/b.jar\n-DlibraryDirectory=libraries\nlibraries/c.jar --launchTarget forgeserver", root));
            c.same("a name that only holds the word is left alone", "-DignoreList=mylibraries.jar,somelibraries/x.jar",
                    Dump.absolute("-DignoreList=mylibraries.jar,somelibraries/x.jar", root));

            // ---- the EULA ----
            c.yes("no eula.txt: not accepted", !Dump.eulaAccepted(server));
            Files.writeString(server.resolve("eula.txt"), "#By changing the setting below to TRUE you are indicating your agreement\neula=false\n");
            c.yes("eula=false: not accepted", !Dump.eulaAccepted(server));
            Files.writeString(server.resolve("eula.txt"), "# eula=true is what the comment says\neula=FALSE\n");
            c.yes("a comment that names it isn't accepting it", !Dump.eulaAccepted(server));
            Files.writeString(server.resolve("eula.txt"), "#comment\neula=true\n");
            c.yes("eula=true: accepted", Dump.eulaAccepted(server));

            // ---- the dumper's classpath ----
            for (String jar : List.of("net/minecraft/server/1.21.1/server-1.21.1.jar", "net/minecraft/server/1.21.1-x/server-1.21.1-x-srg.jar",
                    "net/minecraft/server/1.21.1-x/server-1.21.1-x-slim.jar", "net/neoforged/neoforge/21.1.266/neoforge-21.1.266-server.jar",
                    "net/neoforged/neoforge/21.1.266/neoforge-21.1.266-universal.jar", "com/google/gson/gson.jar")) {
                Files.createDirectories(server.resolve("libraries/" + jar).getParent());
                Files.writeString(server.resolve("libraries/" + jar), "");
            }
            List<String> cp = Dump.classpath(server).stream().map(p -> server.resolve("libraries").relativize(p).toString().replace('\\', '/')).toList();
            c.same("NeoForge's patched classes, then the game's named ones, then the rest; not the installer's inputs", List.of(
                    "net/neoforged/neoforge/21.1.266/neoforge-21.1.266-server.jar",
                    "net/minecraft/server/1.21.1-x/server-1.21.1-x-srg.jar",
                    "com/google/gson/gson.jar",
                    "net/neoforged/neoforge/21.1.266/neoforge-21.1.266-universal.jar"), cp);

            // ---- the server it runs ----
            String properties = Dump.properties(25565);
            c.yes("it listens on this machine alone", properties.contains("\nserver-ip=127.0.0.1\n") && properties.contains("\nserver-port=25565\n"));
            c.yes("in a flat world with no structures", properties.contains("\nlevel-type=minecraft\\:flat\n") && properties.contains("\ngenerate-structures=false\n"));
            c.yes("and no remote console", properties.contains("\nenable-rcon=false\n"));

            // ---- the dumper in this tool's jar ----
            for (String f : Dump.DUMPER_FILES.keySet()) {
                c.yes("the tool carries " + f, Dump.class.getResource("/" + f) != null);
            }
        } finally {
            Checks.delete(dir);
        }
        System.exit(c.done());
    }
}
