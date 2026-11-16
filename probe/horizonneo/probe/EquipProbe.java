package horizonneo.probe;

import net.minecraft.world.entity.EquipmentSlot;

/** The game's EquipmentSlot, as GameProbe observes the rest (see there). */
final class EquipProbe {
    private EquipProbe() {}

    static void run() {
        p("slot values", () -> EquipmentSlot.values().length + " " + EquipmentSlot.Type.values().length + " " + EquipmentSlot.NO_COUNT_LIMIT);
        for (EquipmentSlot s : EquipmentSlot.values()) {
            p("slot " + s.ordinal(), () -> s.name() + " " + s + " " + s.getType() + " " + s.getType().ordinal() + " " + s.getIndex() + " " + s.getIndex(100)
                    + " " + s.getFilterFlag() + " " + s.getName() + " " + s.isArmor() + " " + s.getSerializedName() + " " + (EquipmentSlot.byName(s.getName()) == s));
        }
        for (String name : new String[] {"mainhand", "MAINHAND", "head", "body", "nope", ""}) p("slot byName " + name, () -> EquipmentSlot.byName(name));
        p("slot byName null", () -> EquipmentSlot.byName(null));
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
