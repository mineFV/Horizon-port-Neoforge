package net.neoforged.neoforge.capabilities;

import net.minecraft.resources.ResourceLocation;

/** The stand-in for NeoForge's BaseCapability: a capability's name, and the classes of what it gives and of its context. */
public abstract class BaseCapability<T, C> {
    private final ResourceLocation name;
    private final Class<T> typeClass;
    private final Class<C> contextClass;

    protected BaseCapability(ResourceLocation name, Class<T> typeClass, Class<C> contextClass) {
        this.name = name;
        this.typeClass = typeClass;
        this.contextClass = contextClass;
    }

    public final ResourceLocation name() {
        return name;
    }

    public final Class<T> typeClass() {
        return typeClass;
    }

    public final Class<C> contextClass() {
        return contextClass;
    }
}
