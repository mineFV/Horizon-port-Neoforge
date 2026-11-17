package horizonneo.probe;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/** Maven's version numbers (a mod's version, as NeoForge gives it), as GameProbe observes the rest (see there). */
final class ModProbe {
    private ModProbe() {}

    static final String[] VERSIONS = {"10.7.19", "1.0.0", "1", "1.2", "1.2.3.4", "1.2.3-SNAPSHOT", "1.2-beta-3", "1.2.3-4", "1.21.1-10.7.19.85",
        "0.0.1-alpha", "2.0.0+build.5", "v1.2", "1.2.x", "", "1..2", "999999999999.1", "-1", "1.2.3.4.5", "1.0-RC1", "abc", "1-99999999999999999999999", "2-4294967296", "3-18446744073709551617"};

    static void run() {
        for (String v : VERSIONS) {
            p("version [" + v + "]", () -> {
                ArtifactVersion a = new DefaultArtifactVersion(v);
                return a.getMajorVersion() + " " + a.getMinorVersion() + " " + a.getIncrementalVersion() + " " + a.getBuildNumber() + " [" + a.getQualifier() + "] " + a + " " + a.hashCode();
            });
        }
        for (String v : VERSIONS) {
            for (String w : VERSIONS) {
                p("version compare [" + v + "] [" + w + "]", () -> Integer.signum(new DefaultArtifactVersion(v).compareTo(new DefaultArtifactVersion(w))) + " " + new DefaultArtifactVersion(v).equals(new DefaultArtifactVersion(w)));
            }
        }
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
