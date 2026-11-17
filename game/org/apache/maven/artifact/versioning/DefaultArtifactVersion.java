package org.apache.maven.artifact.versioning;

/**
 * The stand-in for Maven's DefaultArtifactVersion, observed (`standins`, probe/ModProbe.java): major.minor.incremental,
 * then after a '-' a build number or a qualifier; anything else (a fourth number, a leading zero, a letter in a number,
 * "..") is all qualifier. Versions compare and are equal as Maven's version order says (ComparableVersion).
 */
public class DefaultArtifactVersion implements ArtifactVersion {
    private Integer majorVersion;
    private Integer minorVersion;
    private Integer incrementalVersion;
    private Integer buildNumber;
    private String qualifier;
    private ComparableVersion comparable;

    public DefaultArtifactVersion(String version) {
        parseVersion(version);
    }

    @Override
    public int hashCode() {
        return 11 + comparable.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ArtifactVersion version)) return false;
        return compareTo(version) == 0;
    }

    @Override
    public int compareTo(ArtifactVersion other) {
        if (other instanceof DefaultArtifactVersion d) return comparable.compareTo(d.comparable);
        return compareTo(new DefaultArtifactVersion(other.toString()));
    }

    @Override
    public int getMajorVersion() {
        return majorVersion != null ? majorVersion : 0;
    }

    @Override
    public int getMinorVersion() {
        return minorVersion != null ? minorVersion : 0;
    }

    @Override
    public int getIncrementalVersion() {
        return incrementalVersion != null ? incrementalVersion : 0;
    }

    @Override
    public int getBuildNumber() {
        return buildNumber != null ? buildNumber : 0;
    }

    @Override
    public String getQualifier() {
        return qualifier;
    }

    @Override
    public final void parseVersion(String version) {
        comparable = new ComparableVersion(version);
        majorVersion = null;
        minorVersion = null;
        incrementalVersion = null;
        buildNumber = null;
        qualifier = null;
        int index = version.indexOf('-');
        String part1 = index < 0 ? version : version.substring(0, index);
        String part2 = index < 0 ? null : version.substring(index + 1);
        if (part2 != null) {
            if (part2.length() == 1 || !part2.startsWith("0")) {
                buildNumber = tryParseInt(part2);
                if (buildNumber == null) qualifier = part2;
            } else {
                qualifier = part2;
            }
        }
        if (!part1.contains(".") && !part1.startsWith("0")) {
            majorVersion = tryParseInt(part1);
            if (majorVersion == null) {
                qualifier = version;
                buildNumber = null;
            }
            return;
        }
        boolean fallback = part1.contains("..") || part1.startsWith(".") || part1.endsWith(".");
        if (!fallback) {
            String[] tokens = part1.split("\\.");
            try {
                majorVersion = number(tokens[0]);
                if (tokens.length > 1) minorVersion = number(tokens[1]);
                if (tokens.length > 2) incrementalVersion = number(tokens[2]);
                if (tokens.length > 3) {
                    qualifier = tokens[3];
                    fallback = isDigits(qualifier) || tokens.length > 4;
                }
            } catch (NumberFormatException e) {
                fallback = true;
            }
        }
        if (fallback) {
            qualifier = version;
            majorVersion = null;
            minorVersion = null;
            incrementalVersion = null;
            buildNumber = null;
        }
    }

    private static boolean isDigits(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) < '0' || s.charAt(i) > '9') return false;
        return true;
    }

    private static Integer number(String s) {
        if (s.length() > 1 && s.startsWith("0")) throw new NumberFormatException("Number part has a leading 0: '" + s + "'");
        if (!isDigits(s)) throw new NumberFormatException(s);
        return Integer.valueOf(s);
    }

    private static Integer tryParseInt(String s) {
        if (!isDigits(s)) return null;
        try {
            long l = Long.parseLong(s);
            return l > Integer.MAX_VALUE ? null : (int) l;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return comparable.toString();
    }
}
