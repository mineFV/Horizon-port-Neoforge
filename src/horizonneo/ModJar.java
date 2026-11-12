package horizonneo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * A mod jar, as far as the converter looks before the licence check: which loaders' metadata it holds,
 * the NeoForge metadata's text, the manifest's version, and the names of the entries that look like
 * licence files. No class is read here.
 *
 * modsToml is null without a META-INF/neoforge.mods.toml; jarVersion is null without an
 * Implementation-Version in the manifest.
 */
record ModJar(Path path, boolean neoforge, boolean fabric, boolean legacyForge, String modsToml, String jarVersion,
              List<String> licenceFiles) {

    /** Fabric's metadata file, and the one Forge and NeoForge before 20.5 use. */
    static final String FABRIC = "fabric.mod.json", LEGACY_FORGE = "META-INF/mods.toml";

    /** The most a metadata or licence entry may unpack to: a zip's sizes are its own claim. */
    static final int MAX_TEXT = 1 << 20;

    ModJar {
        licenceFiles = List.copyOf(licenceFiles);
    }

    /** The jar's file name, for messages. */
    String name() {
        return path.getFileName().toString();
    }

    static ModJar open(Path path) throws IOException {
        if (!Files.isRegularFile(path)) throw new IOException(path + " isn't a file");
        try (ZipFile zip = new ZipFile(path.toFile())) {
            String modsToml = null, jarVersion = null;
            ZipEntry toml = zip.getEntry(ModsToml.PATH);
            if (toml != null) modsToml = new String(read(zip, toml, MAX_TEXT), StandardCharsets.UTF_8);
            ZipEntry manifest = zip.getEntry("META-INF/MANIFEST.MF");
            if (manifest != null) {
                jarVersion = new Manifest(new ByteArrayInputStream(read(zip, manifest, MAX_TEXT))).getMainAttributes().getValue("Implementation-Version");
            }
            List<String> licences = new ArrayList<>();
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (!e.isDirectory() && licenceLike(e.getName())) licences.add(e.getName());
            }
            return new ModJar(path, toml != null, zip.getEntry(FABRIC) != null, zip.getEntry(LEGACY_FORGE) != null,
                    modsToml, jarVersion, licences);
        } catch (ZipException e) {
            throw new IOException(path.getFileName() + " isn't a jar (a zip file): " + e.getMessage());
        }
    }

    /**
     * Whether an entry's name says it may be a licence: LICENSE, LICENCE, COPYING, UNLICENSE... anywhere
     * in the jar. This errs on the wide side: horizon-port decides which of them count.
     */
    static boolean licenceLike(String entry) {
        if (entry.startsWith("/") || entry.contains("..") || entry.contains("\\")) return false;
        String base = entry.substring(entry.lastIndexOf('/') + 1).toUpperCase(Locale.ROOT);
        return base.contains("LICEN") || base.startsWith("COPYING");
    }

    /** All of an entry, refused when it unpacks to more than `cap` bytes. */
    static byte[] read(ZipFile zip, ZipEntry entry, int cap) throws IOException {
        try (InputStream in = zip.getInputStream(entry)) {
            byte[] bytes = in.readNBytes(cap + 1);
            if (bytes.length > cap) throw new IOException(entry.getName() + " unpacks to more than " + (cap >> 20) + " MiB: refused");
            return bytes;
        }
    }
}
