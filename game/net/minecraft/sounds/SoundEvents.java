package net.minecraft.sounds;

import net.minecraft.resources.ResourceLocation;

/**
 * The stand-in for the game's SoundEvents: the sounds of the blocks' sound types here (SoundType), their ids and range
 * observed in a server (`standins --horizon-src`, probe/ChunkProbe.java). Not here: the others, the registry they are in.
 */
public class SoundEvents {
    public static final SoundEvent DEEPSLATE_BREAK = sound("block.deepslate.break");
    public static final SoundEvent DEEPSLATE_FALL = sound("block.deepslate.fall");
    public static final SoundEvent DEEPSLATE_HIT = sound("block.deepslate.hit");
    public static final SoundEvent DEEPSLATE_PLACE = sound("block.deepslate.place");
    public static final SoundEvent DEEPSLATE_STEP = sound("block.deepslate.step");
    public static final SoundEvent GRASS_BREAK = sound("block.grass.break");
    public static final SoundEvent GRASS_FALL = sound("block.grass.fall");
    public static final SoundEvent GRASS_HIT = sound("block.grass.hit");
    public static final SoundEvent GRASS_PLACE = sound("block.grass.place");
    public static final SoundEvent GRASS_STEP = sound("block.grass.step");
    public static final SoundEvent SAND_BREAK = sound("block.sand.break");
    public static final SoundEvent SAND_FALL = sound("block.sand.fall");
    public static final SoundEvent SAND_HIT = sound("block.sand.hit");
    public static final SoundEvent SAND_PLACE = sound("block.sand.place");
    public static final SoundEvent SAND_STEP = sound("block.sand.step");

    public SoundEvents() {}

    private static SoundEvent sound(String id) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace(id));
    }
}
