package horizonneo.probe;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/** The game's attributes a mod names: ids, description ids, defaults, bounds, sync, sanitizing and colours; RangedAttribute's checks. */
final class AttributeProbe {
    private AttributeProbe() {}

    static void run() {
        java.util.Map<String, Holder<Attribute>> named = new java.util.LinkedHashMap<>();
        named.put("ATTACK_DAMAGE", Attributes.ATTACK_DAMAGE);
        named.put("ATTACK_SPEED", Attributes.ATTACK_SPEED);
        named.put("FALL_DAMAGE_MULTIPLIER", Attributes.FALL_DAMAGE_MULTIPLIER);
        named.put("MAX_HEALTH", Attributes.MAX_HEALTH);
        named.put("MOVEMENT_EFFICIENCY", Attributes.MOVEMENT_EFFICIENCY);
        named.put("MOVEMENT_SPEED", Attributes.MOVEMENT_SPEED);
        named.put("SAFE_FALL_DISTANCE", Attributes.SAFE_FALL_DISTANCE);
        named.put("SNEAKING_SPEED", Attributes.SNEAKING_SPEED);
        named.put("STEP_HEIGHT", Attributes.STEP_HEIGHT);
        named.put("SUBMERGED_MINING_SPEED", Attributes.SUBMERGED_MINING_SPEED);
        named.put("WATER_MOVEMENT_EFFICIENCY", Attributes.WATER_MOVEMENT_EFFICIENCY);
        named.forEach((name, h) -> p("attribute " + name, () -> {
            Attribute a = h.value();
            String range = a instanceof RangedAttribute r ? r.getMinValue() + " " + r.getMaxValue() : "-";
            return h.unwrapKey().orElseThrow().location() + " " + a.getClass().getSimpleName() + " " + a.getDescriptionId() + " " + a.getDefaultValue() + " " + a.isClientSyncable()
                    + " " + range + " " + a.sanitizeValue(-1e9) + " " + a.sanitizeValue(1e9) + " " + a.sanitizeValue(Double.NaN) + " " + a.sanitizeValue(0.5) + " " + a.getStyle(true)
                    + " " + a.getStyle(false) + " " + (h.value() == a);
        }));
        double[][] bad = {{5, 10, 1}, {0, 1, 2}, {3, 1, 2}, {1, 1, 1}};
        for (double[] b : bad) {
            p("attribute ranged " + java.util.Arrays.toString(b), () -> {
                try {
                    RangedAttribute r = new RangedAttribute("x", b[0], b[1], b[2]);
                    return r.getDefaultValue() + " " + r.getMinValue() + " " + r.getMaxValue() + " " + r.isClientSyncable() + " " + r.setSyncable(true).isClientSyncable() + " " + r.getStyle(true)
                            + " " + r.setSentiment(Attribute.Sentiment.NEGATIVE).getStyle(true);
                } catch (IllegalArgumentException e) {
                    return e.getMessage();
                }
            });
        }
        for (Attribute.Sentiment s : Attribute.Sentiment.values()) p("attribute sentiment " + s, () -> s.ordinal() + " " + s.getStyle(true) + " " + s.getStyle(false));
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
