package net.minecraft.tags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Not the game's: which values each tag holds, as `dump` read them (HorizonRegistries.known: "tags_item",
 * "tags_block", "tags_fluid", "namespaces"). The facts list every tag that holds one of the mod's values, with those of
 * its members, and the "c" tags and the mod's own whole: so a mod's value's tags are known, and whether a value is in
 * such a tag. Fluids' tags are given whole. What isn't known says so.
 */
public final class HorizonTags {
    private HorizonTags() {}

    private static final Map<String, Map<String, Set<String>>> TAGS = new HashMap<>();
    private static final Set<String> WHOLE = new HashSet<>();
    private static final Set<String> OURS = new HashSet<>();

    /** A registry's tags ("tag=member,member;..."), and whether they are given whole. Not the game's API. */
    public static synchronized void given(String registry, String text, boolean whole) {
        Map<String, Set<String>> tags = new TreeMap<>();
        for (String entry : text.split(";")) {
            int eq = entry.indexOf('=');
            if (eq <= 0) continue;
            Set<String> members = new HashSet<>();
            for (String m : entry.substring(eq + 1).split(",")) if (!m.isEmpty()) members.add(m);
            tags.put(entry.substring(0, eq), members);
        }
        TAGS.put(registry, tags);
        if (whole) WHOLE.add(registry);
    }

    /** The mod's namespaces ("a,b"). Not the game's API. */
    public static synchronized void namespaces(String text) {
        for (String n : text.split(",")) if (!n.isEmpty()) OURS.add(n);
    }

    private static String registry(ResourceKey<?> key) {
        ResourceLocation r = key.registry();
        return r.getNamespace().equals("minecraft") ? r.getPath() : r.toString();
    }

    /** Whether the value of a key is in a tag. */
    public static synchronized boolean is(ResourceKey<?> key, TagKey<?> tag) {
        String registry = registry(key);
        Map<String, Set<String>> tags = TAGS.get(registry);
        if (tags == null) throw new IllegalStateException("the tags of " + registry + " weren't given (horizon-port-neoforge)");
        Set<String> members = tags.get(tag.location().toString());
        String member = key.location().toString();
        if (members != null && members.contains(member)) return true;
        if (WHOLE.contains(registry) || OURS.contains(key.location().getNamespace()) || tag.location().getNamespace().equals("c")
                || OURS.contains(tag.location().getNamespace())) return false;
        throw new IllegalStateException("whether " + member + " is in " + tag.location() + " isn't known: the game's own tags aren't all given (horizon-port-neoforge)");
    }

    /** The tags a key's value is in, by their ids' order (the game's order changes from run to run). */
    public static synchronized List<TagKey<?>> tags(ResourceKey<?> key) {
        String registry = registry(key);
        Map<String, Set<String>> tags = TAGS.get(registry);
        if (tags == null) throw new IllegalStateException("the tags of " + registry + " weren't given (horizon-port-neoforge)");
        if (!WHOLE.contains(registry) && !OURS.contains(key.location().getNamespace())) {
            throw new IllegalStateException("the tags of " + key.location() + " aren't known: the game's own tags aren't all given (horizon-port-neoforge)");
        }
        ResourceKey<?> registryKey = ResourceKey.createRegistryKey(key.registry());
        List<TagKey<?>> out = new ArrayList<>();
        String member = key.location().toString();
        for (Map.Entry<String, Set<String>> e : tags.entrySet()) {
            if (e.getValue().contains(member)) out.add(TagKey.create(registryKey, ResourceLocation.parse(e.getKey())));
        }
        return out;
    }
}
