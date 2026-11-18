package net.minecraft.sounds;

import net.minecraft.resources.ResourceLocation;

/** The stand-in for the game's SoundEvent: a sound's id and its range (observed, FluidTypeProbe). Its codecs aren't here. */
public class SoundEvent {
    private final ResourceLocation location;
    private final float range;
    private final boolean newSystem;

    private SoundEvent(ResourceLocation location, float range, boolean newSystem) {
        this.location = location;
        this.range = range;
        this.newSystem = newSystem;
    }

    public static SoundEvent createVariableRangeEvent(ResourceLocation location) {
        return new SoundEvent(location, 16.0F, false);
    }

    public static SoundEvent createFixedRangeEvent(ResourceLocation location, float range) {
        return new SoundEvent(location, range, true);
    }

    public ResourceLocation getLocation() {
        return location;
    }

    /** Observed: a fixed range, else 16 or 16 times a volume above 1. */
    public float getRange(float volume) {
        return newSystem ? range : volume > 1.0F ? 16.0F * volume : 16.0F;
    }
}
