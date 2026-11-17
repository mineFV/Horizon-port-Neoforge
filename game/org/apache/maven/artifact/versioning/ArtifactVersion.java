package org.apache.maven.artifact.versioning;

/** The stand-in for Maven's ArtifactVersion: a version number (a mod's, as NeoForge gives it). */
public interface ArtifactVersion extends Comparable<ArtifactVersion> {
    int getMajorVersion();

    int getMinorVersion();

    int getIncrementalVersion();

    int getBuildNumber();

    String getQualifier();

    void parseVersion(String version);
}
