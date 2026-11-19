package horizonneo.probe;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;

/** The pieces of armour, and the game's armour materials a mod names. */
final class ArmorProbe {
    private ArmorProbe() {}

    static void run() {
        for (ArmorItem.Type t : ArmorItem.Type.values()) {
            p("armor type " + t.ordinal(), () -> t + " " + t.name() + " " + t.getName() + " " + t.getSerializedName() + " " + t.getSlot() + " " + t.hasTrims() + " " + t.getDurability(37));
        }
        p("armor netherite", () -> {
            ArmorMaterial m = ArmorMaterials.NETHERITE.value();
            StringBuilder b = new StringBuilder().append(ArmorMaterials.NETHERITE.unwrapKey().orElseThrow().location()).append(' ').append(m.defense()).append(' ');
            for (ArmorItem.Type t : ArmorItem.Type.values()) b.append(m.getDefense(t)).append(',');
            return b.append(' ').append(m.enchantmentValue()).append(' ').append(m.equipSound().unwrapKey().orElseThrow().location()).append(' ').append(m.equipSound().value().getLocation())
                    .append(' ').append(m.toughness()).append(' ').append(m.knockbackResistance()).append(' ').append(m.layers().size()).append(' ').append(m.layers().get(0).dyeable())
                    .append(' ').append(m.repairIngredient() != null).append(' ').append(m.equals(m));
        });
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
