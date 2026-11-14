package horizonneo;

import java.util.ArrayList;
import java.util.List;

/** What `pack` decides by itself: which files a native file may hold, and how the Horizon commit is pinned in the manifest. */
final class PackTest {
    private PackTest() {}

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("packing");

        // Horizon's rule for the files under mixins/<id>/ (docs/api/mod-files.md).
        for (String yes : List.of("horizon-mixin.toml", "runtime.rs", "tick.rs", "api/Cargo.toml", "api/src/lib.rs", "data/table.bin", "engine/data/deep/x.json",
                "README.md", "LICENSE", "LICENCE.txt", "COPYING", "NOTICE", ".gitignore", "api/README", "api/.gitignore")) {
            c.yes("taken: " + yes, Pack.allowed(yes));
        }
        for (String no : List.of("notes.txt", "Mod.class", "a/b/data/x.bin", "a/b/README.md", "src/Thing.java", "target/x.o", "../outside.rs", "a//b.rs", "./x.rs", "")) {
            c.yes("not taken: " + no, !Pack.allowed(no));
        }

        // The commit in the manifest.
        String commit = "0123456789abcdef0123456789abcdef01234567";
        String plain = "[mod]\nid = \"m\"\nversion = \"1\"\n\n[[module]]\nid = \"runtime\"\n";
        List<String> why = new ArrayList<>();
        c.same("added after [mod], before the first module", "[mod]\nid = \"m\"\nversion = \"1\"\n\n[compatibility]\nhorizon_revision = \"" + commit + "\"\n\n[[module]]\nid = \"runtime\"\n",
                Pack.pinned(plain, Toml.parse(plain).root(), commit, why));
        String bare = "[mod]\nid = \"m\"";
        c.same("added at the end when there is nothing after [mod]", "[mod]\nid = \"m\"\n\n[compatibility]\nhorizon_revision = \"" + commit + "\"\n\n",
                Pack.pinned(bare, Toml.parse(bare).root(), commit, why));
        String there = plain + "\n[compatibility]\nhorizon_revision = \"" + commit + "\"\n";
        c.same("left as it is when it names that commit", there, Pack.pinned(there, Toml.parse(there).root(), commit, why));
        c.same("nothing refused so far", List.of(), why);
        String other = plain + "\n[compatibility]\nhorizon_revision = \"" + "f".repeat(40) + "\"\n";
        Pack.pinned(other, Toml.parse(other).root(), commit, why);
        c.yes("another commit is refused", why.size() == 1 && why.get(0).contains("a native file names one commit"));
        String empty = plain + "\n[compatibility]\nnote = \"x\"\n";
        Pack.pinned(empty, Toml.parse(empty).root(), commit, why);
        c.yes("a table without the commit is refused", why.size() == 2 && why.get(1).contains("without `horizon_revision`"));

        System.exit(c.done());
    }
}
