package net.neoforged.neoforge.common;

/** The stand-in for NeoForge's PercentageAttribute: a ranged attribute shown as a percentage (its tooltip isn't modelled). */
public class PercentageAttribute extends net.minecraft.world.entity.ai.attributes.RangedAttribute {
    public PercentageAttribute(String descriptionId, double defaultValue, double min, double max) {
        super(descriptionId, defaultValue, min, max);
    }
}
