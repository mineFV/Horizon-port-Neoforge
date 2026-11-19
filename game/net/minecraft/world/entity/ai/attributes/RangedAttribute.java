package net.minecraft.world.entity.ai.attributes;

/** The stand-in for the game's RangedAttribute: an attribute between two bounds (observed, `standins --horizon-src`). */
public class RangedAttribute extends Attribute {
    private final double minValue;
    private final double maxValue;

    public RangedAttribute(String descriptionId, double defaultValue, double min, double max) {
        super(descriptionId, defaultValue);
        this.minValue = min;
        this.maxValue = max;
        if (min > max) throw new IllegalArgumentException("Minimum value cannot be bigger than maximum value!");
        if (defaultValue < min) throw new IllegalArgumentException("Default value cannot be lower than minimum value!");
        if (defaultValue > max) throw new IllegalArgumentException("Default value cannot be bigger than maximum value!");
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    /** Observed: NaN to the minimum, else within the bounds. */
    @Override
    public double sanitizeValue(double value) {
        return Double.isNaN(value) ? minValue : net.minecraft.util.Mth.clamp(value, minValue, maxValue);
    }
}
