package horizonneo;

import horizonneo.Analysis.Registration;
import horizonneo.Analysis.Registry;
import horizonneo.Content.Declared;
import horizonneo.Content.Result;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** The content mod: what a carried folder declares, what the mod registers that it doesn't, and how each outcome is put in words. */
final class ContentTest {
    private ContentTest() {}

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("the content mod");
        Analysis a = analysis(1, 1, List.of(
                new Registry("BLOCK", "m", "com/example/Reg", "BLOCKS", true, List.of(
                        new Registration("gem_block", "registerSimpleBlock", "net/minecraft/world/level/block/Block", "what registerSimpleBlock always makes", "Reg.GEM_BLOCK"),
                        new Registration("odd_block", "registerBlock", "com/example/OddBlock", "the lambda at the call", "Reg.ODD_BLOCK"))),
                new Registry("ITEM", "m", "com/example/Reg", "ITEMS", true, List.of(
                        new Registration("gem", "registerSimpleItem", "net/minecraft/world/item/Item", "what registerSimpleItem always makes", "Reg.GEM"),
                        new Registration("gem_block", "registerSimpleBlockItem", "net/minecraft/world/item/BlockItem", "what registerSimpleBlockItem always makes", null),
                        new Registration(null, "register", null, null, null))),
                new Registry("SOUND_EVENT", "m", "com/example/Reg", "SOUNDS", true, List.of(new Registration("ding", "register", null, null, null)))));

        Path dir = Files.createTempDirectory("content-test-");
        try {
            // ---- what a carried folder declares ----
            Path carried = dir.resolve("carry");
            write(carried.resolve("horizon-mod.toml"), "id = \"m\"\n");
            write(carried.resolve("data/m/horizon/block/gem_block.json"), "{ \"template\": \"minecraft:stone\" }");
            write(carried.resolve("data/m/horizon/item/gem.json"), "{ \"template\": \"minecraft:stick\", \"max_stack_size\": 1 }");
            write(carried.resolve("data/m/horizon/item/tools/pick.json"), "{ \"template\": \"minecraft:stick\" }");
            write(carried.resolve("data/other/horizon/block/broken.json"), "not JSON");
            write(carried.resolve("data/m/horizon/block/notes.txt"), "not a declaration");
            write(carried.resolve("data/m/recipe/gem.json"), "{}");
            write(carried.resolve("assets/m/lang/en_us.json"), "{}");
            List<Declared> declared = Content.declared(carried, a);
            c.same("the declarations, in path order", List.of(
                    new Declared("block", "m:gem_block", "minecraft:stone", true),
                    new Declared("item", "m:gem", "minecraft:stick", true),
                    new Declared("item", "m:tools/pick", "minecraft:stick", false),
                    new Declared("block", "other:broken", null, false)), declared);
            c.same("a folder with no data declares nothing", List.of(), Content.declared(dir.resolve("nothing"), a));
            c.same("what the code registers that isn't declared: a block's own item goes with its block", List.of("block m:odd_block"), Content.undeclared(declared, a));
            c.same("with nothing declared, every block and item", List.of("block m:gem_block", "block m:odd_block", "item m:gem", "item m:gem_block"),
                    Content.undeclared(List.of(), a));
            Analysis wrapped = new Analysis(a.census(), List.of(), List.of(), a.registries(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), false,
                    List.of("MyBlockRegister"), List.of());
            c.same("with a register that wasn't read, one not found isn't known: not read, not no", List.of(
                    new Declared("block", "m:gem_block", "minecraft:stone", true),
                    new Declared("item", "m:gem", "minecraft:stick", true),
                    new Declared("item", "m:tools/pick", "minecraft:stick", null),
                    new Declared("block", "other:broken", null, null)), Content.declared(carried, wrapped));
            Analysis kindless = analysis(1, 1, List.of(new Registry(null, "m", "com/example/Reg", "THINGS", true,
                    List.of(new Registration("pick", "register", null, null, null)))));
            c.same("so is one beside a register whose kind wasn't read", null, Content.declared(carried, kindless).get(2).registered());
            c.same("an empty register of unknown kind changes nothing", false, Content.declared(carried,
                    analysis(1, 1, List.of(new Registry(null, "m", "com/example/Reg", "THINGS", true, List.of())))).get(2).registered());

            // ---- when nothing is carried ----
            ModJar jar = new ModJar(dir.resolve("m.jar"), true, false, false, "", null, List.of(), List.of());
            Result none = Content.carry(jar, analysis(0, 0, List.of()), null, dir.resolve("work"), dir.resolve("bundle"), false, null);
            c.same("a jar with no assets and no data", "none", none.state());
            c.yes("that is said", none.why().startsWith("the jar holds no `assets/` and no `data/`"));
            Result noTools = Content.carry(jar, a, null, dir.resolve("work"), dir.resolve("bundle"), false, null);
            c.same("content, but Horizon's tools aren't built", "no-tools", noTools.state());
            c.yes("with how to build them", noTools.why().contains("cargo build --release -p horizon-port-pack -p cargo-horizon-mod"));
            c.yes("nothing was written for either", !Files.exists(dir.resolve("bundle")) && !Files.exists(dir.resolve("work")));

            // ---- Horizon's tools, by where they are ----
            c.same("a source where they aren't built", null, HorizonTools.locate(null, dir));
            c.same("no source to look in", null, HorizonTools.locate(null, null));
            c.refuses("a folder that is named and doesn't hold them", "doesn't hold horizon-port-pack and cargo-horizon-mod", () -> HorizonTools.locate(dir.toString(), null));
            for (String build : List.of("debug", "release")) {
                write(dir.resolve("target/" + build + "/horizon-port-pack"), "");
                write(dir.resolve("target/" + build + "/cargo-horizon-mod"), "");
            }
            c.same("built in the source: the release build first", dir.resolve("target/release").toAbsolutePath(), HorizonTools.locate(null, dir).folder());
            c.same("a folder that is named wins", dir.resolve("target/debug").toAbsolutePath(), HorizonTools.locate(dir.resolve("target/debug").toString(), dir).folder());
            Files.delete(dir.resolve("target/release/cargo-horizon-mod"));
            c.same("one of the two alone isn't enough", dir.resolve("target/debug").toAbsolutePath(), HorizonTools.locate(null, dir).folder());
        } finally {
            Checks.delete(dir);
        }

        // ---- in words ----
        List<Declared> two = List.of(new Declared("block", "m:gem_block", "minecraft:stone", true), new Declared("item", "m:pick", "minecraft:stick", false));
        Map<String, String> leftOut = Map.of("todo/content-left-out/data/m/recipe/x.json", "a recipe whose NeoForge conditions don't pass");
        String packed = String.join("\n", Content.words(new Result("packed", "m.hmod", "content", 3, "Content only: 1 block port.", List.of("1 block", "2 recipes"), List.of(),
                two, List.of("block m:odd_block"), leftOut, "packed and checked by `cargo horizon-mod`")));
        c.yes("a packed one: the file, and where it goes", packed.startsWith("The mod's data and assets are in **`m.hmod`**, a content mod (3 files; its folder is `content/`)")
                && packed.contains("Put it in `horizon-mods/`"));
        c.yes("that it wasn't tried beside the native mod", packed.contains("whether it takes the two side by side wasn't tried"));
        c.yes("Horizon's own words on the jar", packed.contains("What `horizon-port-pack` says of the jar: Content only: 1 block port. It carries: 1 block; 2 recipes."));
        c.yes("each declaration, with what it copies", packed.contains("| block `m:gem_block` | `minecraft:stone` | yes |"));
        c.yes("one whose registers weren't all read says so", String.join("\n", Content.words(new Result("todo", null, "todo/content", 1, null, List.of(), List.of(),
                List.of(new Declared("item", "m:pick", "minecraft:stick", null)), List.of(), Map.of(), "not packed")))
                .contains("| item `m:pick` | `minecraft:stick` | not read: some of the mod's registers weren't read (`report/report.md` names them) |"));
        c.yes("one the code doesn't register stands out", packed.contains("| item `m:pick` | `minecraft:stick` | **no**: the carry declares it from an asset, the code doesn't register it |"));
        c.yes("what the code registers that isn't declared", packed.contains("not declared in the content mod (the jar has no blockstate or item model for them): block `m:odd_block`."));
        c.yes("what the carry kept out, with why", packed.contains("- `todo/content-left-out/data/m/recipe/x.json`: a recipe whose NeoForge conditions don't pass."));

        String todo = String.join("\n", Content.words(new Result("todo", null, "todo/content", 1, null, List.of(), List.of(), two, List.of(), Map.of(), "not packed: it declares 1 block and 1 item")));
        c.yes("one left to check: the folder, in bold that it isn't packed", todo.startsWith("The mod's data and assets were carried over by Horizon's `horizon-port-pack` into the folder content mod "
                + "`todo/content/` (1 file), **not packed: it declares 1 block and 1 item**."));
        c.yes("with a TODO", todo.contains("\nTODO: check each declaration below against the mod"));
        c.yes("and the declarations", todo.contains("| block `m:gem_block` | `minecraft:stone` | yes |"));
        c.same("nothing to carry", List.of("No content mod was made: the jar holds nothing."),
                Content.words(new Result("none", null, null, 0, null, List.of(), List.of(), List.of(), List.of("block m:odd_block"), Map.of(), "the jar holds nothing")));
        c.yes("no tools: the content is still to carry over", Content.words(new Result("no-tools", null, null, 0, null, List.of(), List.of(), List.of(), List.of(), Map.of(), "they aren't built"))
                .get(0).equals("**No content mod was made**: they aren't built. The jar's data and assets are still to carry over."));
        c.yes("a tool that refused: why, and the folder", Content.words(new Result("failed", null, "todo/content", 2, null, List.of(), List.of(), List.of(), List.of(), Map.of(), "cargo horizon-mod build refused it: bad"))
                .get(0).equals("**No `.hmod` was made**: cargo horizon-mod build refused it: bad. The folder it was to be packed from is `todo/content/`."));

        // ---- with the mod's facts ----
        Content.Oracle oracle = Content.oracle(Map.of(
                "blocks_unknown", Map.of("bin", "no vanilla Block with properties [active, facing]"),
                "items_unknown", Map.of("bin", "its block is left out"),
                "differences", Map.of("gem_block", List.of("class com.example.GemBlock (behaviour beyond Block)"))), "m");
        c.same("what was left out, each named in the mod's namespace, with why", Map.of(
                "block m:bin", "no vanilla Block with properties [active, facing]", "item m:bin", "its block is left out"), oracle.unknown());
        c.same("what a template doesn't reproduce", Map.of("m:gem_block", List.of("class com.example.GemBlock (behaviour beyond Block)")), oracle.differences());
        c.same("a report that says nothing of them", new Content.Oracle(Map.of(), Map.of()), Content.oracle(Map.of(), "m"));
        String withFacts = String.join("\n", Content.words(new Result("packed", "m.hmod", "content", 3, null, List.of(), List.of(),
                List.of(new Declared("block", "m:gem_block", "minecraft:stone", true)), List.of(), Map.of(), "packed", oracle)));
        c.yes("the ones left out are a TODO, with why", withFacts.contains("TODO: 2 blocks and items the mod registers aren't declared")
                && withFacts.contains("- block `m:bin`: no vanilla Block with properties [active, facing]."));
        c.yes("and what a template doesn't reproduce is listed", withFacts.contains("- `m:gem_block`: class com.example.GemBlock (behaviour beyond Block)."));
        c.yes("without facts, neither is said", !packed.contains("TODO: ") && !packed.contains("doesn't reproduce"));
        System.exit(c.done());
    }

    private static Analysis analysis(int assets, int data, List<Registry> registries) {
        return new Analysis(new Analysis.Census(1, assets, data), List.of(), List.of(), registries, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), false, List.of(), List.of());
    }

    private static void write(Path file, String text) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }
}
