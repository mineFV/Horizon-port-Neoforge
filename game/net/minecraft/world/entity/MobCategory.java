package net.minecraft.world.entity;

/** The stand-in for the game's MobCategory: its constants and their numbers (observed, EntityTypeProbe). */
public enum MobCategory implements net.minecraft.util.StringRepresentable, net.neoforged.fml.common.asm.enumextension.IExtensibleEnum {
    MONSTER("monster", 70, false, false, 128),
    CREATURE("creature", 10, true, true, 128),
    AMBIENT("ambient", 15, true, false, 128),
    AXOLOTLS("axolotls", 5, true, false, 128),
    UNDERGROUND_WATER_CREATURE("underground_water_creature", 5, true, false, 128),
    WATER_CREATURE("water_creature", 5, true, false, 128),
    WATER_AMBIENT("water_ambient", 20, true, false, 64),
    MISC("misc", -1, true, true, 128);

    private final String name;
    private final int max;
    private final boolean isFriendly;
    private final boolean isPersistent;
    private final int despawnDistance;

    MobCategory(String name, int max, boolean isFriendly, boolean isPersistent, int despawnDistance) {
        this.name = name;
        this.max = max;
        this.isFriendly = isFriendly;
        this.isPersistent = isPersistent;
        this.despawnDistance = despawnDistance;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public int getMaxInstancesPerChunk() {
        return max;
    }

    public boolean isFriendly() {
        return isFriendly;
    }

    public boolean isPersistent() {
        return isPersistent;
    }

    public int getDespawnDistance() {
        return despawnDistance;
    }

    public int getNoDespawnDistance() {
        return 32;
    }
}
