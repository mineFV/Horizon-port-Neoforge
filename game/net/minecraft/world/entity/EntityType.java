package net.minecraft.world.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The stand-in for the game's EntityType: what its Builder gives (category, size, ranges, whether it is saved,
 * summoned, fire immune), its id, description and tags (observed, EntityTypeProbe). The game's types a mod names are
 * here with their own; making an entity isn't modelled: it throws.
 */
public class EntityType<T extends Entity> implements net.minecraft.world.flag.FeatureElement, net.minecraft.world.level.entity.EntityTypeTest<Entity, T> {
    public static final EntityType<Entity> ARMOR_STAND = game("armor_stand", Builder.of(null, MobCategory.MISC).sized(0.5F, 1.975F).clientTrackingRange(10));
    public static final EntityType<Entity> ITEM = game("item", Builder.of(null, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(6).updateInterval(20));
    public static final EntityType<Entity> LIGHTNING_BOLT = game("lightning_bolt", Builder.of(null, MobCategory.MISC).noSave().sized(0.0F, 0.0F).clientTrackingRange(16)
            .updateInterval(Integer.MAX_VALUE));
    public static final EntityType<net.minecraft.world.entity.player.Player> PLAYER = game("player", Builder.<net.minecraft.world.entity.player.Player>of(null, MobCategory.MISC).noSave()
            .noSummon().sized(0.6F, 1.8F).clientTrackingRange(32).updateInterval(2));

    private final EntityFactory<T> factory;
    private final MobCategory category;
    private final boolean serialize;
    private final boolean summon;
    private final boolean fireImmune;
    private final boolean canSpawnFarFromPlayer;
    private final float width;
    private final float height;
    private final int clientTrackingRange;
    private final int updateInterval;
    private String descriptionId;

    private EntityType(Builder<T> b) {
        this.factory = b.factory;
        this.category = b.category;
        this.serialize = b.serialize;
        this.summon = b.summon;
        this.fireImmune = b.fireImmune;
        this.canSpawnFarFromPlayer = b.canSpawnFarFromPlayer;
        this.width = b.width;
        this.height = b.height;
        this.clientTrackingRange = b.clientTrackingRange;
        this.updateInterval = b.updateInterval;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> EntityType<T> game(String name, Builder<T> builder) {
        EntityType<T> type = new EntityType<>(builder);
        net.neoforged.neoforge.registries.HorizonRegistries.bind(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                ResourceLocation.withDefaultNamespace(name)), (Object) type);
        return type;
    }

    /** Its registered id; the registry's default, pig, when it isn't registered (observed). */
    public static ResourceLocation getKey(EntityType<?> type) {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

    public boolean canSerialize() {
        return serialize;
    }

    public boolean canSummon() {
        return summon;
    }

    public boolean fireImmune() {
        return fireImmune;
    }

    public boolean canSpawnFarFromPlayer() {
        return canSpawnFarFromPlayer;
    }

    public MobCategory getCategory() {
        return category;
    }

    public String getDescriptionId() {
        if (descriptionId == null) descriptionId = net.minecraft.Util.makeDescriptionId("entity", getKey(this));
        return descriptionId;
    }

    public Component getDescription() {
        return Component.translatable(getDescriptionId());
    }

    /** Observed: its description's key. */
    @Override
    public String toString() {
        return getDescriptionId();
    }

    public String toShortString() {
        return getKey(this).getPath();
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public int clientTrackingRange() {
        return clientTrackingRange;
    }

    public int updateInterval() {
        return updateInterval;
    }

    public T create(net.minecraft.world.level.Level level) {
        throw new UnsupportedOperationException("making an entity isn't modelled");
    }

    @Override
    public T tryCast(Entity entity) {
        throw new UnsupportedOperationException("entities aren't modelled");
    }

    @Override
    public Class<? extends Entity> getBaseClass() {
        return Entity.class;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private net.minecraft.resources.ResourceKey<?> horizonKey() {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, getKey(this));
    }

    public boolean is(net.minecraft.tags.TagKey<EntityType<?>> tag) {
        return net.minecraft.tags.HorizonTags.is(horizonKey(), tag);
    }

    public java.util.stream.Stream<net.minecraft.tags.TagKey<EntityType<?>>> getTags() {
        java.util.List<net.minecraft.tags.TagKey<EntityType<?>>> out = new java.util.ArrayList<>();
        for (net.minecraft.tags.TagKey<?> t : net.minecraft.tags.HorizonTags.tags(horizonKey())) out.add(cast(t));
        return out.stream();
    }

    @SuppressWarnings("unchecked")
    private static net.minecraft.tags.TagKey<EntityType<?>> cast(net.minecraft.tags.TagKey<?> t) {
        return (net.minecraft.tags.TagKey<EntityType<?>>) t;
    }

    /** The stand-in for EntityType.EntityFactory: what makes the type's entities. */
    public interface EntityFactory<T extends Entity> {
        T create(EntityType<T> type, net.minecraft.world.level.Level level);
    }

    /** The stand-in for EntityType.Builder: its defaults observed. */
    public static class Builder<T extends Entity> {
        private final EntityFactory<T> factory;
        private final MobCategory category;
        private boolean serialize = true;
        private boolean summon = true;
        private boolean fireImmune;
        private boolean canSpawnFarFromPlayer;
        private float width = 0.6F;
        private float height = 1.8F;
        private int clientTrackingRange = 5;
        private int updateInterval = 3;

        private Builder(EntityFactory<T> factory, MobCategory category) {
            this.factory = factory;
            this.category = category;
            this.canSpawnFarFromPlayer = category == MobCategory.CREATURE || category == MobCategory.MISC;
        }

        public static <T extends Entity> Builder<T> of(EntityFactory<T> factory, MobCategory category) {
            return new Builder<>(factory, category);
        }

        public Builder<T> sized(float width, float height) {
            this.width = width;
            this.height = height;
            return this;
        }

        /** Where its eyes are: not modelled, kept by nothing. */
        public Builder<T> eyeHeight(float eyeHeight) {
            return this;
        }

        public Builder<T> noSummon() {
            summon = false;
            return this;
        }

        public Builder<T> noSave() {
            serialize = false;
            return this;
        }

        public Builder<T> fireImmune() {
            fireImmune = true;
            return this;
        }

        public Builder<T> canSpawnFarFromPlayer() {
            canSpawnFarFromPlayer = true;
            return this;
        }

        public Builder<T> clientTrackingRange(int range) {
            clientTrackingRange = range;
            return this;
        }

        public Builder<T> updateInterval(int interval) {
            updateInterval = interval;
            return this;
        }

        public EntityType<T> build(String id) {
            return new EntityType<>(this);
        }
    }
}
