package net.neoforged.neoforgespi.language;

import org.apache.maven.artifact.versioning.ArtifactVersion;

/** The stand-in for NeoForge's IModInfo: a mod's id, name, version and namespace (from its neoforge.mods.toml). */
public interface IModInfo {
    String getModId();

    String getDisplayName();

    ArtifactVersion getVersion();

    String getNamespace();
}
