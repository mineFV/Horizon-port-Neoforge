package horizonneo;

import java.util.List;

/**
 * What a mod jar says about itself, whichever loader it was made for. A loader's frontend makes it;
 * everything after works on it and never asks which loader it was.
 *
 * metadataFile: the file in the jar this was read from. licences: what the mod declares, none when it
 * declares nothing usable. authors: every name that counts as an author of the mod.
 */
record ModInfo(String loader, String metadataFile, String id, String name, String version, String description,
               List<String> authors, List<String> licences, List<String> mixinConfigs, List<String> warnings) {
    ModInfo {
        authors = List.copyOf(authors);
        licences = List.copyOf(licences);
        mixinConfigs = List.copyOf(mixinConfigs);
        warnings = List.copyOf(warnings);
    }

    /** What is known of a mod by its id alone: its name is its id, its version "0". */
    static ModInfo minimal(String id) {
        return new ModInfo("neoforge", null, id, id, "0", null, List.of(), List.of(), List.of(), List.of());
    }
}
