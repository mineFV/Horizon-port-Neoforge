package horizonneo;

import horizonneo.Analysis.MixinClass;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * The jar horizon-port is handed in place of a NeoForge mod's, so that it can be run unchanged.
 *
 * horizon-port reads a Fabric jar: the mod and its Mixin configs from fabric.mod.json, then every
 * class. This writes a copy of the mod's jar that reads that way: the mod's classes and resources
 * as they are, plus a fabric.mod.json written from the mod's own metadata. Four things differ from
 * the mod's jar, each because the copy would otherwise say something the mod doesn't:
 *
 * - Mixin configs are written again as plain JSON. Mixin reads them leniently (comments, trailing
 *   commas); horizon-port doesn't, and stops on them.
 * - A Mixin class no config lists is left out. Mixin never applies it, but horizon-port converts
 *   every class with `@Mixin`, listed or not.
 * - Jars inside the jar are left out: they are other people's libraries, under licences of their own.
 * - A fabric.mod.json of the mod's own (a jar made for both loaders) is replaced.
 *
 * The copy lives in a temporary folder and is deleted when the conversion ends.
 */
final class StandIn {
    private StandIn() {}

    /** The most the copy may unpack to. */
    static final long MAX_BYTES = 2L << 30;

    /** files: how many entries were written. leftOut: what wasn't, and why, in words. */
    record Written(int files, List<String> leftOut) {
        Written {
            leftOut = List.copyOf(leftOut);
        }
    }

    static Written write(ModJar jar, ModInfo info, Analysis analysis, Path target) throws IOException {
        Set<String> unlisted = new HashSet<>();
        for (MixinClass m : analysis.mixins()) if (m.config() == null) unlisted.add(m.className() + ".class");
        Set<String> configs = new HashSet<>(info.mixinConfigs());
        List<String> readable = new ArrayList<>(), nested = new ArrayList<>(), dropped = new ArrayList<>(), unsafe = new ArrayList<>();
        Set<String> written = new HashSet<>(Set.of(ModJar.FABRIC));
        int files = 0;
        long left = MAX_BYTES;
        try (ZipFile original = new ZipFile(jar.path().toFile());
             OutputStream file = Files.newOutputStream(target);
             ZipOutputStream zip = new ZipOutputStream(file)) {
            Enumeration<? extends ZipEntry> entries = original.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (e.isDirectory() || name.equals(ModJar.FABRIC) || signature(name)) continue;
                if (name.startsWith("/") || name.contains("\\") || List.of(name.split("/")).contains("..")) {
                    unsafe.add(name);
                    continue;
                }
                if (name.endsWith(".jar")) {
                    nested.add(name);
                    continue;
                }
                if (unlisted.contains(name)) {
                    dropped.add(ClassData.simple(name.substring(0, name.length() - ".class".length())));
                    continue;
                }
                if (!written.add(name)) continue;
                if (configs.contains(name)) {
                    String plain = plainJson(new String(ModJar.read(original, e, ModJar.MAX_TEXT), StandardCharsets.UTF_8));
                    // One this tool can't read isn't handed over: the analysis has said so already.
                    if (plain == null) continue;
                    readable.add(name);
                    put(zip, name, plain.getBytes(StandardCharsets.UTF_8));
                    files++;
                    continue;
                }
                zip.putNextEntry(new ZipEntry(name));
                try (InputStream in = original.getInputStream(e)) {
                    byte[] buffer = new byte[1 << 16];
                    for (int n; (n = in.read(buffer)) > 0; ) {
                        left -= n;
                        if (left < 0) throw new IOException(jar.name() + " unpacks to more than 2 GiB: refused");
                        zip.write(buffer, 0, n);
                    }
                }
                zip.closeEntry();
                files++;
            }
            put(zip, ModJar.FABRIC, Json.write(metadata(info, readable)).getBytes(StandardCharsets.UTF_8));
        }
        List<String> leftOut = new ArrayList<>();
        dropped.sort(null);
        if (!dropped.isEmpty()) {
            leftOut.add((dropped.size() == 1 ? "the Mixin class " : "the Mixin classes ") + String.join(", ", dropped)
                    + ": no Mixin config lists " + (dropped.size() == 1 ? "it" : "them") + ", so Mixin never applies " + (dropped.size() == 1 ? "it" : "them"));
        }
        if (!nested.isEmpty()) {
            leftOut.add(nested.size() + (nested.size() == 1 ? " jar inside the jar (" : " jars inside the jar (") + first(nested)
                    + "): other people's code, under a licence of its own");
        }
        if (!unsafe.isEmpty()) {
            leftOut.add(unsafe.size() + (unsafe.size() == 1 ? " entry whose name leads" : " entries whose names lead") + " out of the jar (" + first(unsafe) + ")");
        }
        return new Written(files, leftOut);
    }

    /** What the mod declares, in the shape horizon-port reads, with the Mixin configs that were handed over. */
    static Map<String, Object> metadata(ModInfo info, List<String> mixinConfigs) {
        Map<String, Object> m = LicenceCheck.fabricMetadata(info);
        if (!info.description().isBlank()) m.put("description", info.description().strip());
        m.put("mixins", mixinConfigs);
        return m;
    }

    /** A Mixin config as plain JSON, or null when it isn't JSON this tool can read. */
    static String plainJson(String text) {
        try {
            Object parsed = Json.parse(text);
            return parsed instanceof Map<?, ?> ? Json.write(parsed) : null;
        } catch (Json.Invalid e) {
            return null;
        }
    }

    /** A jar signature's files: they sign the mod's jar, not this copy. */
    private static boolean signature(String name) {
        if (!name.startsWith("META-INF/") || name.indexOf('/', "META-INF/".length()) >= 0) return false;
        String upper = name.toUpperCase(java.util.Locale.ROOT);
        return upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC");
    }

    private static String first(List<String> names) {
        return String.join(", ", names.subList(0, Math.min(3, names.size()))) + (names.size() > 3 ? ", ..." : "");
    }

    private static void put(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }
}
