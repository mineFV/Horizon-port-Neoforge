package net.minecraft.resources;

import net.minecraft.ResourceLocationException;

/**
 * The stand-in for the game's ResourceLocation (an id, "namespace:path"), observed (`standins`, probe/IdProbe.java):
 * which characters each part takes, how text is split, the errors and their messages, its hash, its two orders and
 * its language keys.
 */
public final class ResourceLocation implements Comparable<ResourceLocation> {
    public static final char NAMESPACE_SEPARATOR = ':';
    public static final String DEFAULT_NAMESPACE = "minecraft";
    public static final String REALMS_NAMESPACE = "realms";

    private final String namespace;
    private final String path;

    private ResourceLocation(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    /** Observed: the namespace checked first, then the path; either throws, the message naming the whole id. */
    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        return new ResourceLocation(validNamespace(namespace, path), validPath(namespace, path));
    }

    /** Observed: split at the first ':', no namespace (or an empty one) being "minecraft". */
    public static ResourceLocation parse(String text) {
        int i = text.indexOf(':');
        if (i < 0) return withDefaultNamespace(text);
        return fromNamespaceAndPath(i == 0 ? DEFAULT_NAMESPACE : text.substring(0, i), text.substring(i + 1));
    }

    public static ResourceLocation withDefaultNamespace(String path) {
        return new ResourceLocation(DEFAULT_NAMESPACE, validPath(DEFAULT_NAMESPACE, path));
    }

    /** Observed: as parse, null rather than an error. */
    public static ResourceLocation tryParse(String text) {
        int i = text.indexOf(':');
        if (i < 0) return tryBuild(DEFAULT_NAMESPACE, text);
        return tryBuild(i == 0 ? DEFAULT_NAMESPACE : text.substring(0, i), text.substring(i + 1));
    }

    public static ResourceLocation tryBuild(String namespace, String path) {
        return isValidNamespace(namespace) && isValidPath(path) ? new ResourceLocation(namespace, path) : null;
    }

    public String getPath() {
        return path;
    }

    public String getNamespace() {
        return namespace;
    }

    /** Observed: a new id each time, the path checked. */
    public ResourceLocation withPath(String path) {
        return new ResourceLocation(namespace, validPath(namespace, path));
    }

    public ResourceLocation withPrefix(String prefix) {
        return withPath(prefix + path);
    }

    public ResourceLocation withSuffix(String suffix) {
        return withPath(path + suffix);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ResourceLocation other && namespace.equals(other.namespace) && path.equals(other.path);
    }

    /** Observed: 31 times the namespace's hash, plus the path's. */
    @Override
    public int hashCode() {
        return 31 * namespace.hashCode() + path.hashCode();
    }

    /** Observed: the paths' order first, then the namespaces'. */
    @Override
    public int compareTo(ResourceLocation other) {
        int c = path.compareTo(other.path);
        return c != 0 ? c : namespace.compareTo(other.namespace);
    }

    /** Observed: the namespaces' order first, then the paths'. */
    public int compareNamespaced(ResourceLocation other) {
        int c = namespace.compareTo(other.namespace);
        return c != 0 ? c : path.compareTo(other.path);
    }

    /** Observed: the id with '/' and ':' as '_'. */
    public String toDebugFileName() {
        return toString().replace('/', '_').replace(':', '_');
    }

    public String toLanguageKey() {
        return namespace + "." + path;
    }

    /** Observed: the path alone for "minecraft". */
    public String toShortLanguageKey() {
        return namespace.equals(DEFAULT_NAMESPACE) ? path : toLanguageKey();
    }

    public String toLanguageKey(String prefix) {
        return prefix + "." + toLanguageKey();
    }

    public String toLanguageKey(String prefix, String suffix) {
        return prefix + "." + toLanguageKey() + "." + suffix;
    }

    /** Observed: what a path, a namespace or the separator may hold: a-z, 0-9, _ - . / and ':'. */
    public static boolean isAllowedInResourceLocation(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c == '_' || c == ':' || c == '/' || c == '.' || c == '-';
    }

    public static boolean isValidPath(String path) {
        for (int i = 0; i < path.length(); i++) if (!validPathChar(path.charAt(i))) return false;
        return true;
    }

    public static boolean isValidNamespace(String namespace) {
        for (int i = 0; i < namespace.length(); i++) if (!validNamespaceChar(namespace.charAt(i))) return false;
        return true;
    }

    public static boolean validPathChar(char c) {
        return c == '_' || c == '-' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '/' || c == '.';
    }

    public static boolean validNamespaceChar(char c) {
        return c == '_' || c == '-' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '.';
    }

    private static String validNamespace(String namespace, String path) {
        if (!isValidNamespace(namespace)) throw new ResourceLocationException("Non [a-z0-9_.-] character in namespace of location: " + escaped(namespace + ":" + path));
        return namespace;
    }

    private static String validPath(String namespace, String path) {
        if (!isValidPath(path)) throw new ResourceLocationException("Non [a-z0-9/._-] character in path of location: " + escaped(namespace + ":" + path));
        return path;
    }

    /**
     * Observed in the messages: a backslash, a double quote and \b \t \n \f \r escaped as in Java's source, any other
     * character below 32 or above 127 as backslash-u and four upper-case hex digits (each half of a surrogate pair).
     */
    private static String escaped(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> b.append("\\\\");
                case '"' -> b.append("\\\"");
                case '\b' -> b.append("\\b");
                case '\t' -> b.append("\\t");
                case '\n' -> b.append("\\n");
                case '\f' -> b.append("\\f");
                case '\r' -> b.append("\\r");
                default -> {
                    if (c < 32 || c > 127) {
                        String hex = Integer.toHexString(c).toUpperCase(java.util.Locale.ROOT);
                        b.append("\\u");
                        for (int k = hex.length(); k < 4; k++) b.append('0');
                        b.append(hex);
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.toString();
    }
}
