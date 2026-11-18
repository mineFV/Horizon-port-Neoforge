package horizonneo.probe;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** The game's entity types: their builders' defaults and values, ids, descriptions and texts (in a server). */
final class EntityTypeProbe {
    private EntityTypeProbe() {}

    /** Registered while the mod loads (InServer): a type made after, the game refuses (its registry is frozen). */
    static final net.neoforged.neoforge.registries.DeferredRegister<EntityType<?>> ENTITIES = net.neoforged.neoforge.registries.DeferredRegister.create(
            net.minecraft.core.registries.Registries.ENTITY_TYPE, "horizonprobe");
    static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<net.minecraft.world.entity.Entity>> THING = ENTITIES.register("thing",
            () -> EntityType.Builder.<net.minecraft.world.entity.Entity>of(null, MobCategory.MONSTER).build("thing"));
    static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<net.minecraft.world.entity.Entity>> OTHER = ENTITIES.register("other",
            () -> EntityType.Builder.<net.minecraft.world.entity.Entity>of(null, MobCategory.CREATURE).sized(1.5F, 0.25F).eyeHeight(0.1F).fireImmune().noSummon().noSave()
                    .clientTrackingRange(4).updateInterval(7).canSpawnFarFromPlayer().build("other"));

    static void run() {
        for (MobCategory c : MobCategory.values()) {
            p("mob category " + c.ordinal(), () -> c + " " + c.getName() + " " + c.getSerializedName() + " " + c.getMaxInstancesPerChunk() + " " + c.isFriendly() + " "
                    + c.isPersistent() + " " + c.getDespawnDistance() + " " + c.getNoDespawnDistance());
        }
        p("entity type armor_stand", () -> describe(EntityType.ARMOR_STAND));
        p("entity type item", () -> describe(EntityType.ITEM));
        p("entity type lightning_bolt", () -> describe(EntityType.LIGHTNING_BOLT));
        p("entity type player", () -> describe(EntityType.PLAYER));
        p("entity type built", () -> describe(THING.get()));
        p("entity type built set", () -> describe(OTHER.get()) + " " + EntityType.getKey(OTHER.get()));
        p("entity type key", () -> EntityType.getKey(EntityType.ITEM) + " " + net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER) + " "
                + EntityType.ITEM.getBaseClass().getName());
    }

    private static String describe(EntityType<?> t) {
        return t.getCategory() + " " + t.canSerialize() + " " + t.canSummon() + " " + t.fireImmune() + " " + t.canSpawnFarFromPlayer() + " " + t.getWidth() + " " + t.getHeight()
                + " " + t.clientTrackingRange() + " " + t.updateInterval() + " " + t.getDescriptionId() + " " + t.getDescription() + " " + t + " " + t.toShortString();
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
