package net.minecraft.world.entity;

import net.minecraft.util.StringRepresentable;

/**
 * The stand-in for the game's EquipmentSlot, observed (`standins`, probe/EquipProbe.java): each slot's kind, index,
 * filter flag and name. limit(ItemStack) isn't here: there is no ItemStack yet.
 */
public enum EquipmentSlot implements StringRepresentable {
    MAINHAND(Type.HAND, 0, 0, "mainhand"),
    OFFHAND(Type.HAND, 1, 5, "offhand"),
    FEET(Type.HUMANOID_ARMOR, 0, 1, "feet"),
    LEGS(Type.HUMANOID_ARMOR, 1, 2, "legs"),
    CHEST(Type.HUMANOID_ARMOR, 2, 3, "chest"),
    HEAD(Type.HUMANOID_ARMOR, 3, 4, "head"),
    BODY(Type.ANIMAL_ARMOR, 0, 6, "body");

    public static final int NO_COUNT_LIMIT = 0;

    private final Type type;
    private final int index;
    private final int filterFlag;
    private final String name;

    EquipmentSlot(Type type, int index, int filterFlag, String name) {
        this.type = type;
        this.index = index;
        this.filterFlag = filterFlag;
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    /** Observed: the base plus this slot's index. */
    public int getIndex(int base) {
        return base + index;
    }

    public int getFilterFlag() {
        return filterFlag;
    }

    public String getName() {
        return name;
    }

    /** Observed: a slot for armour, a humanoid's or an animal's. */
    public boolean isArmor() {
        return type == Type.HUMANOID_ARMOR || type == Type.ANIMAL_ARMOR;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** Observed: the slot of that lower-case name, else an IllegalArgumentException quoting it (null included). */
    public static EquipmentSlot byName(String name) {
        for (EquipmentSlot s : values()) if (s.name.equals(name)) return s;
        throw new IllegalArgumentException("Invalid slot '" + name + "'");
    }

    /** The stand-in for EquipmentSlot.Type. */
    public enum Type {
        HAND,
        HUMANOID_ARMOR,
        ANIMAL_ARMOR
    }
}
