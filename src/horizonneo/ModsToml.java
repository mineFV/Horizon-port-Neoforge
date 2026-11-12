package horizonneo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * META-INF/neoforge.mods.toml, as NeoForge's "Mod Files" page describes it: the jar's own properties,
 * each [[mods]] entry, their dependencies, the Mixin configs and the access transformers. Defaults are
 * the ones that page gives.
 *
 * license is "" when the file has none. Texts NeoForge fills in at load (`${file.jarVersion}`, and the
 * file's own `[properties]`) are filled in here too.
 */
record ModsToml(
        String license, String modLoader, String loaderVersion, String issueTrackerUrl,
        List<Mod> mods, List<Dependency> dependencies, List<MixinConfig> mixins, List<String> accessTransformers,
        List<String> warnings) {

    /** Where the file is in a jar. */
    static final String PATH = "META-INF/neoforge.mods.toml";

    /** One [[mods]] entry. authors is the file's one free-form text, "" when there is none. */
    record Mod(String modId, String version, String displayName, String description, String authors, String credits,
               String logoFile, String displayUrl) {}

    /** One [[dependencies.<of>]] entry: `of` depends on `modId`. type is required, optional, incompatible or discouraged. */
    record Dependency(String of, String modId, String type, String versionRange, String ordering, String side, String reason) {}

    /** One [[mixins]] entry: a Mixin config file in the jar. */
    record MixinConfig(String config, List<String> requiredMods, String behaviorVersion) {}

    /** The file can't be a mod's metadata: it isn't TOML, or it names no mod. */
    static final class Invalid extends Exception {
        private static final long serialVersionUID = 1L;

        Invalid(String message) {
            super(message);
        }
    }

    ModsToml {
        mods = List.copyOf(mods);
        dependencies = List.copyOf(dependencies);
        mixins = List.copyOf(mixins);
        accessTransformers = List.copyOf(accessTransformers);
        warnings = List.copyOf(warnings);
    }

    /**
     * The licence the file declares, or null when it gives none to go by: missing, blank, or a build
     * placeholder (`${mod_license}`) that nobody filled in. A placeholder's name must never be read as
     * a licence.
     */
    String declaredLicense() {
        return license.isBlank() || license.contains("${") ? null : license.strip();
    }

    private static final Pattern FILE_PROPERTY = Pattern.compile("\\$\\{file\\.([A-Za-z0-9_.-]+)}");

    /** jarVersion: the jar manifest's Implementation-Version (what `${file.jarVersion}` stands for), or null. */
    static ModsToml read(String text, String jarVersion) throws Invalid {
        Toml.Doc doc;
        try {
            doc = Toml.parse(text);
        } catch (Toml.Problem e) {
            throw new Invalid(PATH + ", " + e.getMessage());
        }
        Map<String, Object> root = doc.root();
        List<String> warnings = new ArrayList<>();
        for (String w : doc.warnings()) warnings.add(PATH + ", " + w);

        Map<String, String> fileProperties = new LinkedHashMap<>();
        if (jarVersion != null) fileProperties.put("jarVersion", jarVersion);
        for (Map.Entry<String, Object> p : Toml.table(root.get("properties")).entrySet()) {
            if (p.getValue() instanceof String value) fileProperties.putIfAbsent(p.getKey(), value);
        }
        Filler fill = new Filler(fileProperties, warnings);

        String license = "";
        if (root.get("license") instanceof String declared) license = declared;
        else if (root.containsKey("license")) warnings.add(PATH + ": `license` isn't text, so it is read as missing");
        else warnings.add(PATH + ": no `license`, which NeoForge requires");

        List<Mod> mods = new ArrayList<>();
        for (Map<String, Object> m : Toml.tables(root.get("mods"))) {
            String id = Toml.text(m, "modId");
            if (id == null || id.isBlank()) throw new Invalid(PATH + ": a [[mods]] entry has no modId");
            mods.add(new Mod(id,
                    fill.in(or(Toml.text(m, "version"), "1")),
                    fill.in(or(Toml.text(m, "displayName"), id)),
                    fill.in(or(Toml.text(m, "description"), "")).strip(),
                    fill.in(or(Toml.text(m, "authors"), "")).strip(),
                    fill.in(or(Toml.text(m, "credits"), "")).strip(),
                    or(Toml.text(m, "logoFile"), ""),
                    or(Toml.text(m, "displayURL"), "")));
        }
        if (mods.isEmpty()) throw new Invalid(PATH + ": no [[mods]] entry, so this jar isn't a mod");

        List<Dependency> dependencies = new ArrayList<>();
        for (Map.Entry<String, Object> of : Toml.table(root.get("dependencies")).entrySet()) {
            for (Map<String, Object> d : Toml.tables(of.getValue())) {
                String id = Toml.text(d, "modId");
                if (id == null || id.isBlank()) {
                    warnings.add(PATH + ": a dependency of `" + of.getKey() + "` has no modId and is left out");
                    continue;
                }
                dependencies.add(new Dependency(of.getKey(), id, dependencyType(d),
                        or(Toml.text(d, "versionRange"), "*"),
                        or(Toml.text(d, "ordering"), "NONE").toUpperCase(Locale.ROOT),
                        or(Toml.text(d, "side"), "BOTH").toUpperCase(Locale.ROOT),
                        or(Toml.text(d, "reason"), "")));
            }
        }

        List<MixinConfig> mixins = new ArrayList<>();
        for (Map<String, Object> m : Toml.tables(root.get("mixins"))) {
            String config = Toml.text(m, "config");
            if (config == null || config.isBlank()) {
                warnings.add(PATH + ": a [[mixins]] entry has no config and is left out");
                continue;
            }
            mixins.add(new MixinConfig(config, List.copyOf(Toml.texts(m.get("requiredMods"))), or(Toml.text(m, "behaviorVersion"), "")));
        }

        List<String> transformers = new ArrayList<>();
        for (Map<String, Object> t : Toml.tables(root.get("accessTransformers"))) {
            String file = Toml.text(t, "file");
            if (file == null || file.isBlank()) warnings.add(PATH + ": an [[accessTransformers]] entry has no file and is left out");
            else transformers.add(file);
        }

        return new ModsToml(license, or(Toml.text(root, "modLoader"), "javafml"), or(Toml.text(root, "loaderVersion"), ""),
                or(Toml.text(root, "issueTrackerURL"), ""), mods, dependencies, mixins, transformers, warnings);
    }

    /** `type`, or what files from before it existed say with `mandatory = true/false`. */
    private static String dependencyType(Map<String, Object> d) {
        String type = Toml.text(d, "type");
        if (type != null) return type.toLowerCase(Locale.ROOT);
        return Boolean.FALSE.equals(d.get("mandatory")) ? "optional" : "required";
    }

    private static String or(String value, String fallback) {
        return value != null ? value : fallback;
    }

    /** Replaces `${file.<key>}` with the jar's value for it; a key nothing gives stays as written, with a warning. */
    private record Filler(Map<String, String> properties, List<String> warnings) {
        String in(String text) {
            Matcher m = FILE_PROPERTY.matcher(text);
            StringBuilder out = new StringBuilder();
            while (m.find()) {
                String value = properties.get(m.group(1));
                if (value == null) {
                    String warning = PATH + ": `" + m.group() + "` has no value in this jar and is left as written";
                    if (!warnings.contains(warning)) warnings.add(warning);
                }
                m.appendReplacement(out, Matcher.quoteReplacement(value != null ? value : m.group()));
            }
            return m.appendTail(out).toString();
        }
    }
}
