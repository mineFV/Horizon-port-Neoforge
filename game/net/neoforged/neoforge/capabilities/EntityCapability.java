package net.neoforged.neoforge.capabilities;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** The stand-in for NeoForge's EntityCapability: one for each name. Entities aren't modelled: nothing can be asked of one. */
public final class EntityCapability<T, C> extends BaseCapability<T, C> {
    private EntityCapability(ResourceLocation name, Class<T> typeClass, Class<C> contextClass) {
        super(name, typeClass, contextClass);
    }

    public static <T, C> EntityCapability<T, C> create(ResourceLocation name, Class<T> typeClass, Class<C> contextClass) {
        return HorizonCapabilities.made("entity", name, typeClass, contextClass, () -> new EntityCapability<>(name, typeClass, contextClass));
    }

    public static <T> EntityCapability<T, Void> createVoid(ResourceLocation name, Class<T> typeClass) {
        return create(name, typeClass, void.class);
    }

    public static <T> EntityCapability<T, Direction> createSided(ResourceLocation name, Class<T> typeClass) {
        return create(name, typeClass, Direction.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized List<EntityCapability<?, ?>> getAll() {
        return (List) HorizonCapabilities.all(EntityCapability.class);
    }
}
