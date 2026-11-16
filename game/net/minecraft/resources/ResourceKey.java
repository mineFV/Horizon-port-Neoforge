package net.minecraft.resources;

import java.util.HashMap;
import java.util.Map;

/** The stand-in for the game's ResourceKey: an id in a registry, one object for each pair (the game interns them). */
public class ResourceKey<T> implements Comparable<ResourceKey<?>> {
    private static final Map<String, ResourceKey<?>> KEYS = new HashMap<>();

    private final ResourceLocation registryName;
    private final ResourceLocation location;

    private ResourceKey(ResourceLocation registryName, ResourceLocation location) {
        this.registryName = registryName;
        this.location = location;
    }

    public static <T> ResourceKey<T> create(ResourceKey<?> registryKey, ResourceLocation location) {
        return create(registryKey.location, location);
    }

    public static <T> ResourceKey<T> createRegistryKey(ResourceLocation registryName) {
        return create(net.minecraft.core.registries.Registries.ROOT_REGISTRY_NAME, registryName);
    }

    @SuppressWarnings("unchecked")
    private static synchronized <T> ResourceKey<T> create(ResourceLocation registryName, ResourceLocation location) {
        String key = registryName + " " + location;
        ResourceKey<?> found = KEYS.get(key);
        if (found == null) {
            found = new ResourceKey<>(registryName, location);
            KEYS.put(key, found);
        }
        return (ResourceKey<T>) found;
    }

    @Override
    public String toString() {
        return "ResourceKey[" + registryName + " / " + location + "]";
    }

    public boolean isFor(ResourceKey<?> registryKey) {
        return registryName.equals(registryKey.location());
    }

    public ResourceLocation location() {
        return location;
    }

    public ResourceLocation registry() {
        return registryName;
    }

    @Override
    public int compareTo(ResourceKey<?> other) {
        int c = registryName.compareTo(other.registryName);
        return c != 0 ? c : location.compareTo(other.location);
    }
}
