package net.minecraft.core;

import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.HorizonRegistries;

/**
 * Not the game's: a registry of BuiltInRegistries. An id's value is the mod's registration, else the game's (items,
 * blocks, fluids and component types, as given: HorizonRegistries); a value's id the one it was registered under. A
 * defaulted registry gives its default for an unknown id and value. The game's values of the other registries (entity
 * types, menus...) aren't modelled, nor the whole of a registry (its keys, entries, size, numbers): asking says so. A
 * registry the mod made holds its registrations alone (another mod registering in it isn't modelled).
 */
public final class HorizonRegistry<T> implements DefaultedRegistry<T> {
    private final ResourceKey<?> key;
    private final String kind;
    private final ResourceLocation defaultKey;
    /** A registry the mod made (RegistryBuilder): what it holds is the mod's registrations alone. */
    private final boolean own;
    /** Whether its values make their holders themselves (RegistryBuilder.withIntrusiveHolders). */
    private final boolean intrusive;

    public HorizonRegistry(ResourceKey<?> key, ResourceLocation defaultKey) {
        this(key, defaultKey, false);
    }

    public HorizonRegistry(ResourceKey<?> key, ResourceLocation defaultKey, boolean own) {
        this(key, defaultKey, own, false);
    }

    public HorizonRegistry(ResourceKey<?> key, ResourceLocation defaultKey, boolean own, boolean intrusive) {
        this.key = key;
        this.kind = key.location().getPath();
        this.defaultKey = defaultKey;
        this.own = own;
        this.intrusive = intrusive;
    }

    /**
     * Observed: a value's holder, made before it is registered and bound to its key then (HorizonRegistries.get); only
     * a registry of the mod's made with intrusive holders makes them here.
     */
    @Override
    public Holder.Reference<T> createIntrusiveHolder(T value) {
        if (!own) throw new UnsupportedOperationException("intrusive holders of the game's registry " + key.location() + " aren't modelled (horizon-port-neoforge)");
        if (!intrusive) throw new IllegalStateException("This registry can't create intrusive holders");
        return HorizonRegistries.intrusive(value, this);
    }

    @Override
    public ResourceKey<?> key() {
        return key;
    }

    /** The value of an id, without the default; null when there is none. */
    @SuppressWarnings("unchecked")
    private T find(ResourceLocation id) {
        if (id == null) return null;
        Object own = HorizonRegistries.get(ResourceKey.create(key, id));
        if (own != null) return (T) own;
        if (this.own) return null;
        String text = id.toString();
        return (T) switch (kind) {
            case "item" -> HorizonRegistries.knownItem(text);
            case "block" -> HorizonRegistries.knownBlock(text);
            case "fluid" -> HorizonRegistries.knownFluid(text);
            case "data_component_type" -> net.minecraft.core.component.HorizonComponentCodecs.type(id);
            default -> throw new UnsupportedOperationException("the game's values of the registry " + key.location() + " aren't modelled (horizon-port-neoforge)");
        };
    }

    private UnsupportedOperationException whole() {
        return new UnsupportedOperationException("the whole of the registry " + key.location() + " isn't modelled (horizon-port-neoforge)");
    }

    @Override
    public T get(ResourceLocation id) {
        T found = find(id);
        return found != null || defaultKey == null ? found : find(defaultKey);
    }

    /** Observed: an unknown id gives nothing, even in a defaulted registry. */
    @Override
    public Optional<T> getOptional(ResourceLocation id) {
        return Optional.ofNullable(find(id));
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(ResourceKey<T> key) {
        return key == null || !key.isFor((ResourceKey<?>) this.key) ? null : find(key.location());
    }

    @Override
    public ResourceLocation getKey(T value) {
        ResourceLocation id = registered(value);
        return id != null ? id : defaultKey;
    }

    /** The id a value of this registry was registered under; null when it isn't one. */
    private ResourceLocation registered(T value) {
        ResourceLocation id = HorizonRegistries.idOf(value);
        return id != null && find(id) == value ? id : null;
    }

    @Override
    public Optional<ResourceKey<T>> getResourceKey(T value) {
        ResourceLocation id = registered(value);
        return id == null ? Optional.empty() : Optional.of(ResourceKey.create(key, id));
    }

    @Override
    public boolean containsValue(T value) {
        return registered(value) != null;
    }

    @Override
    public boolean containsKey(ResourceLocation id) {
        return find(id) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsKey(ResourceKey<T> key) {
        return key.isFor((ResourceKey<?>) this.key) && find(key.location()) != null;
    }

    @Override
    public Optional<Holder.Reference<T>> getHolder(ResourceLocation id) {
        T value = find(id);
        return value == null ? Optional.empty() : Optional.of(holder(value, id));
    }

    @Override
    public Optional<Holder.Reference<T>> getHolder(ResourceKey<T> key) {
        return getHolder(key.location());
    }

    @Override
    public Holder<T> wrapAsHolder(T value) {
        ResourceLocation id = registered(value);
        if (id == null) throw new UnsupportedOperationException("a holder of a value the registry " + key.location() + " doesn't hold isn't modelled (horizon-port-neoforge)");
        return holder(value, id);
    }

    @SuppressWarnings("unchecked")
    private Holder.Reference<T> holder(T value, ResourceLocation id) {
        if (value instanceof net.minecraft.world.item.Item item) return (Holder.Reference<T>) item.builtInRegistryHolder();
        if (value instanceof net.minecraft.world.level.block.Block block) return (Holder.Reference<T>) block.builtInRegistryHolder();
        if (value instanceof net.minecraft.world.level.material.Fluid fluid) return (Holder.Reference<T>) fluid.builtInRegistryHolder();
        return HorizonRegistries.holder(ResourceKey.create(key, id), value);
    }

    @Override
    public ResourceLocation getDefaultKey() {
        return defaultKey;
    }

    @Override
    public java.util.Set<ResourceLocation> keySet() {
        throw whole();
    }

    @Override
    public java.util.Set<java.util.Map.Entry<ResourceKey<T>, T>> entrySet() {
        throw whole();
    }

    @Override
    public java.util.stream.Stream<Holder.Reference<T>> holders() {
        throw whole();
    }

    @Override
    public java.util.Iterator<T> iterator() {
        throw whole();
    }

    @Override
    public int getId(T value) {
        throw whole();
    }

    @Override
    public T byId(int id) {
        throw whole();
    }

    @Override
    public int size() {
        throw whole();
    }

    /** Observed: "Registry[" its key " (" its lifecycle ")]". */
    @Override
    public String toString() {
        return "Registry[" + key + " (Stable)]";
    }
}
