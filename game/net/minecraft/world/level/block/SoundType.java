package net.minecraft.world.level.block;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * The stand-in for the game's SoundType: a block's volume, pitch and sounds (breaking, stepping, placing, hitting,
 * falling); the game's that a mod names (Mekanism's: sand, grass, deepslate), observed in a server (`standins
 * --horizon-src`, probe/ChunkProbe.java). Not here: the game's others.
 */
public class SoundType {
    public static final SoundType GRASS = new SoundType(1.0F, 1.0F, SoundEvents.GRASS_BREAK, SoundEvents.GRASS_STEP, SoundEvents.GRASS_PLACE, SoundEvents.GRASS_HIT, SoundEvents.GRASS_FALL);
    public static final SoundType SAND = new SoundType(1.0F, 1.0F, SoundEvents.SAND_BREAK, SoundEvents.SAND_STEP, SoundEvents.SAND_PLACE, SoundEvents.SAND_HIT, SoundEvents.SAND_FALL);
    public static final SoundType DEEPSLATE = new SoundType(1.0F, 1.0F, SoundEvents.DEEPSLATE_BREAK, SoundEvents.DEEPSLATE_STEP, SoundEvents.DEEPSLATE_PLACE, SoundEvents.DEEPSLATE_HIT, SoundEvents.DEEPSLATE_FALL);

    public final float volume;
    public final float pitch;
    private final SoundEvent breakSound;
    private final SoundEvent stepSound;
    private final SoundEvent placeSound;
    private final SoundEvent hitSound;
    private final SoundEvent fallSound;

    public SoundType(float volume, float pitch, SoundEvent breakSound, SoundEvent stepSound, SoundEvent placeSound, SoundEvent hitSound, SoundEvent fallSound) {
        this.volume = volume;
        this.pitch = pitch;
        this.breakSound = breakSound;
        this.stepSound = stepSound;
        this.placeSound = placeSound;
        this.hitSound = hitSound;
        this.fallSound = fallSound;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

    public SoundEvent getBreakSound() {
        return breakSound;
    }

    public SoundEvent getStepSound() {
        return stepSound;
    }

    public SoundEvent getPlaceSound() {
        return placeSound;
    }

    public SoundEvent getHitSound() {
        return hitSound;
    }

    public SoundEvent getFallSound() {
        return fallSound;
    }
}
