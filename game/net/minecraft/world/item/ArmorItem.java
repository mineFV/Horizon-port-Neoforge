package net.minecraft.world.item;

/**
 * The stand-in for the game's ArmorItem: its Type (observed in a server). An armour item itself isn't modelled: no
 * constructor of the game's is here, so a mod's armour isn't made.
 */
public class ArmorItem extends Item implements Equipable {
    private ArmorItem() {
        super(null);
    }

    /** The stand-in for ArmorItem.Type: the pieces of armour, their names, slots and durability (observed). */
    public enum Type implements net.minecraft.util.StringRepresentable {
        HELMET(net.minecraft.world.entity.EquipmentSlot.HEAD, 11, "helmet"),
        CHESTPLATE(net.minecraft.world.entity.EquipmentSlot.CHEST, 16, "chestplate"),
        LEGGINGS(net.minecraft.world.entity.EquipmentSlot.LEGS, 15, "leggings"),
        BOOTS(net.minecraft.world.entity.EquipmentSlot.FEET, 13, "boots"),
        BODY(net.minecraft.world.entity.EquipmentSlot.BODY, 16, "body");

        private final net.minecraft.world.entity.EquipmentSlot slot;
        private final int unitDurability;
        private final String name;

        Type(net.minecraft.world.entity.EquipmentSlot slot, int unitDurability, String name) {
            this.slot = slot;
            this.unitDurability = unitDurability;
            this.name = name;
        }

        public int getDurability(int multiplier) {
            return unitDurability * multiplier;
        }

        public net.minecraft.world.entity.EquipmentSlot getSlot() {
            return slot;
        }

        public String getName() {
            return name;
        }

        /** Observed: every piece but an animal's body armour. */
        public boolean hasTrims() {
            return this != BODY;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
