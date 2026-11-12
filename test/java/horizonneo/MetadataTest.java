package horizonneo;

import java.nio.file.Path;
import java.util.List;

/** neoforge.mods.toml as the converter reads it: fields, defaults, what it fills in and what it refuses. */
final class MetadataTest {
    private MetadataTest() {}

    private static final String FULL = """
            modLoader = "javafml"
            loaderVersion = "[1,)"
            license = "MIT"
            issueTrackerURL = "https://example.com/issues"

            [[mods]]
            modId = "pocketcompass"
            version = "${file.jarVersion}"
            displayName = "Pocket Compass"
            authors = "Mine, Second Author"
            credits = "The testers"
            logoFile = "logo.png"
            displayURL = "https://example.com"
            description = '''
            Shows where you face.
            Made up for the tests.
            '''

            [[mixins]]
            config = "pocketcompass.mixins.json"

            [[mixins]]
            config = "pocketcompass.compat.mixins.json"
            requiredMods = ["jei"]

            [[accessTransformers]]
            file = "META-INF/accesstransformer.cfg"

            [[dependencies.pocketcompass]]
            modId = "neoforge"
            type = "required"
            versionRange = "[26.3,)"
            ordering = "NONE"
            side = "BOTH"

            [[dependencies.pocketcompass]]
            modId = "jei"
            type = "OPTIONAL"
            side = "client"
            reason = "Shows the compass recipe"

            [[dependencies.pocketcompass]]
            modId = "oldstyle"
            mandatory = false
            """;

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("metadata");

        ModsToml t = ModsToml.read(FULL, "1.2.3");
        c.same("license", "MIT", t.license());
        c.same("the declared licence", "MIT", t.declaredLicense());
        c.same("modLoader", "javafml", t.modLoader());
        c.same("loaderVersion", "[1,)", t.loaderVersion());
        c.same("issueTrackerURL", "https://example.com/issues", t.issueTrackerUrl());
        c.same("one mod", 1, t.mods().size());
        ModsToml.Mod mod = t.mods().get(0);
        c.same("modId", "pocketcompass", mod.modId());
        c.same("${file.jarVersion} is the manifest's version", "1.2.3", mod.version());
        c.same("displayName", "Pocket Compass", mod.displayName());
        c.same("authors, as written", "Mine, Second Author", mod.authors());
        c.same("credits", "The testers", mod.credits());
        c.same("logoFile", "logo.png", mod.logoFile());
        c.same("displayURL", "https://example.com", mod.displayUrl());
        c.same("a description over two lines", "Shows where you face.\nMade up for the tests.", mod.description());
        c.same("Mixin configs", List.of("pocketcompass.mixins.json", "pocketcompass.compat.mixins.json"),
                t.mixins().stream().map(ModsToml.MixinConfig::config).toList());
        c.same("a Mixin config's required mods", List.of("jei"), t.mixins().get(1).requiredMods());
        c.same("access transformers", List.of("META-INF/accesstransformer.cfg"), t.accessTransformers());
        c.same("dependencies", List.of("neoforge", "jei", "oldstyle"), t.dependencies().stream().map(ModsToml.Dependency::modId).toList());
        c.same("a dependency, in full", new ModsToml.Dependency("pocketcompass", "neoforge", "required", "[26.3,)", "NONE", "BOTH", ""),
                t.dependencies().get(0));
        c.same("a dependency's defaults, with type and side in any case",
                new ModsToml.Dependency("pocketcompass", "jei", "optional", "*", "NONE", "CLIENT", "Shows the compass recipe"), t.dependencies().get(1));
        c.same("`mandatory = false`, as files wrote before `type`", "optional", t.dependencies().get(2).type());
        c.same("nothing odd in it", List.of(), t.warnings());

        // What the documentation gives as defaults.
        ModsToml bare = ModsToml.read("license = \"MIT\"\n[[mods]]\nmodId = \"bare\"\n", null);
        ModsToml.Mod b = bare.mods().get(0);
        c.same("default version", "1", b.version());
        c.same("default displayName is the mod id", "bare", b.displayName());
        c.same("no description", "", b.description());
        c.same("no authors", "", b.authors());
        c.same("default modLoader", "javafml", bare.modLoader());
        c.yes("no dependencies, Mixin configs or transformers", bare.dependencies().isEmpty() && bare.mixins().isEmpty() && bare.accessTransformers().isEmpty());

        // Texts NeoForge fills in at load.
        ModsToml unknown = ModsToml.read("license = \"MIT\"\n[[mods]]\nmodId = \"m\"\nversion = \"${file.jarVersion}\"\n", null);
        c.same("${file.jarVersion} without a manifest version stays as written", "${file.jarVersion}", unknown.mods().get(0).version());
        c.yes("... with a warning", unknown.warnings().stream().anyMatch(w -> w.contains("`${file.jarVersion}` has no value in this jar")));
        ModsToml props = ModsToml.read("license = \"MIT\"\n[properties]\nflavour = \"Deluxe\"\n[[mods]]\nmodId = \"m\"\ndisplayName = \"Mod ${file.flavour}\"\n", "9");
        c.same("a ${file.<key>} from the file's own [properties]", "Mod Deluxe", props.mods().get(0).displayName());

        // The licence: only what the file really declares counts.
        ModsToml none = ModsToml.read("[[mods]]\nmodId = \"m\"\n", null);
        c.same("no license: none declared", null, none.declaredLicense());
        c.yes("... and a warning, since NeoForge requires it", none.warnings().stream().anyMatch(w -> w.contains("no `license`")));
        c.same("a blank license: none declared", null, ModsToml.read("license = \"  \"\n[[mods]]\nmodId = \"m\"\n", null).declaredLicense());
        c.same("a placeholder nobody filled in: none declared", null,
                ModsToml.read("license = \"${mod_license}\"\n[[mods]]\nmodId = \"m\"\n", null).declaredLicense());
        c.same("even one whose name looks like a licence", null,
                ModsToml.read("license = \"${license.MIT}\"\n[[mods]]\nmodId = \"m\"\n", null).declaredLicense());
        ModsToml number = ModsToml.read("license = 3\n[[mods]]\nmodId = \"m\"\n", null);
        c.same("a license that isn't text: none declared", null, number.declaredLicense());
        c.yes("... with a warning", number.warnings().stream().anyMatch(w -> w.contains("`license` isn't text")));
        c.same("spaces around a licence go", "LGPL-3.0-only", ModsToml.read("license = \" LGPL-3.0-only \"\n[[mods]]\nmodId = \"m\"\n", null).declaredLicense());

        // Not a mod's metadata.
        c.refuses("no [[mods]]", "META-INF/neoforge.mods.toml: no [[mods]] entry", () -> ModsToml.read("license = \"MIT\"\n", null));
        c.refuses("a [[mods]] without a modId", "a [[mods]] entry has no modId", () -> ModsToml.read("license = \"MIT\"\n[[mods]]\nversion = \"1\"\n", null));
        c.refuses("broken TOML names the file and the line", "META-INF/neoforge.mods.toml, line 2: can't read the value",
                () -> ModsToml.read("license = \"MIT\"\nmodLoader = javafml\n", null));
        ModsToml lenient = ModsToml.read("license = \"MIT\"\n[[mods]]\nmodId = \"m\"\n[[mixins]]\nrequiredMods = []\n[[dependencies.m]]\ntype = \"required\"\n", null);
        c.yes("a Mixin entry without a config and a dependency without a modId are left out", lenient.mixins().isEmpty() && lenient.dependencies().isEmpty());
        c.same("... with a warning each", 2, lenient.warnings().size());

        // Who counts as an author.
        c.same("the whole text and each name between commas", List.of("Mine, Second Author", "Mine", "Second Author"), NeoForgeFrontend.authors("Mine, Second Author"));
        c.same("one author", List.of("Solo"), NeoForgeFrontend.authors("Solo"));
        c.same("no author", List.of(), NeoForgeFrontend.authors(""));
        c.same("`and` doesn't split", List.of("A and B"), NeoForgeFrontend.authors("A and B"));
        c.same("blanks between commas aren't names", List.of("A ,, B", "A", "B"), NeoForgeFrontend.authors(" A ,, B "));

        // The loader-neutral view.
        ModInfo info = NeoForgeFrontend.read(jar(FULL, "1.2.3")).info();
        c.same("loader", "neoforge", info.loader());
        c.same("the file it was read from", "META-INF/neoforge.mods.toml", info.metadataFile());
        c.same("id, name, version", List.of("pocketcompass", "Pocket Compass", "1.2.3"), List.of(info.id(), info.name(), info.version()));
        c.same("licences", List.of("MIT"), info.licences());
        c.same("authors", List.of("Mine, Second Author", "Mine", "Second Author"), info.authors());
        c.same("Mixin configs", List.of("pocketcompass.mixins.json", "pocketcompass.compat.mixins.json"), info.mixinConfigs());
        ModInfo two = NeoForgeFrontend.read(jar("license = \"MIT\"\n[[mods]]\nmodId = \"first\"\n[[mods]]\nmodId = \"second\"\n", null)).info();
        c.same("a jar with two mods: the first is the jar's mod", "first", two.id());
        c.yes("... and the warnings say so", two.warnings().stream().anyMatch(w -> w.contains("lists 2 mods: only the first, `first`, is converted")));
        ModInfo placeholder = NeoForgeFrontend.read(jar("license = \"${mod_license}\"\n[[mods]]\nmodId = \"m\"\n", null)).info();
        c.same("a placeholder licence: no licence handed on", List.of(), placeholder.licences());
        c.yes("... and the warnings say why", placeholder.warnings().stream().anyMatch(w -> w.contains("build placeholder nobody filled in (`${mod_license}`)")));
        c.refuses("a jar without the file", "has no META-INF/neoforge.mods.toml", () -> NeoForgeFrontend.read(jar(null, null)));

        // Entries that may be licence files, by name.
        for (String yes : List.of("LICENSE", "LICENSE.txt", "license.md", "META-INF/LICENSE_pocketcompass.md", "COPYING.LESSER", "UNLICENSE", "assets/x/licence.txt")) {
            c.yes(yes + " may be a licence file", ModJar.licenceLike(yes));
        }
        for (String no : List.of("META-INF/neoforge.mods.toml", "README.md", "com/example/Main.class", "../LICENSE", "/LICENSE", "a\\LICENSE")) {
            c.yes(no + " isn't a licence file", !ModJar.licenceLike(no));
        }

        System.exit(c.done());
    }

    private static ModJar jar(String modsToml, String jarVersion) {
        return new ModJar(Path.of("test.jar"), modsToml != null, false, false, modsToml, jarVersion, List.of());
    }
}
