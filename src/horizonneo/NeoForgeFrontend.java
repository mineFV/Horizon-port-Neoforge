package horizonneo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The NeoForge frontend: a jar with a META-INF/neoforge.mods.toml. */
final class NeoForgeFrontend implements ModFrontend {
    private final ModsToml toml;

    private NeoForgeFrontend(ModsToml toml) {
        this.toml = toml;
    }

    static NeoForgeFrontend read(ModJar jar) throws ModsToml.Invalid {
        if (jar.modsToml() == null) throw new ModsToml.Invalid(jar.name() + " has no " + ModsToml.PATH);
        return new NeoForgeFrontend(ModsToml.read(jar.modsToml(), jar.jarVersion()));
    }

    /** The metadata file itself, with everything NeoForge-specific in it. */
    ModsToml toml() {
        return toml;
    }

    @Override
    public String loader() {
        return "neoforge";
    }

    /** The jar's mod is its first [[mods]] entry; a jar that lists more says so in the warnings. */
    @Override
    public ModInfo info() {
        ModsToml.Mod mod = toml.mods().get(0);
        List<String> warnings = new ArrayList<>(toml.warnings());
        if (toml.mods().size() > 1) {
            warnings.add(ModsToml.PATH + " lists " + toml.mods().size() + " mods: only the first, `" + mod.modId() + "`, is converted");
        }
        if (toml.declaredLicense() == null && !toml.license().isBlank()) {
            warnings.add(ModsToml.PATH + ": `license` is a build placeholder nobody filled in (`" + toml.license().strip() + "`), so no licence is declared");
        }
        String licence = toml.declaredLicense();
        return new ModInfo(loader(), ModsToml.PATH, mod.modId(), mod.displayName(), mod.version(), mod.description(),
                authors(mod.authors()), licence == null ? List.of() : List.of(licence),
                toml.mixins().stream().map(ModsToml.MixinConfig::config).toList(), warnings);
    }

    /**
     * Who counts as an author. NeoForge's `authors` is one free-form text: the whole text is an author,
     * and so is each name between commas in it. Nothing looser: "A and B" is one name.
     */
    static List<String> authors(String text) {
        Set<String> names = new LinkedHashSet<>();
        if (!text.isBlank()) names.add(text.strip());
        for (String part : text.split(",")) {
            if (!part.isBlank()) names.add(part.strip());
        }
        return List.copyOf(names);
    }
}
