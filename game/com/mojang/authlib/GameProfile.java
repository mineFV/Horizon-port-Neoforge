package com.mojang.authlib;

import java.util.Objects;
import java.util.UUID;

/**
 * The stand-in for authlib's GameProfile: an id and a name, both required (observed, `standins`). Its properties (a
 * Guava multimap) aren't modelled: its text shows them empty, as a new profile's are.
 */
public class GameProfile {
    private final UUID id;
    private final String name;

    public GameProfile(UUID id, String name) {
        this.id = Objects.requireNonNull(id, "Profile ID must not be null");
        this.name = Objects.requireNonNull(name, "Profile name must not be null");
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GameProfile p && id.equals(p.id) && name.equals(p.name);
    }

    @Override
    public int hashCode() {
        return 31 * id.hashCode() + name.hashCode();
    }

    /** Observed: its class, "@" and its identity hash in hex (which changes from run to run), then its fields. */
    @Override
    public String toString() {
        return "com.mojang.authlib.GameProfile@" + Integer.toHexString(System.identityHashCode(this)) + "[id=" + id + ",name=" + name + ",properties={}]";
    }
}
